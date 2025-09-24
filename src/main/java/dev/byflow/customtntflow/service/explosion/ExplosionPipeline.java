package dev.byflow.customtntflow.service.explosion;

import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

public class ExplosionPipeline {

    private final List<ExplosionStage> stages;

    public ExplosionPipeline(List<ExplosionStage> stages) {
        this.stages = List.copyOf(stages);
    }

    public List<Block> process(TNTPrimed tnt,
                               RegionTNTType type,
                               RegionTNTType.BlockBehavior behavior,
                               Logger logger,
                               UUID ownerUuid,
                               boolean trace) {
        ExplosionContext context = new ExplosionContext(tnt, type, behavior, logger, ownerUuid);
        for (ExplosionStage stage : stages) {
            int before = context.blocks().size();
            stage.apply(context);
            if (trace && logger != null) {
                int after = context.blocks().size();
                logger.info("[trace:{}] Stage {} — blocks {} -> {}", type.getId(), stage.getClass().getSimpleName(), before, after);
            }
        }
        List<Block> finalized = context.finalizedBlocks();
        if (trace && logger != null) {
            logger.info("[trace:{}] Pipeline итог: {} блоков", type.getId(), finalized.size());
        }
        return finalized;
    }
}
