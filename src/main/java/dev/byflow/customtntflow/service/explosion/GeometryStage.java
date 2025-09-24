package dev.byflow.customtntflow.service.explosion;

import dev.byflow.customtntflow.model.ExplosionShape;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class GeometryStage implements ExplosionStage {

    @Override
    public void apply(ExplosionContext context) {
        double radius = Math.max(0.5, context.behavior().radius());
        int ceil = (int) Math.ceil(radius);
        ExplosionShape shape = context.behavior().shape();
        Location center = context.tnt().getLocation();
        for (int x = -ceil; x <= ceil; x++) {
            for (int y = -ceil; y <= ceil; y++) {
                for (int z = -ceil; z <= ceil; z++) {
                    if (!isWithinShape(shape, radius, x, y, z)) {
                        continue;
                    }
                    Block block = center.clone().add(x, y, z).getBlock();
                    context.addBlock(block);
                    int maxBlocks = context.behavior().maxBlocks();
                    if (maxBlocks >= 0 && context.blocks().size() >= maxBlocks) {
                        return;
                    }
                }
            }
        }
    }

    private boolean isWithinShape(ExplosionShape shape, double radius, int x, int y, int z) {
        return switch (shape) {
            case CUBE -> Math.abs(x) <= radius && Math.abs(y) <= radius && Math.abs(z) <= radius;
            case SPHERE -> new Vector(x, y, z).lengthSquared() <= radius * radius;
        };
    }
}
