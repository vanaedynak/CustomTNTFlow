package dev.byflow.customtntflow;

import dev.byflow.customtntflow.api.RegionTNTAPI;
import dev.byflow.customtntflow.listener.RegionTNTListener;
import dev.byflow.customtntflow.model.DebugFlag;
import dev.byflow.customtntflow.model.DebugSettings;
import dev.byflow.customtntflow.service.RegionTNTRegistry;
import dev.byflow.customtntflow.service.command.RegionTNTCommand;
import dev.byflow.customtntflow.service.nbt.NbtService;
import dev.byflow.customtntflow.service.region.RegionIntegrationService;
import dev.byflow.customtntflow.util.PersistentDataKeys;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class CustomTNTFlowPlugin extends JavaPlugin {

    private Logger logger;
    private RegionTNTRegistry registry;
    private PersistentDataKeys persistentDataKeys;
    private RegionIntegrationService regionIntegrationService;
    private NbtService nbtService;
    private DebugSettings debugSettings = DebugSettings.defaults();

    @Override
    public void onEnable() {
        this.logger = getSLF4JLogger();

        saveDefaultConfig();

        applyDebugSettings(DebugSettings.fromConfig(getConfig()));

        this.persistentDataKeys = new PersistentDataKeys(this);
        this.regionIntegrationService = new RegionIntegrationService(this);
        this.nbtService = new NbtService(logger);
        this.registry = new RegionTNTRegistry(this, persistentDataKeys, nbtService);
        this.registry.applyDebugSettings(debugSettings);
        this.registry.reloadFromConfig();
        this.regionIntegrationService.reload();

        RegionTNTAPI.initialize(this, registry);

        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new RegionTNTListener(this, registry, regionIntegrationService), this);

        var command = getCommand("tntflow");
        if (command != null) {
            RegionTNTCommand executor = new RegionTNTCommand(this, registry);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            logger.warn("Command tntflow is not defined in plugin.yml");
        }

        logger.info("CustomTNTFlow enabled with {} TNT types", registry.getTypes().size());
    }

    @Override
    public void onDisable() {
        RegionTNTAPI.shutdown();
        if (logger == null) {
            logger = getSLF4JLogger();
        }
        logger.info("CustomTNTFlow disabled");
    }

    public void reloadEverything() {
        reloadConfig();
        applyDebugSettings(DebugSettings.fromConfig(getConfig()));
        registry.reloadFromConfig();
        regionIntegrationService.reload();
        if (logger != null) {
            int warningCount = registry.getLastWarnings().size();
            int errorCount = registry.getLastErrors().size();
            logger.info("Configuration reloaded. Types: {}, mixins: {}, warnings: {}, errors: {}",
                    registry.getTypes().size(),
                    registry.getLastMixinCount(),
                    warningCount,
                    errorCount);
        }
    }

    public RegionTNTRegistry getRegistry() {
        return registry;
    }

    public PersistentDataKeys getPersistentDataKeys() {
        return persistentDataKeys;
    }

    public Logger getPluginLogger() {
        return logger;
    }

    public DebugSettings getDebugSettings() {
        return debugSettings;
    }

    public DebugSettings toggleDebugFlag(DebugFlag flag) {
        if (flag == null) {
            return debugSettings;
        }
        DebugSettings newSettings = debugSettings.toggle(flag);
        applyDebugSettings(newSettings);
        if (logger != null) {
            logger.info("Debug flag {} -> {}", flag.configKey(), newSettings.isEnabled(flag) ? "ON" : "OFF");
        }
        if (registry != null && newSettings.isEnabled(flag) && flag != DebugFlag.TRACE_EXPLOSION) {
            registry.emitDebugSnapshot(flag);
        }
        return debugSettings;
    }

    private void applyDebugSettings(DebugSettings settings) {
        this.debugSettings = settings != null ? settings : DebugSettings.defaults();
        if (registry != null) {
            registry.applyDebugSettings(this.debugSettings);
        }
    }
}
