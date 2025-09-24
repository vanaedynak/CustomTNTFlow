package dev.byflow.customtntflow.api.event;

import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Fired when a custom TNT block is about to transform into a primed entity.
 */
public class CustomTNTPrimeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Block block;
    private final RegionTNTType type;
    private final ItemStack itemStack;
    private final Player player;
    private boolean cancelled;

    public CustomTNTPrimeEvent(Block block, RegionTNTType type, ItemStack itemStack, Player player) {
        this.block = block;
        this.type = type;
        this.itemStack = itemStack == null ? null : itemStack.clone();
        this.player = player;
    }

    public Block getBlock() {
        return block;
    }

    public RegionTNTType getType() {
        return type;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    public Optional<ItemStack> getItemStack() {
        return Optional.ofNullable(itemStack == null ? null : itemStack.clone());
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
