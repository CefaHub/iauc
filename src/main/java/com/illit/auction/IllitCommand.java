package com.illit.auction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class IllitCommand implements CommandExecutor {

    private final IllitAuctionPlugin plugin;

    public IllitCommand(IllitAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    private String prefix() { return plugin.prefix(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(prefix() + "§7Админ: /illit ah <reload|remove|purge>");
            return true;
        }

        if (!args[0].equalsIgnoreCase("ah")) {
            sender.sendMessage(prefix() + "§7Админ: /illit ah <reload|remove|purge>");
            return true;
        }

        if (!sender.hasPermission("illit.ah.admin")) {
            sender.sendMessage(prefix() + "§cНет прав.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix() + "§7Админ: /illit ah <reload|remove|purge>");
            return true;
        }

        String sub = args[1].toLowerCase();

        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(prefix() + "§aКонфиг перезагружен.");
                return true;
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(prefix() + "§cИспользование: /illit ah remove <id>");
                    return true;
                }
                String id = args[2];
                if (plugin.store().get(id) == null) {
                    sender.sendMessage(prefix() + "§cЛот не найден.");
                    return true;
                }
                plugin.store().remove(id);
                plugin.markDirty();
                sender.sendMessage(prefix() + "§aЛот удалён: §f" + id);
                return true;
            }
            case "purge" -> {
                int removed = plugin.service().purgeEmpty();
                plugin.markDirty();
                sender.sendMessage(prefix() + "§aОчистка выполнена. Удалено пустых лотов: §f" + removed);
                return true;
            }
            default -> {
                sender.sendMessage(prefix() + "§7Админ: /illit ah <reload|remove|purge>");
                return true;
            }
        }
    }
}
