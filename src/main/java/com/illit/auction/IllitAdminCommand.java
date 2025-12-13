package com.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IllitAdminCommand implements CommandExecutor {

    private final IllitAuctionPlugin plugin;
    private final AuctionService service;
    private final YamlEconomy economy;

    public IllitAdminCommand(IllitAuctionPlugin plugin, AuctionService service, YamlEconomy economy) {
        this.plugin = plugin;
        this.service = service;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return help(sender);

        String root = args[0].toLowerCase();
        if (!root.equals("ah")) return help(sender);

        if (!sender.hasPermission("illit.ah.admin")) {
            sender.sendMessage(plugin.prefix() + "§cНет прав.");
            return true;
        }

        if (args.length < 2) return help(sender);
        String sub = args[1].toLowerCase();

        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                economy.reload();
                service.reload();
                sender.sendMessage(plugin.prefix() + "§aПерезагружено.");
                return true;
            }
            case "forceexpire" -> {
                service.expireSweep();
                sender.sendMessage(plugin.prefix() + "§aПросрочка обработана.");
                return true;
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.prefix() + "§cИспользование: /illit ah remove <listing_uuid>");
                    return true;
                }
                try {
                    UUID id = UUID.fromString(args[2]);
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(plugin.prefix() + "§cЭту команду лучше выполнять от игрока (OP).");
                        return true;
                    }
                    boolean ok = service.withdrawListing(id, p);
                    sender.sendMessage(plugin.prefix() + (ok ? "§aЛот удалён (вернётся продавцу)." : "§cЛот не найден/нет прав."));
                } catch (Exception e) {
                    sender.sendMessage(plugin.prefix() + "§cНеверный UUID.");
                }
                return true;
            }
            case "money" -> {
                if (args.length < 5) {
                    sender.sendMessage(plugin.prefix() + "§cИспользование: /illit ah money <set|add> <ник> <сумма>");
                    return true;
                }
                String act = args[2].toLowerCase();
                String nick = args[3];
                long amt;
                try { amt = Long.parseLong(args[4]); } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.prefix() + "§cСумма должна быть числом.");
                    return true;
                }
                var op = Bukkit.getOfflinePlayer(nick);
                if (act.equals("set")) {
                    economy.setBalance(op.getUniqueId(), Math.max(0, amt));
                    sender.sendMessage(plugin.prefix() + "§aБаланс установлен.");
                } else if (act.equals("add")) {
                    economy.deposit(op.getUniqueId(), Math.max(0, amt));
                    sender.sendMessage(plugin.prefix() + "§aБаланс пополнен.");
                } else {
                    sender.sendMessage(plugin.prefix() + "§cДействие: set/add");
                }
                return true;
            }
        }

        return help(sender);
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage(CC.t("&7[&f&lILLIT AH&7]&r &7Админ-команды:"));
        sender.sendMessage(CC.t("&f/illit ah reload &7— перезагрузка"));
        sender.sendMessage(CC.t("&f/illit ah forceexpire &7— принудительно обработать просрочку"));
        sender.sendMessage(CC.t("&f/illit ah remove <uuid> &7— удалить лот (вернётся продавцу)"));
        sender.sendMessage(CC.t("&f/illit ah money <set|add> <ник> <сумма> &7— временная внутренняя валюта"));
        return true;
    }
}
