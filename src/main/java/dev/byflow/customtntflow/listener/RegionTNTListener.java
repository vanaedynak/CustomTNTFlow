package dev.byflow.customtntflow.listener;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.api.event.CustomTNTAffectEvent;
import dev.byflow.customtntflow.api.event.CustomTNTExplodeEvent;
import dev.byflow.customtntflow.api.event.CustomTNTPreAffectEvent;
import dev.byflow.customtntflow.api.event.CustomTNTPrimeEvent;
import dev.byflow.customtntflow.api.event.RegionTNTDetonateEvent;
import dev.byflow.customtntflow.model.RegionTNTType;
import dev.byflow.customtntflow.service.RegionTNTRegistry;
import dev.byflow.customtntflow.service.explosion.ExplosionPipeline;
import dev.byflow.customtntflow.service.explosion.FilterStage;
import dev.byflow.customtntflow.service.explosion.FinalizeStage;
import dev.byflow.customtntflow.service.explosion.GeometryStage;
import dev.byflow.customtntflow.service.explosion.RegionCheckStage;
import dev.byflow.customtntflow.service.region.RegionIntegrationService;
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
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RegionTNTListener implements Listener {

    private static final int BLOCKS_PER_TICK = 3000;

    private final CustomTNTFlowPlugin plugin;
    private final RegionTNTRegistry registry;
    private final ExplosionPipeline explosionPipeline;

    public RegionTNTListener(CustomTNTFlowPlugin plugin,
                             RegionTNTRegistry registry,
                             RegionIntegrationService regionIntegrationService) {
        this.plugin = plugin;
        this.registry = registry;
        this.explosionPipeline = new ExplosionPipeline(List.of(
                new GeometryStage(),
                new FilterStage(),
                new RegionCheckStage(regionIntegrationService),
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
            Player player = event.getPlayer();
            CustomTNTPrimeEvent primeEvent = new CustomTNTPrimeEvent(event.getBlockPlaced(), type, stack, player);
            Bukkit.getPluginManager().callEvent(primeEvent);
            if (primeEvent.isCancelled()) {
                return;
            }
            var block = event.getBlockPlaced();
            block.setType(Material.AIR, false);
            var world = block.getWorld();
            var location = block.getLocation().add(0.5, 0.0, 0.5);
            TNTPrimed primed = (TNTPrimed) world.spawnEntity(location, EntityType.TNT);
            primed.setSource(player);
            registry.applyToPrimed(primed, type, stack, player != null ? player.getUniqueId() : null);
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
        UUID ownerUuid = registry.resolveOwner(tnt).orElse(null);

        if (!primed.explodeInWater() && (tnt.isInWater() || event.getLocation().getBlock().isLiquid())) {
            event.blockList().clear();
            RegionTNTDetonateEvent waterEvent = callDetonateEvent(tnt, type, List.of());
            Bukkit.getPluginManager().callEvent(new CustomTNTExplodeEvent(tnt, type, waterEvent.getAffectedBlocks(), ownerUuid));
            return;
        }

        CustomTNTPreAffectEvent preEvent = new CustomTNTPreAffectEvent(tnt, type);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            event.blockList().clear();
            return;
        }

        RegionTNTType.BlockBehavior behavior = preEvent.getBehavior().toImmutable();
        registry.updateTraits(tnt, behavior);

        boolean hasAffectListeners = CustomTNTAffectEvent.getHandlerList().getRegisteredListeners().length > 0;
        boolean shouldCollect = behavior.breakBlocks() || behavior.apiOnly() || hasAffectListeners;
        boolean trace = plugin.getDebugSettings().traceExplosion();
        List<Block> candidateBlocks = shouldCollect
                ? explosionPipeline.process(tnt, type, behavior, plugin.getSLF4JLogger(), ownerUuid, trace)
                : new ArrayList<>();
        Set<Block> mutableBlocks = new LinkedHashSet<>(candidateBlocks);
        CustomTNTAffectEvent affectEvent = new CustomTNTAffectEvent(tnt, type, mutableBlocks);
        Bukkit.getPluginManager().callEvent(affectEvent);
        List<Block> finalBlocks = new ArrayList<>(affectEvent.getBlocks());
        int maxBlocks = behavior.maxBlocks();
        if (maxBlocks >= 0 && finalBlocks.size() > maxBlocks) {
            if (finalBlocks.size() > candidateBlocks.size()) {
                plugin.getSLF4JLogger().warn("Тип {} превысил лимит max-blocks ({}). Остальные блоки будут проигнорированы.", type.getId(), maxBlocks);
            }
            finalBlocks = new ArrayList<>(finalBlocks.subList(0, maxBlocks));
        }

        if (trace) {
            plugin.getPluginLogger().info("[trace:{}] После событий: кандидаты={}, финал={}", type.getId(), candidateBlocks.size(), finalBlocks.size());
        }

        RegionTNTDetonateEvent customEvent = callDetonateEvent(tnt, type, finalBlocks);
        event.blockList().clear();
        Bukkit.getPluginManager().callEvent(new CustomTNTExplodeEvent(tnt, type, customEvent.getAffectedBlocks(), ownerUuid));
        if (customEvent.isCancelled() || behavior.apiOnly()) {
            return;
        }
        if (!behavior.breakBlocks()) {
            return;
        }
        List<Block> blocksToBreak = new ArrayList<>(customEvent.getAffectedBlocks());
        if (maxBlocks >= 0 && blocksToBreak.size() > maxBlocks) {
            blocksToBreak = new ArrayList<>(blocksToBreak.subList(0, maxBlocks));
        }
        boolean dropBlocks = behavior.dropBlocks();
        Set<Material> dropBlacklist = behavior.dropBlacklist();
        processBlockBreaks(blocksToBreak, dropBlocks, dropBlacklist);
    }

    private RegionTNTDetonateEvent callDetonateEvent(TNTPrimed tnt, RegionTNTType type, List<Block> blocks) {
        RegionTNTDetonateEvent customEvent = new RegionTNTDetonateEvent(tnt, type, blocks);
        Bukkit.getPluginManager().callEvent(customEvent);
        return customEvent;
    }

    private void processBlockBreaks(List<Block> blocks, boolean dropBlocks, Set<Material> dropBlacklist) {
        if (blocks.isEmpty()) {
            return;
        }
        if (blocks.size() <= BLOCKS_PER_TICK) {
            breakBatch(blocks, dropBlocks, dropBlacklist);
            return;
        }
        List<Block> queue = new ArrayList<>(blocks);
        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < queue.size() && processed < BLOCKS_PER_TICK) {
                    breakBlock(queue.get(index++), dropBlocks, dropBlacklist);
                    processed++;
                }
                if (index >= queue.size()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void breakBatch(List<Block> blocks, boolean dropBlocks, Set<Material> dropBlacklist) {
        for (Block block : blocks) {
            breakBlock(block, dropBlocks, dropBlacklist);
        }
    }

    private void breakBlock(Block block, boolean dropBlocks, Set<Material> dropBlacklist) {
        var world = block.getWorld();
        int chunkX = block.getX() >> 4;
        int chunkZ = block.getZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }
        boolean shouldDrop = dropBlocks && (dropBlacklist == null || !dropBlacklist.contains(block.getType()));
        if (shouldDrop) {
            block.breakNaturally();
        } else {
            block.setType(Material.AIR, false);
        }
    }
}
