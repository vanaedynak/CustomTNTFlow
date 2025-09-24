package dev.byflow.customtntflow.service;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.config.ConfigLoadResult;
import dev.byflow.customtntflow.config.TypeConfigurationLoader;
import dev.byflow.customtntflow.model.RegionTNTType;
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

public class RegionTNTRegistry {

    private final CustomTNTFlowPlugin plugin;
    private final NamespacedKey typeKey;
    private final Logger logger;
    private final TypeConfigurationLoader configurationLoader;
    private final Map<String, RegionTNTType> types = new LinkedHashMap<>();
    private int lastMixinCount = 0;
    private List<String> lastWarnings = List.of();

    public RegionTNTRegistry(CustomTNTFlowPlugin plugin, NamespacedKey typeKey) {
        this.plugin = plugin;
        this.typeKey = typeKey;
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
}
