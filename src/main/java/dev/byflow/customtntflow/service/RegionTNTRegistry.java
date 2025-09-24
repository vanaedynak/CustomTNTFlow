package dev.byflow.customtntflow.service;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.config.ConfigLoadResult;
import dev.byflow.customtntflow.config.TypeConfigurationLoader;
import dev.byflow.customtntflow.model.DebugFlag;
import dev.byflow.customtntflow.model.DebugSettings;
import dev.byflow.customtntflow.model.RegionTNTType;
import dev.byflow.customtntflow.model.TypeMetadata;
import dev.byflow.customtntflow.util.PersistentDataKeys;
import dev.byflow.customtntflow.util.TraitSnapshotParser;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RegionTNTRegistry {

    private final CustomTNTFlowPlugin plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey legacyTypeKey;
    private final NamespacedKey traitsKey;
    private final NamespacedKey uuidKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey explodeIdKey;
    private final Logger logger;
    private final TraitSnapshotParser traitParser = new TraitSnapshotParser();
    private final TypeConfigurationLoader configurationLoader;
    private final Map<String, RegionTNTType> types = new LinkedHashMap<>();
    private final Map<String, TypeMetadata> metadata = new LinkedHashMap<>();
    private int lastMixinCount = 0;
    private List<String> lastWarnings = List.of();
    private List<String> lastErrors = List.of();
    private DebugSettings debugSettings = DebugSettings.defaults();

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
        metadata.clear();
        ConfigLoadResult result = configurationLoader.load(plugin.getConfig());
        types.putAll(result.types());
        metadata.putAll(result.metadata());
        lastMixinCount = result.mixinCount();
        lastWarnings = result.warnings();
        lastErrors = result.errors();
        if (!lastErrors.isEmpty()) {
            for (String error : lastErrors) {
                logger.error(error);
            }
        }
        if (!lastWarnings.isEmpty()) {
            for (String warning : lastWarnings) {
                logger.warn(warning);
            }
        }
        if (debugSettings.logMergeSources()) {
            logMergeSourcesSnapshot();
        }
        if (debugSettings.logCompiledTypes()) {
            logCompiledTypesSnapshot();
        }
        logger.info("Загружено типов TNT: {}, миксинов: {}", types.size(), lastMixinCount);
    }

    public Collection<RegionTNTType> getTypes() {
        return List.copyOf(types.values());
    }

    public RegionTNTType getType(String id) {
        return types.get(id);
    }

    public Optional<TypeMetadata> getMetadata(String id) {
        return Optional.ofNullable(metadata.get(id));
    }

    public Map<String, TypeMetadata> getMetadataSnapshot() {
        return Map.copyOf(metadata);
    }

    public List<String> resolveInheritanceChain(String id) {
        List<String> chain = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        TypeMetadata current = metadata.get(id);
        while (current != null && current.hasParent()) {
            String parent = current.parentId();
            if (!visited.add(parent)) {
                chain.add(parent + " (cycle)");
                break;
            }
            chain.add(parent);
            current = metadata.get(parent);
        }
        return chain;
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
                meta.setDisplayName(color(applyItemPlaceholders(settings.displayName(), type)));
            }
            if (!settings.lore().isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : settings.lore()) {
                    lore.add(color(applyItemPlaceholders(line, type)));
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
            container.set(traitsKey, PersistentDataType.STRING, buildTraitsSnapshot(type.getBlockBehavior()));
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

    public boolean isCustom(Entity entity) {
        if (entity == null) {
            return false;
        }
        return resolveTypeId(entity.getPersistentDataContainer()) != null;
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
        container.set(traitsKey, PersistentDataType.STRING, buildTraitsSnapshot(type.getBlockBehavior()));
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

    public void updateTraits(TNTPrimed tnt, RegionTNTType.BlockBehavior behavior) {
        if (tnt == null || behavior == null) {
            return;
        }
        tnt.getPersistentDataContainer().set(traitsKey, PersistentDataType.STRING, buildTraitsSnapshot(behavior));
    }

    public Optional<UUID> resolveOwner(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return readUuid(entity.getPersistentDataContainer(), ownerKey);
    }

    public Optional<UUID> resolveExplodeId(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return readUuid(entity.getPersistentDataContainer(), explodeIdKey);
    }

    public Optional<UUID> resolveUniqueItemId(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return readUuid(entity.getPersistentDataContainer(), uuidKey);
    }

    public Map<String, Object> getEffectiveTraits(Entity entity) {
        if (entity == null) {
            return Map.of();
        }
        PersistentDataContainer container = entity.getPersistentDataContainer();
        String raw = container.get(traitsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return traitParser.parse(raw);
        } catch (IllegalArgumentException ex) {
            logger.warn("Не удалось распарсить traits JSON: {}", raw, ex);
            return Map.of();
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

    public List<String> getLastErrors() {
        return lastErrors;
    }

    public void applyDebugSettings(DebugSettings settings) {
        this.debugSettings = settings != null ? settings : DebugSettings.defaults();
    }

    public void emitDebugSnapshot(DebugFlag flag) {
        if (flag == DebugFlag.LOG_COMPILED_TYPES) {
            logCompiledTypesSnapshot();
        } else if (flag == DebugFlag.LOG_MERGE_SOURCES) {
            logMergeSourcesSnapshot();
        }
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

    private String applyItemPlaceholders(String text, RegionTNTType type) {
        if (text == null) {
            return null;
        }
        RegionTNTType.BlockBehavior behavior = type.getBlockBehavior();
        RegionTNTType.PrimedSettings primed = type.getPrimedSettings();
        String replaced = text.replace("{radius}", formatRadius(behavior.radius()));
        replaced = replaced.replace("{drops}", behavior.dropBlocks() ? "Да" : "Нет");
        boolean obsidian = behavior.allowObsidian() || behavior.allowCryingObsidian();
        replaced = replaced.replace("{obsidian}", obsidian ? "Да" : "Нет");
        boolean water = primed.explodeInWater() && behavior.allowFluids();
        replaced = replaced.replace("{water}", water ? "Да" : "Нет");
        return replaced;
    }

    private String formatRadius(double radius) {
        return String.format(Locale.ROOT, "%.1f", radius);
    }

    private void logMergeSourcesSnapshot() {
        for (RegionTNTType type : types.values()) {
            TypeMetadata meta = metadata.get(type.getId());
            if (meta == null) {
                continue;
            }
            List<String> chain = resolveInheritanceChain(type.getId());
            String inheritance = chain.isEmpty() ? "(нет)" : String.join(" -> ", chain);
            String mixinInfo = meta.mixins().isEmpty()
                    ? "(нет)"
                    : meta.mixins().stream().map(this::formatMixin).collect(Collectors.joining(", "));
            logger.info("[debug] Тип {}: extends={}, mixins={}, defaults={}",
                    type.getId(),
                    inheritance,
                    mixinInfo,
                    meta.defaultsApplied() ? "включены" : "нет");
        }
    }

    private void logCompiledTypesSnapshot() {
        for (RegionTNTType type : types.values()) {
            RegionTNTType.BlockBehavior behavior = type.getBlockBehavior();
            logger.info(
                    "[debug] Параметры {} => radius={}, shape={}, break-blocks={}, drop-blocks={}, allow-fluids={}, allow-obsidian={}, allow-crying-obsidian={}, whitelist-only={}, whitelist={}, blacklist={}, max-blocks={}, api-only={}",
                    type.getId(),
                    formatRadius(behavior.radius()),
                    behavior.shape(),
                    behavior.breakBlocks(),
                    behavior.dropBlocks(),
                    behavior.allowFluids(),
                    behavior.allowObsidian(),
                    behavior.allowCryingObsidian(),
                    behavior.whitelistOnly(),
                    formatMaterials(behavior.whitelist()),
                    formatMaterials(behavior.blacklist()),
                    behavior.maxBlocks(),
                    behavior.apiOnly());
        }
    }

    private String formatMixin(TypeMetadata.MixinMetadata mixin) {
        if (mixin == null) {
            return "";
        }
        if (!mixin.hasOverrides()) {
            return mixin.name();
        }
        return mixin.name() + formatOverrides(mixin.overrides());
    }

    private String formatOverrides(Map<String, Object> overrides) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=');
            builder.append(formatOverrideValue(entry.getValue()));
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    private String formatOverrideValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return formatOverrides(normalized);
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
        }
        return String.valueOf(value);
    }

    private String formatMaterials(Set<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return "[]";
        }
        return materials.stream().map(Material::name).collect(Collectors.joining(", ", "[", "]"));
    }

    private String buildTraitsSnapshot(RegionTNTType.BlockBehavior behavior) {
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
