package dev.byflow.customtntflow.service.explosion;

import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExplosionContext {

    private final TNTPrimed tnt;
    private final RegionTNTType type;
    private final RegionTNTType.BlockBehavior behavior;
    private final Logger logger;
    private final Set<Block> blocks = new LinkedHashSet<>();
    private List<Block> finalizedBlocks = List.of();

    public ExplosionContext(TNTPrimed tnt, RegionTNTType type, RegionTNTType.BlockBehavior behavior, Logger logger) {
        this.tnt = tnt;
        this.type = type;
        this.behavior = behavior;
        this.logger = logger;
    }

    public TNTPrimed tnt() {
        return tnt;
    }

    public RegionTNTType type() {
        return type;
    }

    public RegionTNTType.BlockBehavior behavior() {
        return behavior;
    }

    public Logger logger() {
        return logger;
    }

    public void addBlock(Block block) {
        blocks.add(block);
    }

    public void addBlocks(Collection<Block> collection) {
        blocks.addAll(collection);
    }

    public Set<Block> blocks() {
        return blocks;
    }

    public void replaceBlocks(Collection<Block> collection) {
        blocks.clear();
        blocks.addAll(collection);
    }

    public void finalizeBlocks(Collection<Block> collection) {
        this.finalizedBlocks = List.copyOf(collection);
    }

    public List<Block> finalizedBlocks() {
        if (finalizedBlocks.isEmpty()) {
            finalizedBlocks = new ArrayList<>(blocks);
        }
        return finalizedBlocks;
    }
}
