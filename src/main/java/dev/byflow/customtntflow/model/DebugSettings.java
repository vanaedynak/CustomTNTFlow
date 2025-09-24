package dev.byflow.customtntflow.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record DebugSettings(boolean logCompiledTypes,
                            boolean logMergeSources,
                            boolean traceExplosion) {

    public static DebugSettings defaults() {
        return new DebugSettings(false, false, false);
    }

    public static DebugSettings fromConfig(FileConfiguration config) {
        if (config == null) {
            return defaults();
        }
        ConfigurationSection section = config.getConfigurationSection("settings.debug");
        if (section == null) {
            return defaults();
        }
        boolean logCompiled = section.getBoolean("log-compiled-types", false);
        boolean logMerge = section.getBoolean("log-merge-sources", false);
        boolean trace = section.getBoolean("trace-explosion", false);
        return new DebugSettings(logCompiled, logMerge, trace);
    }

    public boolean isEnabled(DebugFlag flag) {
        return switch (flag) {
            case LOG_COMPILED_TYPES -> logCompiledTypes;
            case LOG_MERGE_SOURCES -> logMergeSources;
            case TRACE_EXPLOSION -> traceExplosion;
        };
    }

    public DebugSettings toggle(DebugFlag flag) {
        return switch (flag) {
            case LOG_COMPILED_TYPES -> new DebugSettings(!logCompiledTypes, logMergeSources, traceExplosion);
            case LOG_MERGE_SOURCES -> new DebugSettings(logCompiledTypes, !logMergeSources, traceExplosion);
            case TRACE_EXPLOSION -> new DebugSettings(logCompiledTypes, logMergeSources, !traceExplosion);
        };
    }

    public DebugSettings withFlag(DebugFlag flag, boolean value) {
        return switch (flag) {
            case LOG_COMPILED_TYPES -> new DebugSettings(value, logMergeSources, traceExplosion);
            case LOG_MERGE_SOURCES -> new DebugSettings(logCompiledTypes, value, traceExplosion);
            case TRACE_EXPLOSION -> new DebugSettings(logCompiledTypes, logMergeSources, value);
        };
    }
}
