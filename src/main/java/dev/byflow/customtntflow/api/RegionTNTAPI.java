package dev.byflow.customtntflow.api;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.model.RegionTNTType;
import dev.byflow.customtntflow.service.RegionTNTRegistry;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Public API entry point for other plugins.
 */
public final class RegionTNTAPI {

    private static RegionTNTAPI instance;

    private final CustomTNTFlowPlugin plugin;
    private final RegionTNTRegistry registry;

    private RegionTNTAPI(CustomTNTFlowPlugin plugin, RegionTNTRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public static void initialize(CustomTNTFlowPlugin plugin, RegionTNTRegistry registry) {
        instance = new RegionTNTAPI(plugin, registry);
    }

    public static void shutdown() {
        instance = null;
    }

    public static RegionTNTAPI get() {
        if (instance == null) {
            throw new IllegalStateException("RegionTNTAPI is not initialized yet");
        }
        return instance;
    }

    public CustomTNTFlowPlugin getPlugin() {
        return plugin;
    }

    public Optional<RegionTNTType> findType(String id) {
        return Optional.ofNullable(registry.getType(id));
    }

    public Optional<RegionTNTType> findType(ItemStack stack) {
        return registry.matchItem(stack);
    }

    public Optional<RegionTNTType> findType(Entity entity) {
        return registry.matchEntity(entity);
    }

    public RegionTNTRegistry getRegistry() {
        return registry;
    }
}
