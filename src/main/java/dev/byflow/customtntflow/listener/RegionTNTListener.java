package dev.byflow.customtntflow.listener;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.api.event.RegionTNTDetonateEvent;
import dev.byflow.customtntflow.model.RegionTNTType;
import dev.byflow.customtntflow.service.RegionTNTRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RegionTNTListener implements Listener {

    private final CustomTNTFlowPlugin plugin;
    private final RegionTNTRegistry registry;

    public RegionTNTListener(CustomTNTFlowPlugin plugin, RegionTNTRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomTNTPlaced(BlockPlaceEvent event) {
        ItemStack stack = event.getItemInHand();
        registry.matchItem(stack).ifPresent(type -> {
            RegionTNTType.BlockBehavior behavior = type.getBlockBehavior();
            if (!behavior.igniteWhenPlaced()) {
                return;
            }
            var block = event.getBlockPlaced();
            block.setType(Material.AIR, false);
            var world = block.getWorld();
            var location = block.getLocation().add(0.5, 0.0, 0.5);
            TNTPrimed primed = (TNTPrimed) world.spawnEntity(location, EntityType.TNT);
            Player player = event.getPlayer();
            primed.setSource(player);
            registry.applyToPrimed(primed, type);
            if (plugin.getConfig().getBoolean("messages.on-place.enabled", true)) {
                String message = plugin.getConfig().getString("messages.on-place.text", "&eЗаряд активирован!");
                if (message != null && !message.isEmpty()) {
                    message = message.replace("{type}", type.getId());
                    message = message.replace("{radius}", String.format("%.1f", behavior.radius()));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCustomTNTExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }
        var typeOptional = registry.matchEntity(tnt);
        if (typeOptional.isEmpty()) {
            return;
        }
        RegionTNTType type = typeOptional.get();
        RegionTNTType.PrimedSettings primed = type.getPrimedSettings();
        RegionTNTType.BlockBehavior behavior = type.getBlockBehavior();

        if (!primed.explodeInWater() && (tnt.isInWater() || event.getLocation().getBlock().isLiquid())) {
            event.blockList().clear();
            callDetonateEvent(tnt, type, List.of());
            return;
        }

        boolean shouldCollect = behavior.breakBlocks() || behavior.apiOnly();
        List<Block> candidateBlocks = shouldCollect ? collectBlocks(tnt, behavior) : new ArrayList<>();
        RegionTNTDetonateEvent customEvent = callDetonateEvent(tnt, type, candidateBlocks);
        event.blockList().clear();
        if (customEvent.isCancelled() || behavior.apiOnly()) {
            return;
        }
        if (!behavior.breakBlocks()) {
            return;
        }
        List<Block> blocksToBreak = new ArrayList<>(customEvent.getAffectedBlocks());
        int maxBlocks = behavior.maxBlocks();
        if (maxBlocks >= 0 && blocksToBreak.size() > maxBlocks) {
            blocksToBreak = new ArrayList<>(blocksToBreak.subList(0, maxBlocks));
        }
        boolean dropBlocks = behavior.dropBlocks();
        for (Block block : blocksToBreak) {
            if (dropBlocks) {
                block.breakNaturally();
            } else {
                block.setType(Material.AIR, false);
            }
        }
    }

    private RegionTNTDetonateEvent callDetonateEvent(TNTPrimed tnt, RegionTNTType type, List<Block> blocks) {
        RegionTNTDetonateEvent customEvent = new RegionTNTDetonateEvent(tnt, type, blocks);
        Bukkit.getPluginManager().callEvent(customEvent);
        return customEvent;
    }

    private List<Block> collectBlocks(TNTPrimed tnt, RegionTNTType.BlockBehavior behavior) {
        double radius = Math.max(0.5, behavior.radius());
        int ceil = (int) Math.ceil(radius);
        var world = tnt.getWorld();
        var center = tnt.getLocation();
        Set<Block> blocks = new LinkedHashSet<>();
        for (int x = -ceil; x <= ceil; x++) {
            for (int y = -ceil; y <= ceil; y++) {
                for (int z = -ceil; z <= ceil; z++) {
                    Vector offset = new Vector(x, y, z);
                    if (offset.lengthSquared() > radius * radius) {
                        continue;
                    }
                    Block block = world.getBlockAt(center.clone().add(offset));
                    if (shouldAffectBlock(block, behavior)) {
                        blocks.add(block);
                        if (behavior.maxBlocks() >= 0 && blocks.size() >= behavior.maxBlocks()) {
                            return new ArrayList<>(blocks);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(blocks);
    }

    private boolean shouldAffectBlock(Block block, RegionTNTType.BlockBehavior behavior) {
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }
        if (!behavior.allowFluids() && (type == Material.WATER || type == Material.LAVA)) {
            return false;
        }
        if (behavior.blacklist().contains(type)) {
            return false;
        }
        boolean obsidian = type == Material.OBSIDIAN;
        boolean crying = type == Material.CRYING_OBSIDIAN;
        if (obsidian && !behavior.allowObsidian()) {
            return false;
        }
        if (crying && !behavior.allowCryingObsidian()) {
            return false;
        }
        if (behavior.whitelistOnly()) {
            if (!behavior.whitelist().contains(type)) {
                if (!(obsidian && behavior.allowObsidian()) && !(crying && behavior.allowCryingObsidian())) {
                    return false;
                }
            }
        }
        return true;
    }
}
