package com.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AhCommand implements CommandExecutor {

    private final IllitAuctionPlugin plugin;
    private final AuctionService service;
    private final GuiManager gui;
    private final SessionStore sessions;

    public AhCommand(IllitAuctionPlugin plugin, AuctionService service, GuiManager gui, SessionStore sessions) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
        this.sessions = sessions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Эта команда только для игроков.");
            return true;
        }
        if (!p.hasPermission("illit.ah.use")) {
            p.sendMessage(plugin.prefix() + "§cНет прав.");
            return true;
        }

        if (args.length == 0) {
            gui.openAuction(p, sessions.get(p));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("sell") || sub.equals("продать")) {
            if (args.length < 2) {
                p.sendMessage(plugin.prefix() + "§cИспользование: /ah sell <цена_за_1> [кол-во]");
                return true;
            }
            long price;
            try { price = Long.parseLong(args[1]); } catch (NumberFormatException e) {
                p.sendMessage(plugin.prefix() + "§cЦена должна быть числом.");
                return true;
            }
            if (price < 0) {
                p.sendMessage(plugin.prefix() + "§cЦена должна быть >= 0.");
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                p.sendMessage(plugin.prefix() + "§cВозьми предмет в руку.");
                return true;
            }

            int amount = hand.getAmount();
            if (args.length >= 3) {
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    p.sendMessage(plugin.prefix() + "§cКоличество должно быть числом.");
                    return true;
                }
            }

            if (amount <= 0) {
                p.sendMessage(plugin.prefix() + "§cКоличество должно быть > 0.");
                return true;
            }
            if (amount > hand.getAmount()) {
                p.sendMessage(plugin.prefix() + "§cНельзя продать больше, чем в руке.");
                return true;
            }
            if (amount > 64) {
                p.sendMessage(plugin.prefix() + "§cМаксимум 64 за один лот.");
                return true;
            }

            try {
                ItemStack sold = hand.clone();
                sold.setAmount(amount);
                hand.setAmount(hand.getAmount() - amount);
                p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);

                service.createListing(p, sold, amount, price);
                p.sendMessage(plugin.prefix() + "§aЛот выставлен на аукцион.");
            } catch (IllegalStateException e) {
                p.sendMessage(plugin.prefix() + "§cЛимит лотов на игрока.");
            } catch (Exception e) {
                p.sendMessage(plugin.prefix() + "§cОшибка выставления лота.");
            }
            return true;
        }

        if (sub.equals("search") || sub.equals("поиск")) {
            if (args.length < 2) {
                p.sendMessage(plugin.prefix() + "§cИспользование: /ah search <текст>");
                return true;
            }
            String q = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            GuiSession s = sessions.get(p);
            s.page = 0;
            s.sort = AuctionService.SortMode.DATE_NEW;
            gui.openSearch(p, q, s);
            return true;
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
        if (op != null) {
            GuiSession s = sessions.get(p);
            s.page = 0;
            s.sort = AuctionService.SortMode.DATE_NEW;
            gui.openPlayerAuction(p, op.getUniqueId(), op.getName(), s);
            return true;
        }

        gui.openAuction(p, sessions.get(p));
        return true;
    }
}
