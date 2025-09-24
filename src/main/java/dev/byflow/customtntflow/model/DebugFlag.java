package dev.byflow.customtntflow.model;

import java.util.Locale;

public enum DebugFlag {
    LOG_COMPILED_TYPES("log-compiled-types"),
    LOG_MERGE_SOURCES("log-merge-sources"),
    TRACE_EXPLOSION("trace-explosion");

    private final String configKey;

    DebugFlag(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }

    public static DebugFlag fromKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace('_', '-');
        for (DebugFlag flag : values()) {
            if (flag.configKey.equals(normalized)) {
                return flag;
            }
        }
        return null;
    }
}
