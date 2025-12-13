package ru.illit.auction;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class AhSellCommand implements CommandExecutor {
    private final IllitAuctionPlugin plugin;

    public AhSellCommand(IllitAuctionPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игрока.");
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(U.c("&7[&f&lILLIT AH&7] &7Использование: &f/ah sell <цена> [кол-во]"));
            return true;
        }
        double price;
        try { price = Double.parseDouble(args[0].replace(",", ".")); } catch (Exception e) { price = -1; }
        if (price <= 0) {
            p.sendMessage(U.c("&7[&f&lILLIT AH&7] &cЦена должна быть > 0"));
            return true;
        }
        int amount = -1;
        if (args.length >= 2) {
            try { amount = Integer.parseInt(args[1]); } catch (Exception ignored) {}
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            p.sendMessage(U.c("&7[&f&lILLIT AH&7] &cВозьмите предмет в руку."));
            return true;
        }
        if (amount <= 0) amount = hand.getAmount();
        amount = Math.min(amount, hand.getAmount());
        if (amount <= 0) {
            p.sendMessage(U.c("&7[&f&lILLIT AH&7] &cНекорректное количество."));
            return true;
        }

        // remove from hand
        if (hand.getAmount() == amount) p.getInventory().setItemInMainHand(null);
        else {
            hand.setAmount(hand.getAmount() - amount);
            p.getInventory().setItemInMainHand(hand);
        }

        plugin.service().createListing(p, hand, amount, price);
        p.sendMessage(U.c("&7[&f&lILLIT AH&7] &aЛот выставлен: &f" + amount + " &7шт. по &f" + U.fmt((long) price) + " &7айлитики"));
        return true;
    }
}
