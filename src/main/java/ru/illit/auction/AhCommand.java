package ru.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class AhCommand implements CommandExecutor, TabCompleter {
    private final IllitAuctionPlugin plugin;

    public AhCommand(IllitAuctionPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игрока.");
            return true;
        }
        if (args.length == 0) {
            plugin.gui().openMain(p, plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext()));
            return true;
        }
        if (args[0].equalsIgnoreCase("sell")) {
            sender.sendMessage(U.c("&7[&f&lILLIT AH&7] &cИспользование: &f/ah sell <цена> [кол-во]"));
            return true;
        }
        if (args[0].equalsIgnoreCase("search")) {
            sender.sendMessage(U.c("&7[&f&lILLIT AH&7] &cИспользование: &f/ah search <текст>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("my")) {
            plugin.gui().openMy(p, plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext()));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.gui().openPlayer(p, plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext()), target);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return java.util.List.of("sell","search","my");
        return java.util.List.of();
    }
}
