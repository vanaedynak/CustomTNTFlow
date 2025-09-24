package dev.byflow.customtntflow.listener;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.api.event.RegionTNTDetonateEvent;
import dev.byflow.customtntflow.model.RegionTNTType;
import dev.byflow.customtntflow.service.RegionTNTRegistry;
import dev.byflow.customtntflow.service.explosion.ExplosionPipeline;
import dev.byflow.customtntflow.service.explosion.FilterStage;
import dev.byflow.customtntflow.service.explosion.FinalizeStage;
import dev.byflow.customtntflow.service.explosion.GeometryStage;
import dev.byflow.customtntflow.service.explosion.RegionCheckStage;
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
import java.util.ArrayList;
import java.util.List;

public class RegionTNTListener implements Listener {

    private final CustomTNTFlowPlugin plugin;
    private final RegionTNTRegistry registry;
    private final ExplosionPipeline explosionPipeline;

    public RegionTNTListener(CustomTNTFlowPlugin plugin, RegionTNTRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.explosionPipeline = new ExplosionPipeline(List.of(
                new GeometryStage(),
                new FilterStage(),
                new RegionCheckStage(null),
                new FinalizeStage()
        ));
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
        List<Block> candidateBlocks = shouldCollect ? explosionPipeline.process(tnt, type, plugin.getSLF4JLogger()) : new ArrayList<>();
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
}
