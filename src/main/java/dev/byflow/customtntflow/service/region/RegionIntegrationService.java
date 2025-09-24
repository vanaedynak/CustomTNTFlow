package dev.byflow.customtntflow.service.region;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.service.explosion.ExplosionContext;
import org.bukkit.block.Block;
import org.bukkit.plugin.PluginManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RegionIntegrationService {

    private final CustomTNTFlowPlugin plugin;
    private final Logger logger;
    private RegionCheckSettings settings = new RegionCheckSettings(false, RegionCheckMode.STRICT, "");
    private final List<RegionProtectionHook> hooks = new ArrayList<>();

    public RegionIntegrationService(CustomTNTFlowPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getSLF4JLogger();
    }

    public void reload() {
        this.settings = RegionCheckSettings.from(plugin.getConfig().getConfigurationSection("settings.region-checks"));
        hooks.clear();
        if (!settings.enabled()) {
            return;
        }
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        if (pluginManager.isPluginEnabled("WorldGuard")) {
            try {
                hooks.add(new WorldGuardHook());
            } catch (Throwable ex) {
                logger.warn("Не удалось инициализировать интеграцию с WorldGuard", ex);
            }
        }
        if (pluginManager.isPluginEnabled("ProtectionStones")) {
            try {
                hooks.add(new ProtectionStonesHook(logger));
            } catch (Throwable ex) {
                logger.warn("Не удалось инициализировать интеграцию с ProtectionStones", ex);
            }
        }
        if (hooks.isEmpty()) {
            logger.info("Регион-проверки активированы, но подходящих интеграций не найдено.");
        } else {
            String names = hooks.stream().map(RegionProtectionHook::name).collect(Collectors.joining(", "));
            logger.info("Регион-проверки активированы ({}), найдено интеграций: {}", settings.mode(), names);
        }
    }

    public boolean canAffect(ExplosionContext context, Block block) {
        if (!settings.enabled() || hooks.isEmpty()) {
            return true;
        }
        for (RegionProtectionHook hook : hooks) {
            try {
                if (!hook.canAffect(context, block, settings.mode())) {
                    if (!settings.denyMessage().isBlank()) {
                        context.sendDenyMessage(settings.denyMessage());
                    }
                    return false;
                }
            } catch (Throwable ex) {
                logger.warn("Ошибка проверки региона через {}: {}", hook.name(), ex.getMessage());
            }
        }
        return true;
    }

    public boolean isFeatureEnabled() {
        return settings.enabled();
    }

    public RegionCheckSettings settings() {
        return settings;
    }
}
