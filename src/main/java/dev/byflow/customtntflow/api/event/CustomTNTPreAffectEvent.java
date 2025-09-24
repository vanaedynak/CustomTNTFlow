package dev.byflow.customtntflow.api.event;

import dev.byflow.customtntflow.api.MutableBlockBehavior;
import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired right before the explosion pipeline collects affected blocks allowing integrations to tweak
 * the runtime behavior or cancel the blast entirely.
 */
public class CustomTNTPreAffectEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TNTPrimed tnt;
    private final RegionTNTType type;
    private final MutableBlockBehavior behavior;
    private boolean cancelled;

    public CustomTNTPreAffectEvent(TNTPrimed tnt, RegionTNTType type) {
        this.tnt = tnt;
        this.type = type;
        this.behavior = MutableBlockBehavior.from(type.getBlockBehavior());
    }

    public TNTPrimed getTnt() {
        return tnt;
    }

    public RegionTNTType getType() {
        return type;
    }

    public MutableBlockBehavior getBehavior() {
        return behavior;
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
