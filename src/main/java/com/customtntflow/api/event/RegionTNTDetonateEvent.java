package com.customtntflow.api.event;

import com.customtntflow.type.RegionTNTType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Событие вызывается непосредственно перед тем, как плагин начнёт обрабатывать ломание блоков
 * вокруг пользовательского TNT. Вы можете отменить событие, чтобы полностью запретить воздействие на блоки,
 * либо модифицировать {@link #getAffectedBlocks()} (например, удалить или добавить блоки).
 */
public class RegionTNTDetonateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TNTPrimed tnt;
    private final RegionTNTType type;
    private final List<Block> affectedBlocks;
    private boolean cancelled;

    public RegionTNTDetonateEvent(TNTPrimed tnt, RegionTNTType type, List<Block> affectedBlocks) {
        this.tnt = tnt;
        this.type = type;
        this.affectedBlocks = new ArrayList<>(affectedBlocks);
    }

    public TNTPrimed getTnt() {
        return tnt;
    }

    public RegionTNTType getType() {
        return type;
    }

    public Location getLocation() {
        return tnt.getLocation();
    }

    public List<Block> getAffectedBlocks() {
        return affectedBlocks;
    }

    public List<Block> getAffectedBlocksView() {
        return Collections.unmodifiableList(affectedBlocks);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
