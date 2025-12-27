package ru.illit.auction;

import org.bukkit.command.*;

public final class IllitAhAdminCommand implements CommandExecutor {
    private final IllitAuctionPlugin plugin;
    public IllitAhAdminCommand(IllitAuctionPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("illit.auction")) {
            sender.sendMessage(U.c("&7[&f&lILLIT AUCTION&7] &cНет прав."));
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("ah") && args[1].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.service().reload();
            sender.sendMessage(U.c("&7[&f&lILLIT AUCTION&7] &aПерезагружено."));
            return true;
        }
        sender.sendMessage(U.c("&7[&f&lILLIT AUCTION&7] &7Команды: &f/illit ah reload"));
        return true;
    }
}
