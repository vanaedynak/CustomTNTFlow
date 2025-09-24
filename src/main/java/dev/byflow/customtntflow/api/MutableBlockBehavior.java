package dev.byflow.customtntflow.api;

import dev.byflow.customtntflow.model.ExplosionShape;
import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.Material;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Mutable view over {@link RegionTNTType.BlockBehavior} that can be tweaked by API consumers
 * inside {@link dev.byflow.customtntflow.api.event.CustomTNTPreAffectEvent}.
 */
public final class MutableBlockBehavior {

    private boolean igniteWhenPlaced;
    private boolean breakBlocks;
    private double radius;
    private ExplosionShape shape;
    private boolean dropBlocks;
    private boolean whitelistOnly;
    private final Set<Material> whitelist;
    private final Set<Material> blacklist;
    private boolean allowObsidian;
    private boolean allowCryingObsidian;
    private boolean allowFluids;
    private int maxBlocks;
    private boolean apiOnly;

    private MutableBlockBehavior(RegionTNTType.BlockBehavior behavior) {
        this.igniteWhenPlaced = behavior.igniteWhenPlaced();
        this.breakBlocks = behavior.breakBlocks();
        this.radius = behavior.radius();
        this.shape = behavior.shape();
        this.dropBlocks = behavior.dropBlocks();
        this.whitelistOnly = behavior.whitelistOnly();
        this.whitelist = new LinkedHashSet<>(behavior.whitelist());
        this.blacklist = new LinkedHashSet<>(behavior.blacklist());
        this.allowObsidian = behavior.allowObsidian();
        this.allowCryingObsidian = behavior.allowCryingObsidian();
        this.allowFluids = behavior.allowFluids();
        this.maxBlocks = behavior.maxBlocks();
        this.apiOnly = behavior.apiOnly();
    }

    public static MutableBlockBehavior from(RegionTNTType.BlockBehavior behavior) {
        Objects.requireNonNull(behavior, "behavior");
        return new MutableBlockBehavior(behavior);
    }

    public RegionTNTType.BlockBehavior toImmutable() {
        return new RegionTNTType.BlockBehavior(
                igniteWhenPlaced,
                breakBlocks,
                radius,
                shape,
                dropBlocks,
                whitelistOnly,
                whitelist,
                blacklist,
                allowObsidian,
                allowCryingObsidian,
                allowFluids,
                maxBlocks,
                apiOnly
        );
    }

    public boolean isIgniteWhenPlaced() {
        return igniteWhenPlaced;
    }

    public void setIgniteWhenPlaced(boolean igniteWhenPlaced) {
        this.igniteWhenPlaced = igniteWhenPlaced;
    }

    public boolean isBreakBlocks() {
        return breakBlocks;
    }

    public void setBreakBlocks(boolean breakBlocks) {
        this.breakBlocks = breakBlocks;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public ExplosionShape getShape() {
        return shape;
    }

    public void setShape(ExplosionShape shape) {
        this.shape = Objects.requireNonNull(shape, "shape");
    }

    public boolean isDropBlocks() {
        return dropBlocks;
    }

    public void setDropBlocks(boolean dropBlocks) {
        this.dropBlocks = dropBlocks;
    }

    public boolean isWhitelistOnly() {
        return whitelistOnly;
    }

    public void setWhitelistOnly(boolean whitelistOnly) {
        this.whitelistOnly = whitelistOnly;
    }

    public Set<Material> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(Collection<Material> whitelist) {
        this.whitelist.clear();
        if (whitelist != null) {
            this.whitelist.addAll(whitelist);
        }
    }

    public Set<Material> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(Collection<Material> blacklist) {
        this.blacklist.clear();
        if (blacklist != null) {
            this.blacklist.addAll(blacklist);
        }
    }

    public boolean isAllowObsidian() {
        return allowObsidian;
    }

    public void setAllowObsidian(boolean allowObsidian) {
        this.allowObsidian = allowObsidian;
    }

    public boolean isAllowCryingObsidian() {
        return allowCryingObsidian;
    }

    public void setAllowCryingObsidian(boolean allowCryingObsidian) {
        this.allowCryingObsidian = allowCryingObsidian;
    }

    public boolean isAllowFluids() {
        return allowFluids;
    }

    public void setAllowFluids(boolean allowFluids) {
        this.allowFluids = allowFluids;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public void setMaxBlocks(int maxBlocks) {
        this.maxBlocks = maxBlocks;
    }

    public boolean isApiOnly() {
        return apiOnly;
    }

    public void setApiOnly(boolean apiOnly) {
        this.apiOnly = apiOnly;
    }
}
