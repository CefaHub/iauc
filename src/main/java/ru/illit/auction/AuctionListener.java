package ru.illit.auction;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class AuctionListener implements Listener {
    private final IllitAuctionPlugin plugin;
    private final AuctionService service;
    private final GuiManager gui;

    public AuctionListener(IllitAuctionPlugin plugin, AuctionService service, GuiManager gui) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = e.getView().getTitle();
        boolean isMain = title.equals(GuiManager.TITLE_MAIN);
        boolean isMy = title.equals(GuiManager.TITLE_MY);
        boolean isBuy = title.equals(GuiManager.TITLE_BUY);
        boolean isCannotBuy = title.equals(GuiManager.TITLE_CANNOT_BUY);
        boolean isPlayerView = title.startsWith(U.c("&f&lАукцион &7- &f")) && !isBuy && !isMy && !isCannotBuy && !isMain;

        if (!isMain && !isMy && !isBuy && !isPlayerView && !isCannotBuy) return;

        e.setCancelled(true);

        ItemStack it = e.getCurrentItem();
        if (it == null || it.getType() == Material.AIR) return;

        // Закрытие окна ошибки
        if (isCannotBuy) {
            if (e.getRawSlot() == 26) p.closeInventory();
            return;
        }

        // 1) BUY amount GUI
        if (isBuy) {
            var state = plugin.buy().get(p.getUniqueId());
            if (state == null) {
                p.closeInventory();
                return;
            }

            Listing l = service.get(state.listingId());
            if (l == null) {
                p.sendMessage(U.c("&cЛот не найден."));
                p.closeInventory();
                return;
            }

            int amount = state.amount();
            int slot = e.getRawSlot();
            ClickType ct = e.getClick();

            if (slot == 26) {
                p.closeInventory();
                return;
            }

            if (slot == 11) { // +
                amount += (ct.isShiftClick() ? 16 : 1);
                gui.openBuyAmount(p, l, amount);
                return;
            }

            if (slot == 15) { // -
                amount -= (ct.isShiftClick() ? 16 : 1);
                gui.openBuyAmount(p, l, amount);
                return;
            }

            if (slot == 22) { // buy
                if (l.seller.equals(p.getUniqueId())) {
                    gui.openCannotBuy(p);
                    return;
                }

                var res = service.buy(p, l.id, amount);
                if (!res.ok()) {
                    p.sendMessage(U.c("&c" + res.message()));
                    return;
                }

                p.sendMessage(U.c("&aПокупка успешна."));
                ViewContext ctx = new ViewContext(); // после покупки открываем чистый /ah (стр. 1)
                plugin.ctx().put(p.getUniqueId(), ctx);
                gui.openMain(p, ctx);
                return;
            }

            return;
        }

        // 2) MY lots menu (reclaim) + paging
        if (isMy) {
            int slot = e.getRawSlot();
            ViewContext ctx = plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext());

            if (slot == 49) {
                ViewContext main = new ViewContext();
                plugin.ctx().put(p.getUniqueId(), main);
                gui.openMain(p, main);
                return;
            }

            if (slot == 52) {
                if (ctx.myPage > 0) ctx.myPage--;
                gui.openMy(p, ctx);
                return;
            }

            if (slot == 53) {
                int perPage = 45;
                int maxPage = Math.max(0, (service.listMine(p.getUniqueId()).size() - 1) / perPage);
                if (ctx.myPage < maxPage) ctx.myPage++;
                gui.openMy(p, ctx);
                return;
            }

            String id = GuiManager.listingIdFromItem(it);
            if (id != null) {
                var res = service.reclaim(p, id);
                if (!res.ok()) {
                    p.sendMessage(U.c("&c" + res.message()));
                    return;
                }
                p.sendMessage(U.c("&aЛот возвращён."));
                gui.openMy(p, ctx);
            }
            return;
        }

        // 3) MAIN / PLAYER VIEW GUI
        if (isMain || isPlayerView) {
            ViewContext ctx = plugin.ctx().getOrDefault(p.getUniqueId(), new ViewContext());
            plugin.ctx().put(p.getUniqueId(), ctx);

            int slot = e.getRawSlot();

            if (slot == 45) {
                p.closeInventory();
                return;
            }

            if (!isPlayerView) {
                if (slot == 46) { // sort toggle
                    ctx.sort = switch (ctx.sort) {
                        case CHEAP_FIRST -> AuctionService.Sort.EXPENSIVE_FIRST;
                        case EXPENSIVE_FIRST -> AuctionService.Sort.NEW_FIRST;
                        case NEW_FIRST -> AuctionService.Sort.OLD_FIRST;
                        case OLD_FIRST -> AuctionService.Sort.CHEAP_FIRST;
                    };
                    ctx.page = 0;
                    gui.openMain(p, ctx);
                    return;
                }

                if (slot == 49) { // refresh
                    ctx.page = 0;
                    ctx.search = null;
                    gui.openMain(p, ctx);
                    return;
                }

                if (slot == 50) { // my lots
                    ctx.myPage = 0;
                    gui.openMy(p, ctx);
                    return;
                }

                if (slot == 52) { // prev
                    if (ctx.page > 0) ctx.page--;
                    gui.openMain(p, ctx);
                    return;
                }

                if (slot == 53) { // next
                    int perPage = 45;
                    int maxPage = Math.max(0, (service.listPublic(ctx.sort, ctx.search).size() - 1) / perPage);
                    if (ctx.page < maxPage) ctx.page++;
                    gui.openMain(p, ctx);
                    return;
                }
            }

            // Listing click
            String id = GuiManager.listingIdFromItem(it);
            if (id == null) return;

            Listing l = service.get(id);
            if (l == null) {
                p.sendMessage(U.c("&cЛот не найден."));
                return;
            }

            if (l.seller.equals(p.getUniqueId())) {
                gui.openCannotBuy(p);
                return;
            }

            gui.openBuyAmount(p, l);
        }
    }
}
