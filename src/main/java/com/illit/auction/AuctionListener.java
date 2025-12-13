package com.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionListener implements Listener {

    private final IllitAuctionPlugin plugin;
    private final AuctionService service;
    private final GuiManager gui;
    private final Map<UUID, ViewContext> ctxs;

    private final Map<UUID, BuySession> buySessions = new ConcurrentHashMap<>();

    public AuctionListener(IllitAuctionPlugin plugin, AuctionService service, GuiManager gui, Map<UUID, ViewContext> ctxs) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
        this.ctxs = ctxs;
    }

    private String prefix() { return plugin.prefix(); }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getInventory() == null) return;
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;

        e.setCancelled(true);

        ViewContext ctx = ctxs.computeIfAbsent(p.getUniqueId(), ViewContext::new);

        switch (h.kind) {
            case "AUCTION" -> handleAuctionClick(p, ctx, e);
            case "MY" -> handleMyClick(p, ctx, e);
            case "BUY" -> handleBuyClick(p, h, e);
        }
    }

    private void handleAuctionClick(Player p, ViewContext ctx, InventoryClickEvent e) {
        int slot = e.getRawSlot();
        if (slot < 0) return;

        if (slot == plugin.getConfig().getInt("gui.buttons.prev-slot", 45)) {
            if (ctx.page > 0) ctx.page--;
            guiRefresh(p, ctx);
            return;
        }
        if (slot == plugin.getConfig().getInt("gui.buttons.next-slot", 53)) {
            ctx.page++;
            guiRefresh(p, ctx);
            return;
        }
        if (slot == plugin.getConfig().getInt("gui.buttons.refresh-slot", 49)) {
            guiRefresh(p, ctx);
            return;
        }
        if (slot == plugin.getConfig().getInt("gui.buttons.sort-slot", 50)) {
            ctx.sort = ctx.sort.next();
            ctx.page = 0;
            guiRefresh(p, ctx);
            return;
        }
        if (slot == plugin.getConfig().getInt("gui.buttons.my-slot", 48)) {
            ctx.page = 0;
            gui.openMy(p, ctx);
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        String id = gui.listingIdFromItem(clicked);
        if (id == null) return;

        Listing l = plugin.store().get(id);
        if (l == null) { p.sendMessage(prefix() + "§cЛот уже недоступен."); guiRefresh(p, ctx); return; }

        if (service.isExpired(l)) {
            p.sendMessage(prefix() + "§cЛот просрочен. Продавец может забрать его в меню своих лотов.");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            guiRefresh(p, ctx);
            return;
        }

        buySessions.put(p.getUniqueId(), new BuySession(id, 1));
        gui.openBuyAmount(p, l);
    }

    private void handleMyClick(Player p, ViewContext ctx, InventoryClickEvent e) {
        int slot = e.getRawSlot();
        if (slot < 0) return;

        if (slot == plugin.getConfig().getInt("gui.buttons.prev-slot", 45)) {
            if (ctx.page > 0) ctx.page--;
            gui.openMy(p, ctx);
            return;
        }
        if (slot == plugin.getConfig().getInt("gui.buttons.next-slot", 53)) {
            ctx.page++;
            gui.openMy(p, ctx);
            return;
        }
        if (slot == plugin.getConfig().getInt("gui.buttons.refresh-slot", 49)) {
            gui.openMy(p, ctx);
            return;
        }
        if (slot == plugin.getConfig().getInt("gui.buttons.sort-slot", 50)) {
            ctx.sort = ctx.sort.next();
            ctx.page = 0;
            gui.openMy(p, ctx);
            return;
        }
        if (slot == plugin.getConfig().getInt("gui.buttons.my-slot", 48)) {
            ctx.page = 0;
            gui.openMain(p, ctx);
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        String id = gui.listingIdFromItem(clicked);
        if (id == null) return;

        AuctionService.ReclaimResult r = service.reclaim(p, id);
        if (!r.ok()) {
            p.sendMessage(prefix() + "§c" + r.message());
            return;
        }
        p.sendMessage(prefix() + "§aЛот возвращен. Количество: §f" + r.amount());
        plugin.markDirty();
        gui.openMy(p, ctx);
    }

    private void handleBuyClick(Player p, Holder h, InventoryClickEvent e) {
        BuySession s = buySessions.get(p.getUniqueId());
        if (s == null || !s.listingId.equals(h.extra)) {
            p.closeInventory();
            return;
        }

        Listing l = plugin.store().get(s.listingId);
        if (l == null || l.amountRemaining <= 0) {
            p.sendMessage(prefix() + "§cЛот уже недоступен.");
            p.closeInventory();
            return;
        }

        int slot = e.getRawSlot();
        boolean shift = e.isShiftClick();
        ClickType ct = e.getClick();

        int delta = 0;
        if (slot == 11) { // + button: LMB or shift
            delta = shift ? 16 : 1;
        }
        if (slot == 15) { // - button: RMB per requirement, but allow any click
            delta = shift ? -16 : -1;
        }

        if (delta != 0) {
            int max = Math.min(64, l.amountRemaining);
            int next = clamp(s.qty + delta, 1, max);
            s.qty = next;
            gui.updateBuyInfo(e.getInventory(), s.qty, l.unitPrice);
            return;
        }

        if (slot == 22) {
            int qty = Math.min(s.qty, Math.min(64, l.amountRemaining));
            AuctionService.BuyResult r = service.buy(p, s.listingId, qty);
            if (!r.ok()) {
                p.sendMessage(prefix() + "§c" + r.message());
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
                return;
            }
            p.sendMessage(prefix() + "§aПокупка успешна: §f" + r.amount() + "§a шт. за §f" + plugin.economy().format(r.total()));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            plugin.markDirty();
            p.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getInventory() == null) return;
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;
        if (h.kind.equals("BUY")) buySessions.remove(p.getUniqueId());
    }

    private void guiRefresh(Player p, ViewContext ctx) {
        ctx.page = Math.max(0, ctx.page);
        switch (ctx.type) {
            case MAIN -> gui.openMain(p, ctx);
            case MY -> gui.openMy(p, ctx);
            case PLAYER -> {
                if (ctx.filterSeller != null) gui.openPlayer(p, ctx, Bukkit.getOfflinePlayer(ctx.filterSeller));
                else gui.openMain(p, ctx);
            }
            case SEARCH -> gui.openSearch(p, ctx, ctx.searchQuery != null ? ctx.searchQuery : "");
        }
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private static class BuySession {
        final String listingId;
        int qty;
        BuySession(String id, int qty) { this.listingId = id; this.qty = qty; }
    }
}
