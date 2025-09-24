package dev.byflow.customtntflow.service.explosion;

import dev.byflow.customtntflow.service.region.RegionIntegrationService;
import org.bukkit.block.Block;

import java.util.Iterator;

public class RegionCheckStage implements ExplosionStage {

    private final RegionIntegrationService integrationService;

    public RegionCheckStage(RegionIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @Override
    public void apply(ExplosionContext context) {
        if (integrationService == null || !integrationService.isFeatureEnabled()) {
            return;
        }
        Iterator<Block> iterator = context.blocks().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!integrationService.canAffect(context, block)) {
                iterator.remove();
            }
        }
    }
}
