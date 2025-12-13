package com.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiManager {

    private final IllitAuctionPlugin plugin;
    private final AuctionService service;
    private final Economy economy;

    public GuiManager(IllitAuctionPlugin plugin, AuctionService service, Economy economy) {
        this.plugin = plugin;
        this.service = service;
        this.economy = economy;
    }

    public void openAuction(Player p, GuiSession s) {
        s.type = GuiType.AUCTION;
        s.filterSeller = null;
        s.filterSellerName = "";
        Inventory inv = Bukkit.createInventory(p, 54, CC.t("&f&lАукцион"));
        renderAuction(inv, p, s, null);
        p.openInventory(inv);
    }

    public void openPlayerAuction(Player p, UUID sellerUuid, String sellerName, GuiSession s) {
        s.type = GuiType.PLAYER_AUCTION;
        s.filterSeller = sellerUuid;
        s.filterSellerName = sellerName == null ? "" : sellerName;
        Inventory inv = Bukkit.createInventory(p, 54, CC.t("&f&lАукцион"));
        renderAuction(inv, p, s, sellerUuid);
        p.openInventory(inv);
    }

    public void openSearch(Player p, String query, GuiSession s) {
        s.type = GuiType.SEARCH;
        s.search = query == null ? "" : query;
        s.filterSeller = null;
        s.filterSellerName = "";
        Inventory inv = Bukkit.createInventory(p, 54, CC.t("&f&lАукцион"));
        renderAuction(inv, p, s, null);
        p.openInventory(inv);
    }

    public void openMyListings(Player p, GuiSession s) {
        s.type = GuiType.MY_LISTINGS;
        s.page = 0;
        Inventory inv = Bukkit.createInventory(p, 54, CC.t("&f&lМои лоты"));
        renderMy(inv, p, s);
        p.openInventory(inv);
    }

    public void openBuyQuantity(Player p, Listing l, GuiSession s) {
        s.type = GuiType.BUY_QUANTITY;
        s.selectedListing = l.id();
        s.selectedQty = 1;

        Inventory inv = Bukkit.createInventory(p, 54, CC.t("&f&lПокупка"));
        renderBuy(inv, p, s, l);
        p.openInventory(inv);
    }

    public void refresh(Player p, Inventory inv, GuiSession s) {
        if (s.type == GuiType.MY_LISTINGS) renderMy(inv, p, s);
        else if (s.type == GuiType.BUY_QUANTITY) {
            Listing l = service.get(s.selectedListing);
            if (l != null) renderBuy(inv, p, s, l);
        } else {
            renderAuction(inv, p, s, s.filterSeller);
        }
    }

    private void renderAuction(Inventory inv, Player viewer, GuiSession s, UUID sellerFilter) {
        inv.clear();

        List<Listing> list = service.queryListings(s.filterSellerName, sellerFilter, s.search, s.sort);

        int perPage = 45;
        int page = Math.max(0, s.page);
        int start = page * perPage;
        int end = Math.min(list.size(), start + perPage);

        for (int i = start; i < end; i++) {
            Listing l = list.get(i);
            ItemStack it = l.buildDisplayItem();
            if (it == null) continue;

            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add(CC.t("&7"));
                lore.add(CC.t("&7Продавец: &f" + safe(l.sellerName())));
                lore.add(CC.t("&7Цена за 1: &f" + l.pricePerItem()));
                lore.add(CC.t("&7Кол-во: &f" + l.remainingAmount() + "&7/&f" + l.totalAmount()));
                lore.add(CC.t("&7Выставлено: &f" + TimeFmt.fmt(l.createdAtMs())));
                lore.add(CC.t("&7Истечёт: &f" + TimeFmt.fmt(l.expireAtMs())));
                lore.add(CC.t("&7"));
                lore.add(CC.t("&7&lКупить: &fЛКМ"));
                lore.add(CC.t("&7Лот пропадёт через 48 часов."));
                lore.add(CC.t("&7Для возврата зайди в меню &fМои лоты&7."));
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                it.setItemMeta(meta);
            }

            int slot = i - start;
            inv.setItem(slot, Tag.tagListing(it, l.id()));
        }

        inv.setItem(45, button(Material.ARROW, "&fПредыдущая", List.of("&7Страница: &f" + (page + 1))));
        inv.setItem(53, button(Material.ARROW, "&fСледующая", List.of("&7Страница: &f" + (page + 1))));

        inv.setItem(46, button(Material.GOLD_INGOT, "&fСортировка: Цена", List.of("&7Нажми чтобы сортировать по дешевизне")));
        inv.setItem(47, button(Material.CLOCK, "&fСортировка: Дата", List.of("&7Нажми чтобы переключить Новое/Старое", "&7Текущая: &f" + sortName(s.sort))));

        inv.setItem(49, button(Material.LIME_DYE, "&fОбновить", List.of("&7Нажми чтобы обновить аукцион")));
        inv.setItem(51, button(Material.CHEST, "&fМои лоты", List.of("&7Выставленные/Просроченные лоты", "&7Забрать можно там")));

        long bal = economy.getBalance(viewer.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add(CC.t("&7Лотов на странице: &f" + Math.max(0, end - start)));
        lore.add(CC.t("&7Всего лотов: &f" + list.size()));
        lore.add(CC.t("&7Баланс: &f" + bal));
        if (s.type == GuiType.SEARCH && s.search != null && !s.search.isBlank()) lore.add(CC.t("&7Поиск: &f" + s.search));
        if (s.type == GuiType.PLAYER_AUCTION && s.filterSellerName != null && !s.filterSellerName.isBlank()) lore.add(CC.t("&7Аукцион игрока: &f" + s.filterSellerName));
        inv.setItem(48, button(Material.PAPER, "&fИнформация", lore));
    }

    private void renderMy(Inventory inv, Player p, GuiSession s) {
        inv.clear();

        List<Listing> active = service.queryListings(p.getName(), p.getUniqueId(), "", AuctionService.SortMode.DATE_NEW);
        List<String> returns = service.getReturns(p.getUniqueId());

        List<ItemStack> items = new ArrayList<>();
        for (Listing l : active) {
            ItemStack it = l.buildDisplayItem();
            if (it == null) continue;
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add(CC.t("&7"));
                lore.add(CC.t("&7Статус: &fАктивен"));
                lore.add(CC.t("&7Осталось: &f" + l.remainingAmount()));
                lore.add(CC.t("&7Цена за 1: &f" + l.pricePerItem()));
                lore.add(CC.t("&7Истечёт: &f" + TimeFmt.fmt(l.expireAtMs())));
                lore.add(CC.t("&7"));
                lore.add(CC.t("&7&lЗабрать: &fЛКМ"));
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                it.setItemMeta(meta);
            }
            items.add(Tag.tagListing(it, l.id()));
        }

        for (int i = 0; i < returns.size(); i++) {
            String entry = returns.get(i);
            String[] parts = entry.split("\\|");
            if (parts.length != 2) continue;
            String base64 = parts[0];
            int amt;
            try { amt = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { continue; }

            try {
                ItemStack it = ItemSer.fromBase64(base64).clone();
                it.setAmount(Math.max(1, Math.min(64, amt)));
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    lore.add(CC.t("&7"));
                    lore.add(CC.t("&7Статус: &fПросрочен (48ч)"));
                    lore.add(CC.t("&7Количество: &f" + amt));
                    lore.add(CC.t("&7"));
                    lore.add(CC.t("&7&lЗабрать: &fЛКМ"));
                    meta.setLore(lore);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    it.setItemMeta(meta);
                }
                items.add(Tag.tagReturn(it, i));
            } catch (Exception ignored) {}
        }

        int perPage = 45;
        int page = Math.max(0, s.page);
        int start = page * perPage;
        int end = Math.min(items.size(), start + perPage);

        for (int i = start; i < end; i++) inv.setItem(i - start, items.get(i));

        inv.setItem(45, button(Material.ARROW, "&fПредыдущая", List.of("&7Страница: &f" + (page + 1))));
        inv.setItem(53, button(Material.ARROW, "&fСледующая", List.of("&7Страница: &f" + (page + 1))));
        inv.setItem(49, button(Material.LIME_DYE, "&fОбновить", List.of("&7Нажми чтобы обновить меню")));
        inv.setItem(48, button(Material.PAPER, "&fИнформация", List.of(
                "&7Активных лотов: &f" + active.size(),
                "&7Просроченных/возвратов: &f" + returns.size()
        )));
        inv.setItem(50, button(Material.BARRIER, "&fНазад", List.of("&7Вернуться в аукцион")));
    }

    private void renderBuy(Inventory inv, Player p, GuiSession s, Listing l) {
        inv.clear();

        ItemStack display = l.buildDisplayItem();
        if (display == null) display = new ItemStack(Material.BARRIER);

        int max = Math.min(64, l.remainingAmount());
        int qty = Math.max(1, Math.min(max, s.selectedQty));
        s.selectedQty = qty;

        ItemStack center = display.clone();
        center.setAmount(qty);

        ItemMeta meta = center.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(CC.t("&7"));
            lore.add(CC.t("&7Продавец: &f" + safe(l.sellerName())));
            lore.add(CC.t("&7Цена за 1: &f" + l.pricePerItem()));
            lore.add(CC.t("&7Выбрано: &f" + qty + "&7/&f" + max));
            lore.add(CC.t("&7Итого: &f" + (l.pricePerItem() * qty)));
            lore.add(CC.t("&7"));
            lore.add(CC.t("&7ЛКМ: &f+1"));
            lore.add(CC.t("&7ЛКМ+Shift: &f+16"));
            lore.add(CC.t("&7ПКМ: &f-1"));
            lore.add(CC.t("&7ПКМ+Shift: &f-16"));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            center.setItemMeta(meta);
        }

        inv.setItem(22, Tag.tagListing(center, l.id()));

        inv.setItem(31, button(Material.EMERALD, "&fКупить", List.of("&7Нажми чтобы купить выбранное количество")));
        inv.setItem(29, button(Material.BARRIER, "&fОтмена", List.of("&7Вернуться назад")));
        long bal = economy.getBalance(p.getUniqueId());
        inv.setItem(13, button(Material.PAPER, "&fИнформация", List.of("&7Баланс: &f" + bal, "&7Итого: &f" + (l.pricePerItem() * qty))));
    }

    private ItemStack button(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.t(name));
            List<String> l = new ArrayList<>();
            if (lore != null) for (String s : lore) l.add(CC.t(s));
            meta.setLore(l);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String sortName(AuctionService.SortMode s) {
        return switch (s) {
            case DATE_NEW -> "Новое";
            case DATE_OLD -> "Старое";
            case PRICE_ASC -> "Цена (дешевле)";
        };
    }

    private String safe(String s) {
        return s == null ? "unknown" : s;
    }
}
