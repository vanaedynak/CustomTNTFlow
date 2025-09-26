package dev.byflow.customtntflow.service.nbt;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import org.bukkit.entity.Entity;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Map;

public class NbtService {

    private final Logger logger;
    private final boolean available;
    private boolean warnedMissing;

    public NbtService(Logger logger) {
        this.logger = logger;
        this.available = detectAvailability();
        this.warnedMissing = false;
        if (available) {
            logger.info("NBT-API обнаружен — поддержка nbt-markers активна.");
        }
    }

    private boolean detectAvailability() {
        try {
            Class.forName("de.tr7zw.changeme.nbtapi.NBT");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public void applyEntityMarkers(Entity entity, Map<String, Object> markers) {
        if (entity == null || markers == null || markers.isEmpty()) {
            return;
        }
        if (!available) {
            if (!warnedMissing) {
                warnedMissing = true;
                logger.info("NBT-API не найден. Секция primed.nbt-markers будет проигнорирована.");
            }
            return;
        }
        NBT.modify(entity, nbt -> {
            markers.forEach((path, value) -> applyValue(nbt, path, value));
        });
    }

    private void applyValue(ReadWriteNBT root, String path, Object value) {
        if (path == null || path.isBlank() || value == null) {
            return;
        }
        String[] parts = path.split("\\.");
        ReadWriteNBT target = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part == null || part.isBlank()) {
                return;
            }
            target = target.getOrCreateCompound(part);
        }
        String key = parts[parts.length - 1];
        if (key == null || key.isBlank()) {
            return;
        }
        if (value instanceof Integer intValue) {
            target.setInteger(key, intValue);
        } else if (value instanceof Long longValue) {
            target.setLong(key, longValue);
        } else if (value instanceof Double doubleValue) {
            target.setDouble(key, doubleValue);
        } else if (value instanceof Float floatValue) {
            target.setFloat(key, floatValue);
        } else if (value instanceof Short shortValue) {
            target.setShort(key, shortValue);
        } else if (value instanceof Byte byteValue) {
            target.setByte(key, byteValue);
        } else if (value instanceof Boolean boolValue) {
            target.setBoolean(key, boolValue);
        } else {
            target.setString(key, stringify(value));
        }
    }

    private String stringify(Object value) {
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return String.format(Locale.ROOT, "%s", value);
    }
}
