package dev.byflow.customtntflow.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Centralized registry of PersistentDataContainer keys exposed to other plugins.
 */
public final class PersistentDataKeys {

    private final NamespacedKey type;
    private final NamespacedKey traits;
    private final NamespacedKey uuid;
    private final NamespacedKey owner;
    private final NamespacedKey explodeId;
    private final NamespacedKey legacyType;

    public PersistentDataKeys(Plugin plugin) {
        this.type = new NamespacedKey("customtntflow", "type");
        this.traits = new NamespacedKey("customtntflow", "traits");
        this.uuid = new NamespacedKey("customtntflow", "uuid");
        this.owner = new NamespacedKey("customtntflow", "owner");
        this.explodeId = new NamespacedKey("customtntflow", "explode_id");
        this.legacyType = new NamespacedKey(plugin, "tnt_type");
    }

    public NamespacedKey type() {
        return type;
    }

    public NamespacedKey traits() {
        return traits;
    }

    public NamespacedKey uuid() {
        return uuid;
    }

    public NamespacedKey owner() {
        return owner;
    }

    public NamespacedKey explodeId() {
        return explodeId;
    }

    public NamespacedKey legacyType() {
        return legacyType;
    }
}
