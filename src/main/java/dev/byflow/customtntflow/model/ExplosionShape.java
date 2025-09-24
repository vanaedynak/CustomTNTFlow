package dev.byflow.customtntflow.model;

public enum ExplosionShape {
    SPHERE,
    CUBE;

    public static ExplosionShape fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return SPHERE;
        }
        try {
            return ExplosionShape.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SPHERE;
        }
    }
}
