package dev.byflow.customtntflow.service.explosion;

import dev.byflow.customtntflow.model.RegionTNTType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ExplosionContext {

    private final TNTPrimed tnt;
    private final RegionTNTType type;
    private final RegionTNTType.BlockBehavior behavior;
    private final Logger logger;
    private final UUID ownerUuid;
    private final UUID sourceUuid;
    private Player messageTarget;
    private boolean denyMessageSent;
    private final Set<Block> blocks = new LinkedHashSet<>();
    private List<Block> finalizedBlocks = List.of();

    public ExplosionContext(TNTPrimed tnt,
                             RegionTNTType type,
                             RegionTNTType.BlockBehavior behavior,
                             Logger logger,
                             UUID ownerUuid) {
        this.tnt = tnt;
        this.type = type;
        this.behavior = behavior;
        this.logger = logger;
        this.ownerUuid = ownerUuid;
        Entity source = tnt.getSource();
        this.sourceUuid = source instanceof Player player ? player.getUniqueId() : null;
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

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public UUID sourceUuid() {
        return sourceUuid;
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

    public void sendDenyMessage(String message) {
        if (denyMessageSent || message == null || message.isBlank()) {
            return;
        }
        Player player = resolveNotifier();
        if (player != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        denyMessageSent = true;
    }

    private Player resolveNotifier() {
        if (messageTarget != null && messageTarget.isOnline()) {
            return messageTarget;
        }
        if (sourceUuid != null) {
            Player source = Bukkit.getPlayer(sourceUuid);
            if (source != null) {
                messageTarget = source;
                return source;
            }
        }
        if (ownerUuid != null) {
            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner != null) {
                messageTarget = owner;
                return owner;
            }
        }
        Entity source = tnt.getSource();
        if (source instanceof Player player) {
            messageTarget = player;
            return player;
        }
        return null;
    }
}
