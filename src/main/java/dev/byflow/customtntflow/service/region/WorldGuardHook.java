package dev.byflow.customtntflow.service.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.byflow.customtntflow.service.explosion.ExplosionContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WorldGuardHook implements RegionProtectionHook {

    private final WorldGuardPlugin plugin;
    private final RegionContainer container;

    public WorldGuardHook() {
        this.plugin = WorldGuardPlugin.inst();
        WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
        this.container = platform.getRegionContainer();
    }

    @Override
    public String name() {
        return "WorldGuard";
    }

    @Override
    public boolean canAffect(ExplosionContext context, Block block, RegionCheckMode mode) {
        RegionQuery query = container.createQuery();
        Location location = BukkitAdapter.adapt(block.getLocation());
        ApplicableRegionSet regions = query.getApplicableRegions(location);
        if (regions.size() == 0) {
            return true;
        }
        if (mode == RegionCheckMode.STRICT) {
            return regions.testState(null, Flags.TNT);
        }
        if (hasAccess(regions, context.ownerUuid())) {
            return true;
        }
        if (hasAccess(regions, context.sourceUuid())) {
            return true;
        }
        LocalPlayer player = resolvePlayer(context);
        if (player != null && regions.testState(player, Flags.TNT)) {
            return true;
        }
        return false;
    }

    private boolean hasAccess(ApplicableRegionSet regions, UUID uuid) {
        if (uuid == null) {
            return false;
        }
        for (ProtectedRegion region : regions) {
            DefaultDomain owners = region.getOwners();
            if (owners.contains(uuid)) {
                return true;
            }
            DefaultDomain members = region.getMembers();
            if (members.contains(uuid)) {
                return true;
            }
        }
        return false;
    }

    private LocalPlayer resolvePlayer(ExplosionContext context) {
        Entity source = context.tnt().getSource();
        if (source instanceof Player player) {
            return plugin.wrapPlayer(player);
        }
        UUID ownerUuid = context.ownerUuid();
        if (ownerUuid != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
            if (owner.isOnline() && owner.getPlayer() != null) {
                return plugin.wrapPlayer(owner.getPlayer());
            }
        }
        UUID sourceUuid = context.sourceUuid();
        if (sourceUuid != null) {
            OfflinePlayer other = Bukkit.getOfflinePlayer(sourceUuid);
            if (other.isOnline() && other.getPlayer() != null) {
                return plugin.wrapPlayer(other.getPlayer());
            }
        }
        return null;
    }
}
