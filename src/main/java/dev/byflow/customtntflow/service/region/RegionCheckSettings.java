package dev.byflow.customtntflow.service.region;

import org.bukkit.configuration.ConfigurationSection;

public record RegionCheckSettings(boolean enabled, RegionCheckMode mode, String denyMessage) {

    public static RegionCheckSettings from(ConfigurationSection section) {
        if (section == null) {
            return new RegionCheckSettings(false, RegionCheckMode.STRICT, "");
        }
        boolean enabled = section.getBoolean("enabled", false);
        RegionCheckMode mode = RegionCheckMode.from(section.getString("mode"));
        String denyMessage = section.getString("deny-message", "");
        return new RegionCheckSettings(enabled, mode, denyMessage == null ? "" : denyMessage);
    }
}
