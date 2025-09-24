package dev.byflow.customtntflow.config;

import dev.byflow.customtntflow.model.RegionTNTType;
import dev.byflow.customtntflow.model.TypeMetadata;

import java.util.List;
import java.util.Map;

public record ConfigLoadResult(Map<String, RegionTNTType> types,
                               Map<String, TypeMetadata> metadata,
                               int mixinCount,
                               List<String> warnings) {
}
