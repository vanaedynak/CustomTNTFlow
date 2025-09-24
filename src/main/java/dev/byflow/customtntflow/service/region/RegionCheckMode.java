package dev.byflow.customtntflow.service.region;

public enum RegionCheckMode {
    STRICT,
    LENIENT;

    public static RegionCheckMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return STRICT;
        }
        try {
            return RegionCheckMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return STRICT;
        }
    }
}
