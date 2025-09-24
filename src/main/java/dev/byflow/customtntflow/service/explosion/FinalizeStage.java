package dev.byflow.customtntflow.service.explosion;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class FinalizeStage implements ExplosionStage {

    @Override
    public void apply(ExplosionContext context) {
        List<Block> ordered = new ArrayList<>(context.blocks());
        int maxBlocks = context.behavior().maxBlocks();
        if (maxBlocks >= 0 && ordered.size() > maxBlocks) {
            ordered = new ArrayList<>(ordered.subList(0, maxBlocks));
            context.logger().warn("Тип {} превысил лимит max-blocks ({}). Остальные блоки будут проигнорированы.", context.type().getId(), maxBlocks);
        }
        context.finalizeBlocks(ordered);
    }
}
