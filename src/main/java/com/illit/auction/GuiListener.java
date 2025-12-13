package com.illit.auction;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GuiListener implements Listener {

    private final IllitAuctionPlugin plugin;
    private final AuctionService service;
    private final GuiManager gui;
    private final SessionStore sessions;

    public GuiListener(IllitAuctionPlugin plugin, AuctionService service, GuiManager gui, SessionStore sessions) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
        this.sessions = sessions;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory inv = e.getInventory();
        if (inv == null) return;

        String title = e.getView().getTitle();
        if (title == null) return;

        boolean isAuction = title.equals(CC.t("&f&lАукцион"));
        boolean isMy = title.equals(CC.t("&f&lМои лоты"));
        boolean isBuy = title.equals(CC.t("&f&lПокупка"));

        if (!isAuction && !isMy && !isBuy) return;

        e.setCancelled(true);

        GuiSession s = sessions.get(p);
        ItemStack clicked = e.getCurrentItem();
        int slot = e.getRawSlot();

        if (clicked == null || clicked.getType().isAir()) return;

        if (slot == 45) {
            if (s.page > 0) s.page--;
            gui.refresh(p, inv, s);
            return;
        }
        if (slot == 53) {
            s.page++;
            gui.refresh(p, inv, s);
            return;
        }
        if (slot == 49 && clicked.getType() == Material.LIME_DYE) {
            gui.refresh(p, inv, s);
            return;
        }

        if (isAuction) {
            if (slot == 51 && clicked.getType() == Material.CHEST) {
                s.page = 0;
                gui.openMyListings(p, s);
                return;
            }
            if (slot == 47 && clicked.getType() == Material.CLOCK) {
                if (s.sort == AuctionService.SortMode.DATE_NEW) s.sort = AuctionService.SortMode.DATE_OLD;
                else s.sort = AuctionService.SortMode.DATE_NEW;
                s.page = 0;
                gui.refresh(p, inv, s);
                return;
            }
            if (slot == 46 && clicked.getType() == Material.GOLD_INGOT) {
                s.sort = AuctionService.SortMode.PRICE_ASC;
                s.page = 0;
                gui.refresh(p, inv, s);
                return;
            }

            UUID listingId = Tag.getListing(clicked);
            if (listingId != null) {
                Listing l = service.get(listingId);
                if (l == null) { gui.refresh(p, inv, s); return; }

                if (l.sellerUuid().equals(p.getUniqueId())) {
                    p.sendMessage(plugin.prefix() + "§cНельзя купить свой лот. Забрать можно в меню 'Мои лоты'.");
                    return;
                }

                if (l.remainingAmount() <= 1) {
                    boolean ok = service.buy(l.id(), p, 1);
                    p.sendMessage(plugin.prefix() + (ok ? "§aПокупка успешна." : "§cНе удалось купить (не хватает денег/лот недоступен)."));
                    gui.refresh(p, inv, s);
                    return;
                }

                gui.openBuyQuantity(p, l, s);
            }
            return;
        }

        if (isMy) {
            if (slot == 50 && clicked.getType() == Material.BARRIER) {
                s.page = 0;
                s.search = "";
                s.filterSeller = null;
                s.filterSellerName = "";
                s.sort = AuctionService.SortMode.DATE_NEW;
                gui.openAuction(p, s);
                return;
            }

            UUID listingId = Tag.getListing(clicked);
            if (listingId != null) {
                boolean ok = service.withdrawListing(listingId, p);
                p.sendMessage(plugin.prefix() + (ok ? "§aЛот снят и отправлен в возвраты." : "§cНе удалось снять лот."));
                gui.refresh(p, inv, s);
                return;
            }

            Integer idx = Tag.getReturnIdx(clicked);
            if (idx != null) {
                boolean ok = service.claimReturn(p.getUniqueId(), idx);
                p.sendMessage(plugin.prefix() + (ok ? "§aЛот получен." : "§cНе удалось забрать (ошибка/нет места)."));
                gui.refresh(p, inv, s);
                return;
            }
            return;
        }

        if (isBuy) {
            if (slot == 29 && clicked.getType() == Material.BARRIER) {
                gui.openAuction(p, s);
                return;
            }
            if (slot == 31 && clicked.getType() == Material.EMERALD) {
                UUID id = s.selectedListing;
                Listing l = service.get(id);
                if (l == null) {
                    p.sendMessage(plugin.prefix() + "§cЛот недоступен.");
                    gui.openAuction(p, s);
                    return;
                }
                int qty = s.selectedQty;
                boolean ok = service.buy(id, p, qty);
                p.sendMessage(plugin.prefix() + (ok ? "§aПокупка успешна." : "§cНе удалось купить (не хватает денег/лот недоступен)."));
                gui.openAuction(p, s);
                return;
            }

            UUID listingId = Tag.getListing(clicked);
            if (listingId != null) {
                Listing l = service.get(listingId);
                if (l == null) { gui.openAuction(p, s); return; }
                int max = Math.min(64, l.remainingAmount());
                int delta = 0;
                ClickType ct = e.getClick();
                if (ct == ClickType.LEFT) delta = 1;
                if (ct == ClickType.SHIFT_LEFT) delta = 16;
                if (ct == ClickType.RIGHT) delta = -1;
                if (ct == ClickType.SHIFT_RIGHT) delta = -16;
                if (delta != 0) {
                    s.selectedQty = Math.max(1, Math.min(max, s.selectedQty + delta));
                    gui.refresh(p, inv, s);
                }
            }
        }
    }
}
