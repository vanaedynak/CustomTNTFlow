package com.customtntflow.type;

import com.customtntflow.CustomTNTFlowPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

public class RegionTNTRegistry {

    private final CustomTNTFlowPlugin plugin;
    private final NamespacedKey typeKey;
    private final Map<String, RegionTNTType> types = new LinkedHashMap<>();

    public RegionTNTRegistry(CustomTNTFlowPlugin plugin, NamespacedKey typeKey) {
        this.plugin = plugin;
        this.typeKey = typeKey;
    }

    public void reloadFromConfig() {
        types.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("types");
        if (section == null) {
            plugin.getLogger().warning("Конфиг не содержит секцию types.");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection typeSection = section.getConfigurationSection(id);
            if (typeSection == null) {
                continue;
            }
            RegionTNTType type = parseType(id, typeSection);
            if (type != null) {
                types.put(id, type);
            }
        }
        plugin.getLogger().info("Загружено типов TNT: " + types.size());
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
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                if (!settings.hiddenFlags().contains(ItemFlag.HIDE_ENCHANTS)) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
            if (!settings.hiddenFlags().isEmpty()) {
                meta.addItemFlags(settings.hiddenFlags().toArray(new ItemFlag[0]));
            }
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(typeKey, PersistentDataType.STRING, type.getId());
            applyPersistentData(container, type.getItemPersistentData());
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
        String id = container.get(typeKey, PersistentDataType.STRING);
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
        String id = container.get(typeKey, PersistentDataType.STRING);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(types.get(id));
    }

    public void applyToPrimed(TNTPrimed tnt, RegionTNTType type) {
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
        container.set(typeKey, PersistentDataType.STRING, type.getId());
        applyPersistentData(container, type.getEntityPersistentData());
        for (String tag : type.getScoreboardTags()) {
            tnt.addScoreboardTag(tag);
        }
    }

    public NamespacedKey getTypeKey() {
        return typeKey;
    }

    private RegionTNTType parseType(String id, ConfigurationSection section) {
        ConfigurationSection itemSection = section.getConfigurationSection("item");
        ConfigurationSection primedSection = section.getConfigurationSection("primed");
        RegionTNTType.ItemSettings itemSettings = parseItemSettings(id, itemSection);
        RegionTNTType.PrimedSettings primedSettings = parsePrimedSettings(primedSection);
        RegionTNTType.BlockBehavior blockBehavior = parseBlockBehavior(section.getConfigurationSection("behavior"));
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
            plugin.getLogger().warning("Тип " + id + ": неизвестный материал " + materialName + ". Использую TNT.");
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
                plugin.getLogger().warning("Тип " + id + ": неизвестный ItemFlag " + raw);
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

    private RegionTNTType.BlockBehavior parseBlockBehavior(ConfigurationSection section) {
        if (section == null) {
            return new RegionTNTType.BlockBehavior(true, false, 4.0, false, false, Set.of(), Set.of(), false, false, false, -1, false);
        }
        boolean igniteWhenPlaced = section.getBoolean("ignite-when-placed", true);
        boolean breakBlocks = section.getBoolean("break-blocks", false);
        double radius = section.getDouble("radius", 4.0);
        boolean dropBlocks = section.getBoolean("drop-blocks", false);
        boolean whitelistOnly = section.getBoolean("whitelist-only", false);
        Set<Material> whitelist = parseMaterials(section.getStringList("whitelist"));
        Set<Material> blacklist = parseMaterials(section.getStringList("blacklist"));
        boolean allowObsidian = section.getBoolean("allow-obsidian", false);
        boolean allowCryingObsidian = section.getBoolean("allow-crying-obsidian", allowObsidian);
        boolean allowFluids = section.getBoolean("allow-fluids", false);
        int maxBlocks = section.getInt("max-blocks", -1);
        boolean apiOnly = section.getBoolean("api-only", false);
        return new RegionTNTType.BlockBehavior(igniteWhenPlaced, breakBlocks, radius, dropBlocks, whitelistOnly, whitelist, blacklist, allowObsidian, allowCryingObsidian, allowFluids, maxBlocks, apiOnly);
    }

    private Set<Material> parseMaterials(List<String> list) {
        Set<Material> result = new java.util.HashSet<>();
        for (String raw : list) {
            Material material = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
            if (material != null) {
                result.add(material);
            } else {
                plugin.getLogger().warning("Не удалось распознать материал: " + raw);
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

    private void applyPersistentData(PersistentDataContainer container, Map<String, String> data) {
        data.forEach((key, value) -> {
            NamespacedKey namespacedKey = toKey(key);
            if (namespacedKey == null) {
                return;
            }
            container.set(namespacedKey, PersistentDataType.STRING, value);
        });
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
            plugin.getLogger().log(Level.WARNING, "Некорректный ключ NBT: " + key, ex);
            return null;
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
