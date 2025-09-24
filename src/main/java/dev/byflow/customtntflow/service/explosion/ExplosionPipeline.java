package dev.byflow.customtntflow.service.explosion;

import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.slf4j.Logger;

import java.util.List;

public class ExplosionPipeline {

    private final List<ExplosionStage> stages;

    public ExplosionPipeline(List<ExplosionStage> stages) {
        this.stages = List.copyOf(stages);
    }

    public List<Block> process(TNTPrimed tnt, RegionTNTType type, RegionTNTType.BlockBehavior behavior, Logger logger) {
        ExplosionContext context = new ExplosionContext(tnt, type, behavior, logger);
        for (ExplosionStage stage : stages) {
            stage.apply(context);
        }
        return context.finalizedBlocks();
    }
}
