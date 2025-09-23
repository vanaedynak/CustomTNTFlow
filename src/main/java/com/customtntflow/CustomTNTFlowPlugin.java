package com.customtntflow;

import com.customtntflow.api.RegionTNTAPI;
import com.customtntflow.command.RegionTNTCommand;
import com.customtntflow.listener.RegionTNTListener;
import com.customtntflow.type.RegionTNTRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomTNTFlowPlugin extends JavaPlugin {

    private RegionTNTRegistry registry;
    private NamespacedKey typeKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.typeKey = new NamespacedKey(this, "tnt_type");
        this.registry = new RegionTNTRegistry(this, typeKey);
        this.registry.reloadFromConfig();

        RegionTNTAPI.initialize(this, registry);

        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new RegionTNTListener(this, registry), this);

        var command = getCommand("regiontnt");
        if (command != null) {
            RegionTNTCommand executor = new RegionTNTCommand(this, registry);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Command regiontnt is not defined in plugin.yml");
        }
    }

    @Override
    public void onDisable() {
        RegionTNTAPI.shutdown();
    }

    public void reloadEverything() {
        reloadConfig();
        registry.reloadFromConfig();
    }

    public RegionTNTRegistry getRegistry() {
        return registry;
    }

    public NamespacedKey getTypeKey() {
        return typeKey;
    }
}
