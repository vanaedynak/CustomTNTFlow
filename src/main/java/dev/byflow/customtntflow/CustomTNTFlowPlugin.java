package dev.byflow.customtntflow;

import dev.byflow.customtntflow.api.RegionTNTAPI;
import dev.byflow.customtntflow.listener.RegionTNTListener;
import dev.byflow.customtntflow.service.RegionTNTRegistry;
import dev.byflow.customtntflow.service.command.RegionTNTCommand;
import dev.byflow.customtntflow.util.PersistentDataKeys;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class CustomTNTFlowPlugin extends JavaPlugin {

    private Logger logger;
    private RegionTNTRegistry registry;
    private PersistentDataKeys persistentDataKeys;

    @Override
    public void onEnable() {
        this.logger = getSLF4JLogger();

        saveDefaultConfig();

        this.persistentDataKeys = new PersistentDataKeys(this);
        this.registry = new RegionTNTRegistry(this, persistentDataKeys);
        this.registry.reloadFromConfig();

        RegionTNTAPI.initialize(this, registry);

        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new RegionTNTListener(this, registry), this);

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
        registry.reloadFromConfig();
        if (logger != null) {
            int warningCount = registry.getLastWarnings().size();
            logger.info("Configuration reloaded. Types: {}, mixins: {}, warnings: {}",
                    registry.getTypes().size(),
                    registry.getLastMixinCount(),
                    warningCount);
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
}
