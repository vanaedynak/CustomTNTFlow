package dev.byflow.customtntflow.service.region;

import dev.byflow.customtntflow.service.explosion.ExplosionContext;
import org.bukkit.block.Block;

public interface RegionProtectionHook {

    String name();

    boolean canAffect(ExplosionContext context, Block block, RegionCheckMode mode);
}
