package dev.byflow.customtntflow.api.event;

import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Fired with a mutable block set after the explosion pipeline finished collecting blocks and filters.
 */
public class CustomTNTAffectEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TNTPrimed tnt;
    private final RegionTNTType type;
    private final Set<Block> blocks;

    public CustomTNTAffectEvent(TNTPrimed tnt, RegionTNTType type, Set<Block> blocks) {
        this.tnt = tnt;
        this.type = type;
        this.blocks = blocks == null ? new LinkedHashSet<>() : blocks;
    }

    public TNTPrimed getTnt() {
        return tnt;
    }

    public RegionTNTType getType() {
        return type;
    }

    public Set<Block> getBlocks() {
        return blocks;
    }

    public Set<Block> getBlocksView() {
        return Collections.unmodifiableSet(blocks);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
