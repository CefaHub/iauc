package ru.illit.auction;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public final class AuctionListener implements Listener {
    private final IllitAuctionPlugin plugin;

    public AuctionListener(IllitAuctionPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (!title.equals(GuiManager.TITLE_MAIN) && !title.equals(GuiManager.TITLE_MY) && !title.equals(GuiManager.TITLE_BUY)
                && !title.startsWith(U.c("&f&lАукцион &7- &f"))) return;

        e.setCancelled(true);
        ItemStack it = e.getCurrentItem();
        if (it == null) return;

        if (title.equals(GuiManager.TITLE_MAIN) || title.startsWith(U.c("&f&lАукцион &7- &f"))) {
            // buttons
            int slot = e.getRawSlot();
            ViewContext ctx = plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext());

            if (slot == 49) { // refresh
                plugin.service().reload();
                plugin.gui().openMain(p, ctx);
                return;
            }
            if (slot == 50) { // my
                plugin.gui().openMy(p, ctx);
                return;
            }
            if (slot == 52) { ctx.page = Math.max(0, ctx.page-1); plugin.gui().openMain(p, ctx); return; }
            if (slot == 53) { ctx.page = ctx.page+1; plugin.gui().openMain(p, ctx); return; }
            if (slot == 46) {
                if (e.isLeftClick()) ctx.sort = AuctionService.Sort.CHEAP;
                else if (e.isRightClick() && e.isShiftClick()) ctx.sort = AuctionService.Sort.OLD;
                else if (e.isRightClick()) ctx.sort = AuctionService.Sort.NEW;
                plugin.gui().openMain(p, ctx);
                return;
            }

            String id = GuiManager.listingIdFromItem(it);
            if (id != null) {
                Listing l = plugin.service().getById(id);
                if (l != null) plugin.gui().openBuyAmount(p, l);
            }
            return;
        }

        if (title.equals(GuiManager.TITLE_MY)) {
            int slot = e.getRawSlot();
            if (slot == 49) {
                plugin.gui().openMain(p, plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext()));
                return;
            }
            String id = GuiManager.listingIdFromItem(it);
            if (id != null) {
                var res = plugin.service().reclaim(p, id);
                p.sendMessage(U.c("&7[&f&lILLIT AH&7] " + (res.ok() ? "&a" : "&c") + res.msg()));
                plugin.gui().openMy(p, plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext()));
            }
            return;
        }

        if (title.equals(GuiManager.TITLE_BUY)) {
            int slot = e.getRawSlot();
            var st = plugin.buy().get(p.getUniqueId());
            if (st == null) return;
            Listing l = plugin.service().getById(st.listingId());
            if (l == null) { p.closeInventory(); return; }

            int amt = st.amount();

            if (slot == 11) { // +1/+16
                int add = e.isShiftClick() ? 16 : 1;
                amt = Math.min(l.amountRemaining, Math.min(64, amt + add));
                plugin.buy().put(p.getUniqueId(), new GuiManager.BuyState(st.listingId(), amt));
                p.sendMessage(U.c("&7[&f&lILLIT AH&7] &7Количество: &f" + amt));
                return;
            }
            if (slot == 15) { // -1/-16
                int sub = e.isShiftClick() ? 16 : 1;
                amt = Math.max(1, amt - sub);
                plugin.buy().put(p.getUniqueId(), new GuiManager.BuyState(st.listingId(), amt));
                p.sendMessage(U.c("&7[&f&lILLIT AH&7] &7Количество: &f" + amt));
                return;
            }
            if (slot == 22) { // buy
                var res = plugin.service().buy(p, st.listingId(), amt);
                p.sendMessage(U.c("&7[&f&lILLIT AH&7] " + (res.ok() ? "&a" : "&c") + res.msg()));
                plugin.service().reload();
                ViewContext ctx = plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext());
                plugin.gui().openMain(p, ctx);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals(GuiManager.TITLE_BUY)) {
            plugin.buy().remove(e.getPlayer().getUniqueId());
        }
    }
}
