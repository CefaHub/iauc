package ru.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
            ViewContext ctx = new ViewContext();
            plugin.ctx().put(p.getUniqueId(), ctx);
            plugin.gui().openMain(p, ctx);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("my") || sub.equals("mine") || sub.equals("lots")) {
            plugin.gui().openMy(p, plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext()));
            return true;
        }

        if (sub.equals("search")) {
            if (args.length < 2) {
                p.sendMessage(U.c("&7[&f&lILLIT AH&7] &7Использование: &f/ah search <текст>"));
                return true;
            }
            ViewContext ctx = plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext());
            ctx.search = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            ctx.page = 0;
            plugin.gui().openMain(p, ctx);
            return true;
        }

        if (sub.equals("sell")) {
            if (args.length < 2) {
                p.sendMessage(U.c("&7[&f&lILLIT AH&7] &7Использование: &f/ah sell <цена> [кол-во]"));
                return true;
            }
            double price;
            try { price = Double.parseDouble(args[1].replace(",", ".")); } catch (Exception e) { price = -1; }
            if (price <= 0) {
                p.sendMessage(U.c("&7[&f&lILLIT AH&7] &cЦена должна быть > 0"));
                return true;
            }
            int amount = -1;
            if (args.length >= 3) {
                try { amount = Integer.parseInt(args[2]); } catch (Exception ignored) {}
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

            int max = maxListings(p);
            if (max != -1) {
                int active = service.countActiveListings(p.getUniqueId());
                if (active >= max) {
                    p.sendMessage(U.c("&7[&f&lILLIT AH&7] &cВы уже выставили максимум лотов: &f" + max));
                    return true;
                }
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

        // /ah <ник>
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.gui().openPlayer(p, plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext()), target);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return java.util.List.of("sell","search","my");
        return java.util.List.of();
    }
    private int maxListings(org.bukkit.entity.Player p) {
        // luckperms: illit.auction.limit.<number>  (берём максимальное число),
        // либо illit.auction.limit.unlimited
        int best = -1;
        for (var perm : p.getEffectivePermissions()) {
            if (!perm.getValue()) continue;
            String n = perm.getPermission();
            if (n.equalsIgnoreCase("illit.auction.limit.unlimited")) return -1;
            if (n.startsWith("illit.auction.limit.")) {
                String tail = n.substring("illit.auction.limit.".length());
                try {
                    int v = Integer.parseInt(tail);
                    if (v > best) best = v;
                } catch (NumberFormatException ignored) { }
            }
        }
        return best == -1 ? 5 : best;
    }

}
