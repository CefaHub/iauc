package ru.illit.auction;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class AhSearchCommand implements CommandExecutor {
    private final IllitAuctionPlugin plugin;
    public AhSearchCommand(IllitAuctionPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игрока.");
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(U.c("&7[&f&lILLIT AH&7] &7Использование: &f/ah search <текст>"));
            return true;
        }
        ViewContext ctx = plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext());
        ctx.search = String.join(" ", args);
        ctx.page = 0;
        plugin.gui().openMain(p, ctx);
        return true;
    }
}
