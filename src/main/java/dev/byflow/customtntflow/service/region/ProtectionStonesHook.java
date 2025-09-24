package dev.byflow.customtntflow.service.region;

import dev.byflow.customtntflow.service.explosion.ExplosionContext;
import org.bukkit.block.Block;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class ProtectionStonesHook implements RegionProtectionHook {

    private final Logger logger;
    private final Method fromLocationMethod;
    private final Method isOwnerMethod;
    private final Method isMemberMethod;

    public ProtectionStonesHook(Logger logger) throws ClassNotFoundException, NoSuchMethodException {
        this.logger = logger;
        Class<?> regionClass = Class.forName("dev.espi.protectionstones.PSRegion");
        this.fromLocationMethod = regionClass.getMethod("fromLocation", org.bukkit.Location.class);
        this.isOwnerMethod = regionClass.getMethod("isOwner", UUID.class);
        this.isMemberMethod = regionClass.getMethod("isMember", UUID.class);
    }

    @Override
    public String name() {
        return "ProtectionStones";
    }

    @Override
    public boolean canAffect(ExplosionContext context, Block block, RegionCheckMode mode) {
        Object region;
        try {
            region = fromLocationMethod.invoke(null, block.getLocation());
        } catch (IllegalAccessException | InvocationTargetException ex) {
            logger.warn("Не удалось получить регион ProtectionStones: {}", ex.getMessage());
            return true;
        }
        if (region == null) {
            return true;
        }
        if (mode == RegionCheckMode.STRICT) {
            return false;
        }
        if (hasAccess(region, context.ownerUuid())) {
            return true;
        }
        if (hasAccess(region, context.sourceUuid())) {
            return true;
        }
        return false;
    }

    private boolean hasAccess(Object region, UUID uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            Boolean owner = (Boolean) isOwnerMethod.invoke(region, uuid);
            if (owner != null && owner) {
                return true;
            }
            Boolean member = (Boolean) isMemberMethod.invoke(region, uuid);
            return member != null && member;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            logger.warn("Ошибка проверки доступа ProtectionStones: {}", ex.getMessage());
            return false;
        }
    }
}
