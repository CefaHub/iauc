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

    private static final String PREFIX_PLAYER = U.c("&f&lАукцион &7- &f");

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = e.getView().getTitle();

        boolean isMain = title.equals(GuiManager.TITLE_MAIN);
        boolean isMy = title.equals(GuiManager.TITLE_MY);
        boolean isBuy = title.equals(GuiManager.TITLE_BUY);
        boolean isErr = title.equals(GuiManager.TITLE_CANNOT_BUY);
        boolean isPlayerView = title.startsWith(PREFIX_PLAYER) && !isMy && !isBuy;

        if (!isMain && !isMy && !isBuy && !isErr && !isPlayerView) return;

        e.setCancelled(true);

        ItemStack it = e.getCurrentItem();
        if (it == null) return;

        // 0) ERROR menu
        if (isErr) {
            int slot = e.getRawSlot();
            if (slot == 26) { p.closeInventory(); }
            return;
        }

        // 1) BUY menu (quantity)
        if (isBuy) {
            int slot = e.getRawSlot();
            var st = plugin.buy().get(p.getUniqueId());
            if (st == null) return;

            Listing l = plugin.service().getById(st.listingId());
            if (l == null) { p.closeInventory(); return; }

            int amt = st.amount();

            // кнопки изменения количества
            if (slot == 11) {
                // ЛКМ: +1, Shift+ЛКМ: +16
                if (!e.isLeftClick()) return;
                int add = e.isShiftClick() ? 16 : 1;
                amt = Math.min(l.amountRemaining, Math.min(64, amt + add));
                plugin.buy().put(p.getUniqueId(), new GuiManager.BuyState(st.listingId(), amt));
                plugin.gui().updateBuyView(e.getView().getTopInventory(), l, amt);
                return;
            }

            if (slot == 15) {
                // ПКМ: -1, Shift+ПКМ: -16
                if (!e.isRightClick()) return;
                int sub = e.isShiftClick() ? 16 : 1;
                amt = Math.max(1, amt - sub);
                plugin.buy().put(p.getUniqueId(), new GuiManager.BuyState(st.listingId(), amt));
                plugin.gui().updateBuyView(e.getView().getTopInventory(), l, amt);
                return;
            }

            if (slot == 26) { p.closeInventory(); return; }

            if (slot == 22) {
                var res = plugin.service().buy(p, st.listingId(), amt);
                p.sendMessage(U.c("&7[&f&lILLIT AH&7] " + (res.ok() ? "&a" : "&c") + res.msg()));
                plugin.service().reload();
                ViewContext ctx = new ViewContext();
                plugin.ctx().put(p.getUniqueId(), ctx);
                plugin.gui().openMain(p, ctx);
                return;
            }

            return;
        }

        // 2) MY lots menu (reclaim)
        if (isMy) {
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

        // 3) MAIN + PLAYER views
        {
            int slot = e.getRawSlot();
            ViewContext ctx = new ViewContext();
                plugin.ctx().put(p.getUniqueId(), ctx);

            if (slot == 45) { p.closeInventory(); return; }
            if (slot == 49) { // refresh
                plugin.service().reload();
                ctx.page = 0;
                ctx.search = null;
                plugin.gui().openMain(p, ctx);
                return;
            }
            if (slot == 50) { // my
                plugin.gui().openMy(p, ctx);
                return;
            }
            if (slot == 52) { if (it.getType().name().contains("ARROW")) { ctx.page = Math.max(0, ctx.page-1); plugin.gui().openMain(p, ctx); } return; }
            if (slot == 53) { if (it.getType().name().contains("ARROW")) { ctx.page = ctx.page+1; plugin.gui().openMain(p, ctx); } return; }

            if (slot == 46) {
                // cycle sort on left click
                if (!e.isLeftClick()) return;
                ctx.sort = switch (ctx.sort) {
                    case NEW_FIRST -> AuctionService.Sort.CHEAP_FIRST;
                    case CHEAP_FIRST -> AuctionService.Sort.EXPENSIVE_FIRST;
                    case EXPENSIVE_FIRST -> AuctionService.Sort.OLD_FIRST;
                    case OLD_FIRST -> AuctionService.Sort.NEW_FIRST;
                };
                plugin.gui().openMain(p, ctx);
                return;
            }

            String id = GuiManager.listingIdFromItem(it);
            if (id != null) {
                Listing l = plugin.service().getById(id);
                if (l != null) {
                    if (l.seller.equals(p.getUniqueId())) plugin.gui().openCannotBuy(p);
                    else plugin.gui().openBuyAmount(p, l);
                }
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
