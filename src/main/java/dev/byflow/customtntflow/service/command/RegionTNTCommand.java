package dev.byflow.customtntflow.service.command;

import dev.byflow.customtntflow.CustomTNTFlowPlugin;
import dev.byflow.customtntflow.model.RegionTNTType;
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
import java.util.List;
import java.util.Locale;

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
        for (RegionTNTType type : registry.getTypes()) {
            sender.sendMessage(ChatColor.YELLOW + " - " + type.getId());
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
        sender.sendMessage(ChatColor.GOLD + "Информация о типе " + type.getId());
        sender.sendMessage(ChatColor.GRAY + "Материал предмета: " + item.material());
        sender.sendMessage(ChatColor.GRAY + "Отображаемое имя: " + (item.displayName() != null ? item.displayName() : "(не задано)"));
        sender.sendMessage(ChatColor.GRAY + "Взрыв радиус: " + behavior.radius());
        sender.sendMessage(ChatColor.GRAY + "Ломать блоки: " + behavior.breakBlocks());
        sender.sendMessage(ChatColor.GRAY + "Только по whitelist: " + behavior.whitelistOnly());
        sender.sendMessage(ChatColor.GRAY + "API-only режим: " + behavior.apiOnly());
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
            return;
        }
        plugin.reloadEverything();
        sender.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("give", "list", "info", "reload")) {
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
        return suggestions;
    }
}
