package dev.byflow.customtntflow;

import dev.byflow.customtntflow.api.RegionTNTAPI;
import dev.byflow.customtntflow.listener.RegionTNTListener;
import dev.byflow.customtntflow.service.RegionTNTRegistry;
import dev.byflow.customtntflow.service.command.RegionTNTCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class CustomTNTFlowPlugin extends JavaPlugin {

    private Logger logger;
    private RegionTNTRegistry registry;
    private NamespacedKey typeKey;

    @Override
    public void onEnable() {
        this.logger = getSLF4JLogger();

        saveDefaultConfig();

        this.typeKey = new NamespacedKey(this, "tnt_type");
        this.registry = new RegionTNTRegistry(this, typeKey);
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
            logger.info("Configuration reloaded. Types available: {}", registry.getTypes().size());
        }
    }

    public RegionTNTRegistry getRegistry() {
        return registry;
    }

    public NamespacedKey getTypeKey() {
        return typeKey;
    }

    public Logger getPluginLogger() {
        return logger;
    }
}
