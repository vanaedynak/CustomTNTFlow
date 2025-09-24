package dev.byflow.customtntflow.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionTNTType {

    private final String id;
    private final ItemSettings itemSettings;
    private final PrimedSettings primedSettings;
    private final BlockBehavior blockBehavior;
    private final Map<String, String> itemPersistentData;
    private final Map<String, String> entityPersistentData;
    private final List<String> scoreboardTags;

    public RegionTNTType(String id,
                         ItemSettings itemSettings,
                         PrimedSettings primedSettings,
                         BlockBehavior blockBehavior,
                         Map<String, String> itemPersistentData,
                         Map<String, String> entityPersistentData,
                         List<String> scoreboardTags) {
        this.id = id;
        this.itemSettings = itemSettings;
        this.primedSettings = primedSettings;
        this.blockBehavior = blockBehavior;
        this.itemPersistentData = itemPersistentData;
        this.entityPersistentData = entityPersistentData;
        this.scoreboardTags = scoreboardTags;
    }

    public String getId() {
        return id;
    }

    public ItemSettings getItemSettings() {
        return itemSettings;
    }

    public PrimedSettings getPrimedSettings() {
        return primedSettings;
    }

    public BlockBehavior getBlockBehavior() {
        return blockBehavior;
    }

    public Map<String, String> getItemPersistentData() {
        return itemPersistentData;
    }

    public Map<String, String> getEntityPersistentData() {
        return entityPersistentData;
    }

    public List<String> getScoreboardTags() {
        return scoreboardTags;
    }

    public record ItemSettings(Material material,
                               String displayName,
                               List<String> lore,
                               boolean glow,
                               Integer customModelData,
                               boolean unbreakable,
                               List<ItemFlag> hiddenFlags) {
        public ItemSettings {
            lore = lore == null ? List.of() : List.copyOf(lore);
            hiddenFlags = hiddenFlags == null ? List.of() : List.copyOf(hiddenFlags);
        }
    }

    public record PrimedSettings(String customName,
                                 boolean showCustomName,
                                 int fuseTicks,
                                 float power,
                                 boolean incendiary,
                                 boolean hasGravity,
                                 boolean explodeInWater) {
    }

    public record BlockBehavior(boolean igniteWhenPlaced,
                                 boolean breakBlocks,
                                 double radius,
                                 ExplosionShape shape,
                                 boolean dropBlocks,
                                 Set<Material> dropBlacklist,
                                 boolean whitelistOnly,
                                 Set<Material> whitelist,
                                 Set<Material> blacklist,
                                 boolean allowObsidian,
                                 boolean allowCryingObsidian,
                                 boolean allowFluids,
                                 int maxBlocks,
                                 boolean apiOnly) {
        public BlockBehavior {
            dropBlacklist = dropBlacklist == null ? Collections.emptySet() : Set.copyOf(dropBlacklist);
            whitelist = whitelist == null ? Collections.emptySet() : Set.copyOf(whitelist);
            blacklist = blacklist == null ? Collections.emptySet() : Set.copyOf(blacklist);
        }
    }
}
