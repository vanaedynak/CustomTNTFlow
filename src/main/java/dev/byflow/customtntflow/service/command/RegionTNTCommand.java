package dev.byflow.customtntflow.service.command;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.model.DebugFlag;
import dev.byflow.customtntflow.model.DebugSettings;
import dev.byflow.customtntflow.model.RegionTNTType;
import dev.byflow.customtntflow.model.TypeMetadata;
import dev.byflow.customtntflow.service.RegionTNTRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionTNTCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "customtntflow.admin";

    private final CustomTNTFlowPlugin plugin;
    private final RegionTNTRegistry registry;

    public RegionTNTCommand(CustomTNTFlowPlugin plugin, RegionTNTRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> handleGive(sender, label, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender, label, args);
            case "debug" -> handleDebug(sender, label, args);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Custom TNT Flow" + ChatColor.GRAY + " — доступные команды:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <player> <type> [amount]" + ChatColor.GRAY + " — выдать TNT");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list" + ChatColor.GRAY + " — показать список типов");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <type>" + ChatColor.GRAY + " — подробности по типу");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " — перезагрузить конфиг");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug [flag]" + ChatColor.GRAY + " — показать/переключить debug-флаги");
    }

    private void handleGive(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Использование: /" + label + " give <player> <type> [amount]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден: " + args[1]);
            return;
        }
        RegionTNTType type = registry.getType(args[2]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Неизвестный тип TNT: " + args[2]);
            sender.sendMessage(ChatColor.GRAY + "Доступные типы: " + String.join(", ", registry.getTypes().stream().map(RegionTNTType::getId).toList()));
            return;
        }
        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Неверное количество: " + args[3]);
                return;
            }
        }
        ItemStack stack = registry.createItem(type, amount);
        target.getInventory().addItem(stack);
        target.sendMessage(ChatColor.GREEN + "Вы получили " + amount + " шт. TNT типа " + type.getId());
        if (!target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Выдано " + amount + " шт. TNT типа " + type.getId() + " игроку " + target.getName());
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
            return;
        }
        if (registry.getTypes().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "В конфиге не описано ни одного типа.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Типы TNT (" + registry.getTypes().size() + "):");
        Map<String, TypeMetadata> metadata = registry.getMetadataSnapshot();
        for (RegionTNTType type : registry.getTypes()) {
            StringBuilder line = new StringBuilder();
            line.append(ChatColor.YELLOW).append(" - ").append(type.getId());
            TypeMetadata meta = metadata.get(type.getId());
            if (meta != null && meta.hasParent()) {
                line.append(ChatColor.GRAY).append(" (extends: ").append(meta.parentId()).append(')');
            }
            if (meta != null && !meta.mixins().isEmpty()) {
                line.append(ChatColor.GRAY).append(" (use: ").append(formatMixins(meta.mixins())).append(')');
            }
            sender.sendMessage(line.toString());
        }
    }

    private void handleInfo(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Использование: /" + label + " info <type>");
            return;
        }
        RegionTNTType type = registry.getType(args[1]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Неизвестный тип: " + args[1]);
            return;
        }
        var item = type.getItemSettings();
        var behavior = type.getBlockBehavior();
        var primed = type.getPrimedSettings();
        TypeMetadata metadata = registry.getMetadata(args[1]).orElse(null);
        List<String> inheritance = registry.resolveInheritanceChain(type.getId());
        sender.sendMessage(ChatColor.GOLD + "Информация о типе " + type.getId());
        sender.sendMessage(ChatColor.GRAY + "Материал предмета: " + item.material());
        sender.sendMessage(ChatColor.GRAY + "Отображаемое имя: " + (item.displayName() != null ? item.displayName() : "(не задано)"));
        sender.sendMessage(ChatColor.GRAY + "Наследование: " + (inheritance.isEmpty() ? "(нет)" : String.join(" -> ", inheritance)));
        sender.sendMessage(ChatColor.GRAY + "Миксины: " + (metadata != null && !metadata.mixins().isEmpty() ? formatMixins(metadata.mixins()) : "(нет)"));
        sender.sendMessage(ChatColor.GRAY + "Defaults: " + (metadata != null && metadata.defaultsApplied() ? "включены" : "нет"));
        sender.sendMessage(ChatColor.GRAY + "Радиус: " + String.format(Locale.ROOT, "%.1f", behavior.radius()) + " (" + behavior.shape() + ")");
        sender.sendMessage(ChatColor.GRAY + "Ломать блоки: " + formatBoolean(behavior.breakBlocks()));
        sender.sendMessage(ChatColor.GRAY + "Дроп блоков: " + formatBoolean(behavior.dropBlocks()));
        sender.sendMessage(ChatColor.GRAY + "Белый список: " + formatBoolean(behavior.whitelistOnly()));
        sender.sendMessage(ChatColor.GRAY + "Whitelist: " + formatMaterials(behavior.whitelist()));
        sender.sendMessage(ChatColor.GRAY + "Blacklist: " + formatMaterials(behavior.blacklist()));
        sender.sendMessage(ChatColor.GRAY + "Обсидиан: " + formatBoolean(behavior.allowObsidian()) + " | Плачущий: " + formatBoolean(behavior.allowCryingObsidian()));
        sender.sendMessage(ChatColor.GRAY + "Жидкости: " + formatBoolean(behavior.allowFluids()));
        sender.sendMessage(ChatColor.GRAY + "Max blocks: " + behavior.maxBlocks());
        sender.sendMessage(ChatColor.GRAY + "API-only: " + formatBoolean(behavior.apiOnly()));
        sender.sendMessage(ChatColor.GRAY + "Взрыв в воде: " + formatBoolean(primed.explodeInWater()));
        if (!type.getScoreboardTags().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Scoreboard tags: " + String.join(", ", type.getScoreboardTags()));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
            return;
        }
        plugin.reloadEverything();
        sender.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена.");
        sender.sendMessage(ChatColor.GRAY + "Типов: " + registry.getTypes().size()
                + ", миксинов: " + registry.getLastMixinCount()
                + ", предупреждений: " + registry.getLastWarnings().size()
                + ", ошибок: " + registry.getLastErrors().size());
        if (!registry.getLastErrors().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Ошибки:");
            for (String error : registry.getLastErrors()) {
                sender.sendMessage(ChatColor.RED + " - " + error);
            }
        }
        if (!registry.getLastWarnings().isEmpty()) {
            for (String warning : registry.getLastWarnings()) {
                sender.sendMessage(ChatColor.YELLOW + " - " + warning);
            }
        }
    }

    private void handleDebug(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
            return;
        }
        if (args.length == 1) {
            DebugSettings settings = plugin.getDebugSettings();
            sender.sendMessage(ChatColor.GOLD + "Debug-флаги:");
            for (DebugFlag flag : DebugFlag.values()) {
                sender.sendMessage(formatDebugFlag(flag, settings.isEnabled(flag)));
            }
            sender.sendMessage(ChatColor.GRAY + "Используйте /" + label + " debug <flag> для переключения.");
            return;
        }
        DebugFlag flag = DebugFlag.fromKey(args[1]);
        if (flag == null) {
            sender.sendMessage(ChatColor.RED + "Неизвестный debug-флаг: " + args[1]);
            return;
        }
        DebugSettings updated = plugin.toggleDebugFlag(flag);
        sender.sendMessage(ChatColor.YELLOW + "Флаг " + flag.configKey() + " теперь " + (updated.isEnabled(flag) ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("give", "list", "info", "reload", "debug")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    suggestions.add(player.getName());
                }
            }
            return suggestions;
        }
        if ((args.length == 3 && args[0].equalsIgnoreCase("give")) || (args.length == 2 && args[0].equalsIgnoreCase("info"))) {
            for (RegionTNTType type : registry.getTypes()) {
                if (type.getId().toLowerCase(Locale.ROOT).startsWith(args[args.length - 1].toLowerCase(Locale.ROOT))) {
                    suggestions.add(type.getId());
                }
            }
            return suggestions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            for (DebugFlag flag : DebugFlag.values()) {
                if (flag.configKey().startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    suggestions.add(flag.configKey());
                }
            }
            return suggestions;
        }
        return suggestions;
    }

    private String formatMixins(List<TypeMetadata.MixinMetadata> mixins) {
        return mixins.stream().map(this::formatMixin).collect(Collectors.joining(", "));
    }

    private String formatMixin(TypeMetadata.MixinMetadata mixin) {
        if (mixin == null) {
            return "";
        }
        if (!mixin.hasOverrides()) {
            return mixin.name();
        }
        return mixin.name() + formatOverrides(mixin.overrides());
    }

    private String formatOverrides(Map<String, Object> overrides) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=');
            builder.append(formatOverrideValue(entry.getValue()));
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    private String formatOverrideValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = map.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
            return formatOverrides(normalized);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
        }
        if (value instanceof Set<?> set) {
            return set.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
        }
        return String.valueOf(value);
    }

    private String formatBoolean(boolean value) {
        return value ? ChatColor.GREEN + "Да" + ChatColor.GRAY : ChatColor.RED + "Нет" + ChatColor.GRAY;
    }

    private String formatMaterials(Set<?> materials) {
        if (materials == null || materials.isEmpty()) {
            return "[]";
        }
        return materials.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatDebugFlag(DebugFlag flag, boolean enabled) {
        return ChatColor.YELLOW + " - " + flag.configKey() + ChatColor.GRAY + ": " + (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
    }
}
