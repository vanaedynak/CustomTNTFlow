package dev.byflow.customtntflow.service.explosion;

import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Iterator;

public class FilterStage implements ExplosionStage {

    @Override
    public void apply(ExplosionContext context) {
        RegionTNTType.BlockBehavior behavior = context.behavior();
        Iterator<Block> iterator = context.blocks().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!shouldAffectBlock(block, behavior)) {
                iterator.remove();
            }
        }
    }

    private boolean shouldAffectBlock(Block block, RegionTNTType.BlockBehavior behavior) {
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }
        if (!behavior.allowFluids() && (type == Material.WATER || type == Material.LAVA)) {
            return false;
        }
        if (behavior.blacklist().contains(type)) {
            return false;
        }
        boolean obsidian = type == Material.OBSIDIAN;
        boolean crying = type == Material.CRYING_OBSIDIAN;
        if (obsidian && !behavior.allowObsidian()) {
            return false;
        }
        if (crying && !behavior.allowCryingObsidian()) {
            return false;
        }
        if (behavior.whitelistOnly()) {
            if (!behavior.whitelist().contains(type)) {
                if (!(obsidian && behavior.allowObsidian()) && !(crying && behavior.allowCryingObsidian())) {
                    return false;
                }
            }
        }
        return true;
    }
}
