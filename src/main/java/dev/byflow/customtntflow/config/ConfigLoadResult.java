package dev.byflow.customtntflow.config;

import dev.byflow.customtntflow.model.RegionTNTType;

import java.util.List;
import java.util.Map;

public record ConfigLoadResult(Map<String, RegionTNTType> types,
                               int mixinCount,
                               List<String> warnings) {
}
