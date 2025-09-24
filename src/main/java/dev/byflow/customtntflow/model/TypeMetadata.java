package dev.byflow.customtntflow.model;

import java.util.List;
import java.util.Map;

public record TypeMetadata(String id,
                           String parentId,
                           List<MixinMetadata> mixins,
                           boolean defaultsApplied) {

    public TypeMetadata {
        mixins = mixins == null ? List.of() : List.copyOf(mixins);
    }

    public boolean hasParent() {
        return parentId != null && !parentId.isBlank();
    }

    public record MixinMetadata(String name, Map<String, Object> overrides) {
        public MixinMetadata {
            overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
        }

        public boolean hasOverrides() {
            return !overrides.isEmpty();
        }
    }
}
