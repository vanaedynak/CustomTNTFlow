package dev.byflow.customtntflow.service;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.config.ConfigLoadResult;
import dev.byflow.customtntflow.config.TypeConfigurationLoader;
import dev.byflow.customtntflow.model.RegionTNTType;
import dev.byflow.customtntflow.util.PersistentDataKeys;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RegionTNTRegistry {

    private final CustomTNTFlowPlugin plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey legacyTypeKey;
    private final NamespacedKey traitsKey;
    private final NamespacedKey uuidKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey explodeIdKey;
    private final Logger logger;
    private final TypeConfigurationLoader configurationLoader;
    private final Map<String, RegionTNTType> types = new LinkedHashMap<>();
    private int lastMixinCount = 0;
    private List<String> lastWarnings = List.of();

    public RegionTNTRegistry(CustomTNTFlowPlugin plugin, PersistentDataKeys dataKeys) {
        this.plugin = plugin;
        this.typeKey = dataKeys.type();
        this.legacyTypeKey = dataKeys.legacyType();
        this.traitsKey = dataKeys.traits();
        this.uuidKey = dataKeys.uuid();
        this.ownerKey = dataKeys.owner();
        this.explodeIdKey = dataKeys.explodeId();
        this.logger = plugin.getSLF4JLogger();
        this.configurationLoader = new TypeConfigurationLoader(this.logger);
    }

    public void reloadFromConfig() {
        types.clear();
        ConfigLoadResult result = configurationLoader.load(plugin.getConfig());
        types.putAll(result.types());
        lastMixinCount = result.mixinCount();
        lastWarnings = result.warnings();
        if (!lastWarnings.isEmpty()) {
            for (String warning : lastWarnings) {
                logger.warn(warning);
            }
        }
        logger.info("Загружено типов TNT: {}, миксинов: {}", types.size(), lastMixinCount);
    }

    public Collection<RegionTNTType> getTypes() {
        return List.copyOf(types.values());
    }

    public RegionTNTType getType(String id) {
        return types.get(id);
    }

    public ItemStack createItem(String id, int amount) {
        RegionTNTType type = types.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Неизвестный тип TNT: " + id);
        }
        return createItem(type, amount);
    }

    public ItemStack createItem(RegionTNTType type, int amount) {
        RegionTNTType.ItemSettings settings = type.getItemSettings();
        Material material = settings.material() != null ? settings.material() : Material.TNT;
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (settings.displayName() != null) {
                meta.setDisplayName(color(settings.displayName()));
            }
            if (!settings.lore().isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : settings.lore()) {
                    lore.add(color(line));
                }
                meta.setLore(lore);
            }
            if (settings.customModelData() != null) {
                meta.setCustomModelData(settings.customModelData());
            }
            meta.setUnbreakable(settings.unbreakable());
            if (settings.glow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                if (!settings.hiddenFlags().contains(ItemFlag.HIDE_ENCHANTS)) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
            if (!settings.hiddenFlags().isEmpty()) {
                meta.addItemFlags(settings.hiddenFlags().toArray(new ItemFlag[0]));
            }
            PersistentDataContainer container = meta.getPersistentDataContainer();
            applyPersistentData(container, type.getItemPersistentData());
            container.set(typeKey, PersistentDataType.STRING, type.getId());
            container.set(legacyTypeKey, PersistentDataType.STRING, type.getId());
            container.set(traitsKey, PersistentDataType.STRING, buildTraitsSnapshot(type));
            container.set(uuidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public Optional<RegionTNTType> matchItem(ItemStack stack) {
        if (stack == null) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = resolveTypeId(container);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(types.get(id));
    }

    public Optional<RegionTNTType> matchEntity(Entity entity) {
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return Optional.empty();
        }
        PersistentDataContainer container = tntPrimed.getPersistentDataContainer();
        String id = resolveTypeId(container);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(types.get(id));
    }

    public void applyToPrimed(TNTPrimed tnt, RegionTNTType type, ItemStack sourceStack, UUID ownerUuid) {
        RegionTNTType.PrimedSettings primed = type.getPrimedSettings();
        tnt.setFuseTicks(Math.max(1, primed.fuseTicks()));
        tnt.setYield(Math.max(0.1f, primed.power()));
        tnt.setIsIncendiary(primed.incendiary());
        tnt.setGravity(primed.hasGravity());
        if (primed.customName() != null && !primed.customName().isEmpty()) {
            tnt.setCustomName(color(primed.customName()));
            tnt.setCustomNameVisible(primed.showCustomName());
        }
        PersistentDataContainer container = tnt.getPersistentDataContainer();
        applyPersistentData(container, type.getEntityPersistentData());
        container.set(typeKey, PersistentDataType.STRING, type.getId());
        container.set(legacyTypeKey, PersistentDataType.STRING, type.getId());
        container.set(traitsKey, PersistentDataType.STRING, buildTraitsSnapshot(type));
        UUID uniqueId = resolveSourceUuid(sourceStack).orElseGet(UUID::randomUUID);
        container.set(uuidKey, PersistentDataType.STRING, uniqueId.toString());
        container.set(explodeIdKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        if (ownerUuid != null) {
            container.set(ownerKey, PersistentDataType.STRING, ownerUuid.toString());
        } else {
            container.remove(ownerKey);
        }
        for (String tag : type.getScoreboardTags()) {
            tnt.addScoreboardTag(tag);
        }
    }

    public NamespacedKey getTypeKey() {
        return typeKey;
    }

    public int getLastMixinCount() {
        return lastMixinCount;
    }

    public List<String> getLastWarnings() {
        return lastWarnings;
    }

    private void applyPersistentData(PersistentDataContainer container, Map<String, String> data) {
        data.forEach((key, value) -> {
            NamespacedKey namespacedKey = toKey(key);
            if (namespacedKey == null) {
                return;
            }
            container.set(namespacedKey, PersistentDataType.STRING, value);
        });
    }

    private String resolveTypeId(PersistentDataContainer container) {
        String id = container.get(typeKey, PersistentDataType.STRING);
        if (id != null) {
            return id;
        }
        return container.get(legacyTypeKey, PersistentDataType.STRING);
    }

    private Optional<UUID> resolveSourceUuid(ItemStack stack) {
        if (stack == null) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        return readUuid(meta.getPersistentDataContainer(), uuidKey);
    }

    private Optional<UUID> readUuid(PersistentDataContainer container, NamespacedKey key) {
        String raw = container.get(key, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            logger.warn("Некорректный UUID в PDC {}: {}", key, raw);
            return Optional.empty();
        }
    }

    private NamespacedKey toKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey != null) {
            return namespacedKey;
        }
        try {
            return new NamespacedKey(plugin, key.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warn("Некорректный ключ NBT: {}", key, ex);
            return null;
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String buildTraitsSnapshot(RegionTNTType type) {
        RegionTNTType.BlockBehavior behavior = type.getBlockBehavior();
        StringBuilder json = new StringBuilder();
        json.append('{');
        boolean first = true;
        first = appendNumber(json, "radius", behavior.radius(), first);
        first = appendString(json, "shape", behavior.shape().name(), first);
        first = appendBoolean(json, "igniteWhenPlaced", behavior.igniteWhenPlaced(), first);
        first = appendBoolean(json, "breakBlocks", behavior.breakBlocks(), first);
        first = appendBoolean(json, "dropBlocks", behavior.dropBlocks(), first);
        first = appendBoolean(json, "allowFluids", behavior.allowFluids(), first);
        first = appendBoolean(json, "allowObsidian", behavior.allowObsidian(), first);
        first = appendBoolean(json, "allowCryingObsidian", behavior.allowCryingObsidian(), first);
        first = appendBoolean(json, "whitelistOnly", behavior.whitelistOnly(), first);
        first = appendBoolean(json, "apiOnly", behavior.apiOnly(), first);
        first = appendNumber(json, "maxBlocks", behavior.maxBlocks(), first);
        first = appendMaterials(json, "whitelist", behavior.whitelist(), first);
        first = appendMaterials(json, "blacklist", behavior.blacklist(), first);
        json.append('}');
        return json.toString();
    }

    private boolean appendBoolean(StringBuilder json, String key, boolean value, boolean first) {
        if (!first) {
            json.append(',');
        }
        json.append('"').append(escapeJson(key)).append('"').append(':').append(value);
        return false;
    }

    private boolean appendNumber(StringBuilder json, String key, double value, boolean first) {
        if (!first) {
            json.append(',');
        }
        json.append('"').append(escapeJson(key)).append('"').append(':').append(Double.toString(value));
        return false;
    }

    private boolean appendNumber(StringBuilder json, String key, int value, boolean first) {
        if (!first) {
            json.append(',');
        }
        json.append('"').append(escapeJson(key)).append('"').append(':').append(value);
        return false;
    }

    private boolean appendString(StringBuilder json, String key, String value, boolean first) {
        if (!first) {
            json.append(',');
        }
        json.append('"').append(escapeJson(key)).append('"').append(':').append('"').append(escapeJson(value)).append('"');
        return false;
    }

    private boolean appendMaterials(StringBuilder json, String key, Set<Material> materials, boolean first) {
        if (materials == null || materials.isEmpty()) {
            return first;
        }
        if (!first) {
            json.append(',');
        }
        json.append('"').append(escapeJson(key)).append('"').append(':').append('[');
        boolean firstMaterial = true;
        for (Material material : materials) {
            if (!firstMaterial) {
                json.append(',');
            }
            json.append('"').append(escapeJson(material.name())).append('"');
            firstMaterial = false;
        }
        json.append(']');
        return false;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
