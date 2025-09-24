package dev.byflow.customtntflow.api.event;

import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fired after the final block set is determined and before the plugin applies world changes.
 */
public class CustomTNTExplodeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TNTPrimed tnt;
    private final RegionTNTType type;
    private final List<Block> blocks;
    private final UUID owner;

    public CustomTNTExplodeEvent(TNTPrimed tnt, RegionTNTType type, List<Block> blocks, UUID owner) {
        this.tnt = tnt;
        this.type = type;
        this.blocks = List.copyOf(blocks);
        this.owner = owner;
    }

    public TNTPrimed getTnt() {
        return tnt;
    }

    public RegionTNTType getType() {
        return type;
    }

    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public Optional<UUID> getOwner() {
        return Optional.ofNullable(owner);
    }

    public PersistentDataContainer getPersistentData() {
        return tnt.getPersistentDataContainer();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
