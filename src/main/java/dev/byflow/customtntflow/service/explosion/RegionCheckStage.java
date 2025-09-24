package dev.byflow.customtntflow.service.explosion;

import org.bukkit.block.Block;

import java.util.Iterator;
import java.util.Objects;

public class RegionCheckStage implements ExplosionStage {

    private final RegionPermissionEvaluator evaluator;

    public RegionCheckStage(RegionPermissionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNullElseGet(evaluator, () -> (context, block) -> true);
    }

    @Override
    public void apply(ExplosionContext context) {
        Iterator<Block> iterator = context.blocks().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!evaluator.canAffect(context, block)) {
                iterator.remove();
            }
        }
    }

    @FunctionalInterface
    public interface RegionPermissionEvaluator {
        boolean canAffect(ExplosionContext context, Block block);
    }
}
