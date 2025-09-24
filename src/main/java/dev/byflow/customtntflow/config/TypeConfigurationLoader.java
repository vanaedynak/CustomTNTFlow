package dev.byflow.customtntflow.config;

import dev.byflow.customtntflow.model.ExplosionShape;
import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TypeConfigurationLoader {

    private final Logger logger;

    public TypeConfigurationLoader(Logger logger) {
        this.logger = logger;
    }

    public ConfigLoadResult load(FileConfiguration config) {
        int version = config.getInt("version", 1);
        if (version >= 2) {
            return loadVersionTwo(config);
        }
        return loadLegacy(config);
    }

    private ConfigLoadResult loadLegacy(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("types");
        if (section == null) {
            logger.warn("Конфиг не содержит секцию types.");
            return new ConfigLoadResult(Map.of(), 0, List.of());
        }
        Map<String, RegionTNTType> result = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection typeSection = section.getConfigurationSection(id);
            if (typeSection == null) {
                continue;
            }
            RegionTNTType type = parseType(id, typeSection, warnings);
            if (type != null) {
                result.put(id, type);
            }
        }
        return new ConfigLoadResult(result, 0, List.copyOf(warnings));
    }

    private ConfigLoadResult loadVersionTwo(FileConfiguration config) {
        ConfigurationSection typesSection = config.getConfigurationSection("types");
        if (typesSection == null) {
            logger.warn("Конфиг версии 2 не содержит секцию types.");
            return new ConfigLoadResult(Map.of(), 0, List.of());
        }

        MergeStrategy mergeStrategy = MergeStrategy.from(config.getConfigurationSection("merge"));
        Map<String, Object> defaults = sectionToMap(config.getConfigurationSection("defaults"));
        Map<String, Map<String, Object>> mixins = readMixins(config.getConfigurationSection("mixins"));

        Map<String, TypeDefinition> definitions = new LinkedHashMap<>();
        for (String id : typesSection.getKeys(false)) {
            ConfigurationSection typeSection = typesSection.getConfigurationSection(id);
            if (typeSection == null) {
                continue;
            }
            Map<String, Object> content = sectionToMap(typeSection);
            String extendsId = Optional.ofNullable(typeSection.getString("extends")).map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
            Object rawUse = typeSection.get("use");
            List<MixinReference> mixinRefs = parseMixins(rawUse);
            content.remove("extends");
            content.remove("use");
            definitions.put(id, new TypeDefinition(id, content, extendsId, mixinRefs));
        }

        Map<String, Map<String, Object>> resolvedCache = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        Map<String, RegionTNTType> resolvedTypes = new LinkedHashMap<>();
        for (String id : definitions.keySet()) {
            Map<String, Object> resolvedMap;
            try {
                resolvedMap = resolveType(id, definitions, defaults, mixins, mergeStrategy, resolvedCache, new ArrayDeque<>(), warnings);
            } catch (IllegalStateException ex) {
                logger.error(ex.getMessage());
                continue;
            }
            MemoryConfiguration section = new MemoryConfiguration();
            populateSection(section, resolvedMap);
            RegionTNTType type = parseType(id, section, warnings);
            if (type != null) {
                resolvedTypes.put(id, type);
            }
        }
        return new ConfigLoadResult(resolvedTypes, mixins.size(), List.copyOf(warnings));
    }

    private Map<String, Object> resolveType(String id,
                                            Map<String, TypeDefinition> definitions,
                                            Map<String, Object> defaults,
                                            Map<String, Map<String, Object>> mixins,
                                            MergeStrategy mergeStrategy,
                                            Map<String, Map<String, Object>> cache,
                                            Deque<String> stack,
                                            List<String> warnings) {
        if (cache.containsKey(id)) {
            return deepCopyMap(cache.get(id));
        }
        if (!definitions.containsKey(id)) {
            throw new IllegalStateException("Тип " + id + " не найден для разрешения extends/use.");
        }
        if (stack.contains(id)) {
            stack.addLast(id);
            throw new IllegalStateException("Обнаружена циклическая зависимость типов: " + String.join(" -> ", stack));
        }
        stack.addLast(id);
        TypeDefinition definition = definitions.get(id);

        Map<String, Object> current = deepCopyMap(defaults);

        if (definition.parentId() != null) {
            Map<String, Object> parent = resolveType(definition.parentId(), definitions, defaults, mixins, mergeStrategy, cache, stack, warnings);
            current = mergeStrategy.merge(current, parent);
        }

        List<MixinReference> mixinRefs = definition.mixins();
        for (int i = mixinRefs.size() - 1; i >= 0; i--) {
            MixinReference mixinReference = mixinRefs.get(i);
            Map<String, Object> mixin = mixins.get(mixinReference.name());
            if (mixin == null) {
                warnings.add("Тип " + id + ": миксин " + mixinReference.name() + " не найден");
                continue;
            }
            Map<String, Object> mixinCopy = deepCopyMap(mixin);
            if (!mixinReference.overrides().isEmpty()) {
                mixinCopy = mergeStrategy.merge(mixinCopy, mixinReference.overrides());
            }
            current = mergeStrategy.merge(current, mixinCopy);
        }

        current = mergeStrategy.merge(current, definition.content());
        cache.put(id, deepCopyMap(current));
        stack.removeLast();
        return deepCopyMap(current);
    }

    private Map<String, Map<String, Object>> readMixins(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection mixinSection = section.getConfigurationSection(id);
            if (mixinSection == null) {
                continue;
            }
            map.put(id, sectionToMap(mixinSection));
        }
        return map;
    }

    private List<MixinReference> parseMixins(Object rawUse) {
        if (rawUse == null) {
            return List.of();
        }
        List<MixinReference> result = new ArrayList<>();
        if (rawUse instanceof Collection<?> collection) {
            for (Object entry : collection) {
                parseSingleMixin(entry).ifPresent(result::add);
            }
        } else {
            parseSingleMixin(rawUse).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private Optional<MixinReference> parseSingleMixin(Object entry) {
        if (entry instanceof String str) {
            String trimmed = str.trim();
            if (!trimmed.isEmpty()) {
                return Optional.of(new MixinReference(trimmed, Map.of()));
            }
            return Optional.empty();
        }
        if (entry instanceof Map<?, ?> map) {
            if (map.size() != 1) {
                return Optional.empty();
            }
            Map.Entry<?, ?> raw = map.entrySet().iterator().next();
            Object key = raw.getKey();
            Object value = raw.getValue();
            if (key == null) {
                return Optional.empty();
            }
            String name = Objects.toString(key);
            Map<String, Object> overrides = value instanceof Map<?, ?> vMap ? normalizeMap(vMap) : Map.of();
            return Optional.of(new MixinReference(name, overrides));
        }
        return Optional.empty();
    }

    private Map<String, Object> sectionToMap(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nested) {
                map.put(key, sectionToMap(nested));
            } else if (value instanceof List<?> list) {
                map.put(key, new ArrayList<>(list));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    private Map<String, Object> normalizeMap(Map<?, ?> raw) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                map.put(Objects.toString(entry.getKey()), normalizeMap(nested));
            } else if (value instanceof List<?> list) {
                map.put(Objects.toString(entry.getKey()), new ArrayList<>(list));
            } else {
                map.put(Objects.toString(entry.getKey()), value);
            }
        }
        return map;
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return deepCopyMap(normalizeMap(map));
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object element : list) {
                copy.add(deepCopyValue(element));
            }
            return copy;
        }
        return value;
    }

    private void populateSection(ConfigurationSection section, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                populateSection(section.createSection(entry.getKey()), normalizeMap(nested));
            } else {
                section.set(entry.getKey(), value);
            }
        }
    }

    private RegionTNTType parseType(String id, ConfigurationSection section, List<String> warnings) {
        ConfigurationSection itemSection = section.getConfigurationSection("item");
        ConfigurationSection primedSection = section.getConfigurationSection("primed");
        ConfigurationSection behaviorSection = section.getConfigurationSection("behavior");

        RegionTNTType.ItemSettings itemSettings = parseItemSettings(id, itemSection);
        RegionTNTType.PrimedSettings primedSettings = parsePrimedSettings(primedSection);
        RegionTNTType.BlockBehavior blockBehavior = parseBlockBehavior(behaviorSection, warnings, id);

        Map<String, String> itemData = parseStringMap(itemSection != null ? itemSection.getConfigurationSection("persistent-data") : null);
        Map<String, String> entityData = parseStringMap(primedSection != null ? primedSection.getConfigurationSection("persistent-data") : null);
        List<String> scoreboardTags = primedSection != null ? primedSection.getStringList("scoreboard-tags") : List.of();

        return new RegionTNTType(id, itemSettings, primedSettings, blockBehavior, itemData, entityData, scoreboardTags);
    }

    private RegionTNTType.ItemSettings parseItemSettings(String id, ConfigurationSection section) {
        if (section == null) {
            return new RegionTNTType.ItemSettings(Material.TNT, null, List.of(), false, null, false, List.of());
        }
        String materialName = section.getString("material", "TNT");
        Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
        if (material == null) {
            logger.warn("Тип {}: неизвестный материал {}. Использую TNT.", id, materialName);
            material = Material.TNT;
        }
        String displayName = section.getString("display-name");
        List<String> lore = section.getStringList("lore");
        boolean glow = section.getBoolean("glow", false);
        Integer customModelData = section.contains("custom-model-data") ? section.getInt("custom-model-data") : null;
        boolean unbreakable = section.getBoolean("unbreakable", false);
        List<ItemFlag> flags = new ArrayList<>();
        for (String raw : section.getStringList("hidden-flags")) {
            try {
                flags.add(ItemFlag.valueOf(raw.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                logger.warn("Тип {}: неизвестный ItemFlag {}", id, raw);
            }
        }
        return new RegionTNTType.ItemSettings(material, displayName, lore, glow, customModelData, unbreakable, flags);
    }

    private RegionTNTType.PrimedSettings parsePrimedSettings(ConfigurationSection section) {
        if (section == null) {
            return new RegionTNTType.PrimedSettings(null, false, 80, 4.0f, false, true, false);
        }
        String customName = section.getString("custom-name");
        boolean showName = section.getBoolean("show-name", false);
        int fuseTicks = section.getInt("fuse-ticks", 80);
        float power = (float) section.getDouble("power", 4.0);
        boolean incendiary = section.getBoolean("incendiary", false);
        boolean gravity = section.getBoolean("gravity", true);
        boolean explodeInWater = section.getBoolean("explode-in-water", false);
        return new RegionTNTType.PrimedSettings(customName, showName, fuseTicks, power, incendiary, gravity, explodeInWater);
    }

    private RegionTNTType.BlockBehavior parseBlockBehavior(ConfigurationSection section, List<String> warnings, String typeId) {
        if (section == null) {
            RegionTNTType.BlockBehavior behavior = new RegionTNTType.BlockBehavior(true, false, 4.0, ExplosionShape.SPHERE, false, false, Set.of(), Set.of(), false, false, false, -1, false);
            return validateBehavior(behavior, warnings, typeId);
        }
        boolean igniteWhenPlaced = section.getBoolean("ignite-when-placed", true);
        boolean breakBlocks = section.getBoolean("break-blocks", false);
        double radius = section.getDouble("radius", 4.0);
        ExplosionShape shape = ExplosionShape.fromConfig(section.getString("shape"));
        boolean dropBlocks = section.getBoolean("drop-blocks", false);
        boolean whitelistOnly = section.getBoolean("whitelist-only", false);
        Set<Material> whitelist = parseMaterials(section.getStringList("whitelist"));
        Set<Material> blacklist = parseMaterials(section.getStringList("blacklist"));
        boolean allowObsidian = section.getBoolean("allow-obsidian", false);
        boolean allowCryingObsidian = section.getBoolean("allow-crying-obsidian", allowObsidian);
        boolean allowFluids = section.getBoolean("allow-fluids", false);
        int maxBlocks = section.getInt("max-blocks", -1);
        boolean apiOnly = section.getBoolean("api-only", false);

        RegionTNTType.BlockBehavior behavior = new RegionTNTType.BlockBehavior(igniteWhenPlaced, breakBlocks, radius, shape, dropBlocks, whitelistOnly, whitelist, blacklist, allowObsidian, allowCryingObsidian, allowFluids, maxBlocks, apiOnly);
        return validateBehavior(behavior, warnings, typeId);
    }

    private RegionTNTType.BlockBehavior validateBehavior(RegionTNTType.BlockBehavior behavior, List<String> warnings, String typeId) {
        if (behavior.radius() <= 0) {
            warnings.add("Тип " + typeId + ": radius должен быть больше 0. Используется значение 1.0");
            behavior = new RegionTNTType.BlockBehavior(behavior.igniteWhenPlaced(), behavior.breakBlocks(), 1.0, behavior.shape(), behavior.dropBlocks(), behavior.whitelistOnly(), behavior.whitelist(), behavior.blacklist(), behavior.allowObsidian(), behavior.allowCryingObsidian(), behavior.allowFluids(), behavior.maxBlocks(), behavior.apiOnly());
        }
        if (behavior.maxBlocks() != -1 && behavior.maxBlocks() < 0) {
            warnings.add("Тип " + typeId + ": max-blocks должен быть -1 или неотрицательным. Используется -1.");
            behavior = new RegionTNTType.BlockBehavior(behavior.igniteWhenPlaced(), behavior.breakBlocks(), behavior.radius(), behavior.shape(), behavior.dropBlocks(), behavior.whitelistOnly(), behavior.whitelist(), behavior.blacklist(), behavior.allowObsidian(), behavior.allowCryingObsidian(), behavior.allowFluids(), -1, behavior.apiOnly());
        }
        if (behavior.whitelistOnly() && behavior.whitelist().isEmpty()) {
            warnings.add("Тип " + typeId + ": whitelist-only=true, но список whitelist пуст.");
        }
        if (behavior.apiOnly() && behavior.breakBlocks()) {
            warnings.add("Тип " + typeId + ": api-only=true — break-blocks будет проигнорирован.");
            behavior = new RegionTNTType.BlockBehavior(
                    behavior.igniteWhenPlaced(),
                    false,
                    behavior.radius(),
                    behavior.shape(),
                    behavior.dropBlocks(),
                    behavior.whitelistOnly(),
                    behavior.whitelist(),
                    behavior.blacklist(),
                    behavior.allowObsidian(),
                    behavior.allowCryingObsidian(),
                    behavior.allowFluids(),
                    behavior.maxBlocks(),
                    true
            );
        }
        return behavior;
    }

    private Set<Material> parseMaterials(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Material> result = new LinkedHashSet<>();
        for (String raw : list) {
            Material material = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
            if (material != null) {
                result.add(material);
            } else {
                logger.warn("Не удалось распознать материал: {}", raw);
            }
        }
        return result;
    }

    private Map<String, String> parseStringMap(ConfigurationSection section) {
        Map<String, String> map = new LinkedHashMap<>();
        if (section == null) {
            return map;
        }
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null) {
                map.put(key, Objects.toString(value));
            }
        }
        return map;
    }

    private record TypeDefinition(String id,
                                  Map<String, Object> content,
                                  String parentId,
                                  List<MixinReference> mixins) {
    }

    private record MixinReference(String name, Map<String, Object> overrides) {
    }

    private static class MergeStrategy {

        private final ListStrategy listStrategy;
        private final MapStrategy mapStrategy;

        private MergeStrategy(ListStrategy listStrategy, MapStrategy mapStrategy) {
            this.listStrategy = listStrategy;
            this.mapStrategy = mapStrategy;
        }

        static MergeStrategy from(ConfigurationSection section) {
            ListStrategy listStrategy = ListStrategy.APPEND_DEDUPE;
            MapStrategy mapStrategy = MapStrategy.DEEP;
            if (section != null) {
                String lists = section.getString("lists", "append-dedupe");
                if (lists != null) {
                    listStrategy = ListStrategy.fromConfig(lists);
                }
                String maps = section.getString("maps", "deep");
                if (maps != null) {
                    mapStrategy = MapStrategy.fromConfig(maps);
                }
            }
            return new MergeStrategy(listStrategy, mapStrategy);
        }

        Map<String, Object> merge(Map<String, Object> base, Map<String, Object> addition) {
            Map<String, Object> result = new LinkedHashMap<>(base);
            mergeInto(result, addition);
            return result;
        }

        private void mergeInto(Map<String, Object> target, Map<String, Object> addition) {
            if (addition == null) {
                return;
            }
            for (Map.Entry<String, Object> entry : addition.entrySet()) {
                Object additionValue = entry.getValue();
                Object existing = target.get(entry.getKey());
                Object merged = mergeValues(existing, additionValue);
                target.put(entry.getKey(), merged);
            }
        }

        private Object mergeValues(Object base, Object addition) {
            if (base == null) {
                return deepCopyStatic(addition);
            }
            if (addition == null) {
                return base;
            }
            if (base instanceof Map<?, ?> baseMap && addition instanceof Map<?, ?> addMap) {
                Map<String, Object> normalizedBase = normalizeStatic(baseMap);
                Map<String, Object> normalizedAddition = normalizeStatic(addMap);
                if (mapStrategy == MapStrategy.REPLACE) {
                    return deepCopyStatic(addition);
                }
                Map<String, Object> copy = new LinkedHashMap<>(normalizedBase);
                for (Map.Entry<String, Object> entry : normalizedAddition.entrySet()) {
                    Object merged = mergeValues(copy.get(entry.getKey()), entry.getValue());
                    copy.put(entry.getKey(), merged);
                }
                return copy;
            }
            if (base instanceof List<?> baseList && addition instanceof List<?> addList) {
                if (listStrategy == ListStrategy.APPEND_DEDUPE) {
                    LinkedHashSet<Object> set = new LinkedHashSet<>();
                    set.addAll(baseList);
                    set.addAll(addList);
                    return new ArrayList<>(set);
                }
                return new ArrayList<>(addList);
            }
            return deepCopyStatic(addition);
        }

        private static Map<String, Object> normalizeStatic(Map<?, ?> raw) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> nested) {
                    map.put(Objects.toString(entry.getKey()), normalizeStatic(nested));
                } else if (value instanceof List<?> list) {
                    map.put(Objects.toString(entry.getKey()), new ArrayList<>(list));
                } else {
                    map.put(Objects.toString(entry.getKey()), value);
                }
            }
            return map;
        }

        private static Object deepCopyStatic(Object value) {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    copy.put(Objects.toString(entry.getKey()), deepCopyStatic(entry.getValue()));
                }
                return copy;
            }
            if (value instanceof List<?> list) {
                List<Object> copy = new ArrayList<>();
                for (Object element : list) {
                    copy.add(deepCopyStatic(element));
                }
                return copy;
            }
            return value;
        }
    }

    private enum ListStrategy {
        APPEND_DEDUPE,
        REPLACE;

        static ListStrategy fromConfig(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "append", "append-dedupe" -> APPEND_DEDUPE;
                case "replace" -> REPLACE;
                default -> APPEND_DEDUPE;
            };
        }
    }

    private enum MapStrategy {
        DEEP,
        REPLACE;

        static MapStrategy fromConfig(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "replace" -> REPLACE;
                default -> DEEP;
            };
        }
    }
}
