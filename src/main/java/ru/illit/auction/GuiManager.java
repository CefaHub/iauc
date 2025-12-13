package ru.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiManager {
    public static final String TITLE_MAIN = U.c("&f&lАукцион");
    public static final String TITLE_MY = U.c("&f&lАукцион &7- &fМои лоты");
    public static final String TITLE_BUY = U.c("&f&lАукцион &7- &fКупить");

    private final IllitAuctionPlugin plugin;
    private final AuctionService service;

    public GuiManager(IllitAuctionPlugin plugin, AuctionService service) {
        this.plugin = plugin;
        this.service = service;
    }

public void openMain(Player p, ViewContext ctx) {
    Inventory inv = Bukkit.createInventory(p, 54, TITLE_MAIN);

    List<Listing> listings = service.listPublic(ctx.sort, ctx.search);

    int perPage = 45;
    int maxPage = Math.max(0, (listings.size() - 1) / perPage);
    if (ctx.page < 0) ctx.page = 0;
    if (ctx.page > maxPage) ctx.page = maxPage;

    decorateMain(inv, ctx, maxPage);
    fillListings(inv, ctx, listings);

    p.openInventory(inv);
    plugin.ctx().put(p.getUniqueId(), ctx);
}

    public void openMy(Player p, ViewContext ctx) {
        Inventory inv = Bukkit.createInventory(p, 54, TITLE_MY);
        decorateMy(inv);
        fillMy(inv, service.listMine(p.getUniqueId()));
        p.openInventory(inv);
        plugin.ctx().put(p.getUniqueId(), ctx);
    }

    public void openPlayer(Player p, ViewContext ctx, OfflinePlayer seller) {
        Inventory inv = Bukkit.createInventory(p, 54, U.c("&f&lАукцион &7- &f" + (seller.getName()==null?"Игрок":seller.getName())));
        decoratePlayer(inv);
        var all = service.listPublic(ctx.sort, ctx.search);
        var only = all.stream().filter(l -> l.seller.equals(seller.getUniqueId())).toList();
        fillListings(inv, ctx, only);
        p.openInventory(inv);
        plugin.ctx().put(p.getUniqueId(), ctx);
    }

    public void openCannotBuy(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, TITLE_CANNOT_BUY);
    for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler());
    inv.setItem(13, button(Material.BARRIER, "&cНельзя купить свой лот", List.of(
            "&7Вы не можете покупать товары,",
            "&7которые выставили сами."
    )));
    inv.setItem(26, button(Material.BARRIER, "&fЗакрыть", List.of("&7Нажмите, чтобы закрыть меню.")));
    p.openInventory(inv);
}

public void openBuyAmount(Player p, Listing l) {
        openBuyAmount(p, l, 1);
    }

    public void openBuyAmount(Player p, Listing l, int amount) {
        int a = Math.max(1, Math.min(64, Math.min(amount, l.amountRemaining)));
        plugin.buy().put(p.getUniqueId(), new BuyState(l.id, a));
        updateBuyInventory(p, l, a);
    }

    public record BuyState(String listingId, int amount) {}

    private void decorateMain(Inventory inv, ViewContext ctx, int maxPage) {
        inv.setItem(45, button(Material.BARRIER, "&fЗакрыть", List.of("&7Нажмите, чтобы закрыть меню.")));
        inv.setItem(46, button(Material.PAPER, "&fСортировка", List.of(
                "&7Текущая: &f" + sortName(ctx.sort),
                "&7ЛКМ: переключить режим"
        )));
        inv.setItem(47, button(Material.BOOK, "&fИнформация", List.of(
        "&7Лоты действуют: &f48 часов&7.",
        "&7Истёкшие лоты забираются в &f/ah my&7.",
        "&7Покупка списывает &fайлитики&7."
)));
        inv.setItem(49, button(Material.SUNFLOWER, "&fОбновить", List.of("&7Нажмите, чтобы обновить.")));
        inv.setItem(50, button(Material.CHEST, "&fМои лоты", List.of("&7Открыть ваши выставленные лоты.")));
        inv.setItem(52, button(Material.ARROW, "&f< Пред.", List.of("&7Предыдущая страница")));
        inv.setItem(53, button(Material.ARROW, "&fСлед. >", List.of("&7Следующая страница")));
    }

    private void decorateMy(Inventory inv) {
        inv.setItem(49, button(Material.ARROW, "&fНазад", List.of("&7Вернуться в аукцион.")));
    }

    private void decoratePlayer(Inventory inv) {
        inv.setItem(49, button(Material.ARROW, "&fНазад", List.of("&7Вернуться в аукцион.")));
    }

    private void fillListings(Inventory inv, ViewContext ctx, List<Listing> listings) {
        int perPage = 45;
        int start = ctx.page * perPage;
        int end = Math.min(listings.size(), start + perPage);
        for (int i = 0; i < perPage; i++) inv.setItem(i, null);

        if (start >= listings.size()) return;

        for (int i = start; i < end; i++) {
            Listing l = listings.get(i);
            ItemStack base = ItemCodec.fromBase64(l.itemBase64).clone();
            int showAmount = Math.min(64, l.amountRemaining);
            base.setAmount(showAmount);
            List<String> lore = new ArrayList<>();
            lore.add(U.c("&7Продавец: &f" + l.sellerName));
            lore.add(U.c("&7Цена за 1: &f" + U.fmt((long) l.unitPrice) + " &7айлитики"));
            lore.add(U.c("&7В лоте: &f" + l.amountRemaining));
            lore.add(U.c("&7Истекает через: &f" + hoursLeft(l.expiresAtMs) + "ч"));
            lore.add("");
            lore.add(U.c("&7&lКупить"));
            lore.add(U.c("&7Откроется выбор количества."));
            inv.setItem(i-start, tag(withLore(base, lore), l.id));
        }
    }

    private void fillMy(Inventory inv, List<Listing> mine) {
        for (int i = 0; i < 54; i++) inv.setItem(i, null);
        int slot = 0;
        long now = System.currentTimeMillis();
        for (Listing l : mine) {
            if (slot >= 45) break;
            ItemStack base = ItemCodec.fromBase64(l.itemBase64).clone();
            base.setAmount(Math.min(64, l.amountRemaining));
            List<String> lore = new ArrayList<>();
            lore.add(U.c("&7Цена за 1: &f" + U.fmt((long) l.unitPrice) + " &7айлитики"));
            lore.add(U.c("&7Осталось: &f" + l.amountRemaining));
            lore.add(U.c("&7Статус: " + (l.expired(now) ? "&cИстёк" : "&aАктивен")));
            lore.add("");
            lore.add(U.c("&7ЛКМ: &fЗабрать лот"));
            inv.setItem(slot++, tag(withLore(base, lore), l.id));
        }
        inv.setItem(49, button(Material.ARROW, "&fНазад", List.of("&7Вернуться в аукцион.")));
    }

    private static String sortName(AuctionService.Sort s) {
        return switch (s) {
            case CHEAP_FIRST -> "Сначала дешёвые";
            case EXPENSIVE_FIRST -> "Сначала дорогие";
            case NEW_FIRST -> "Сначала новые";
            case OLD_FIRST -> "Сначала старые";
        };
    }

private void updateBuyInventory(Player p, Listing l, int amount) {
        Inventory inv = Bukkit.createInventory(p, 27, TITLE_BUY);
        // background
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler());
        inv.setItem(11, button(Material.LIME_STAINED_GLASS_PANE, "&fДобавить", List.of("&7ЛКМ: &f+1", "&7Shift+ЛКМ: &f+16")));
        inv.setItem(15, button(Material.RED_STAINED_GLASS_PANE, "&fУбавить", List.of("&7ПКМ: &f-1", "&7Shift+ПКМ: &f-16")));
        inv.setItem(22, button(Material.EMERALD, "&fКупить", List.of("&7Нажмите, чтобы купить выбранное количество.")));
        inv.setItem(26, button(Material.BARRIER, "&fЗакрыть", List.of("&7Нажмите, чтобы закрыть меню.")));
        updateBuyView(inv, l, amount);
        p.openInventory(inv);
    }

    public void updateBuyView(Inventory inv, Listing l, int amount) {
        int a = Math.max(1, Math.min(64, Math.min(amount, l.amountRemaining)));
        ItemStack item = ItemCodec.fromBase64(l.itemBase64);
        // визуально нельзя ставить >maxStackSize, но значение количества остаётся в lore
        int visual = Math.min(a, Math.max(1, item.getMaxStackSize()));
        item.setAmount(visual);
        var meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(U.c("&7Продавец: &f" + l.sellerName));
        lore.add(U.c("&7Цена за 1: &f" + U.fmt((long) l.unitPrice) + " &7айлитики"));
        lore.add(U.c("&7Осталось: &f" + l.amountRemaining));
        lore.add("");
        lore.add(U.c("&7Выбрано: &f" + a + " &7шт."));
        lore.add(U.c("&7Стоимость: &f" + U.fmt((long) (l.unitPrice * a)) + " &7айлитики"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(13, item);
    }

    private void _old_updateBuyInventory(Player p, Listing l, int amount) {
    Inventory inv = Bukkit.createInventory(p, 27, TITLE_BUY);
    // background
    for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler());
    inv.setItem(11, button(Material.LIME_STAINED_GLASS_PANE, "&fДобавить", List.of("&7ЛКМ: &f+1", "&7Shift+ЛКМ: &f+16")));
    inv.setItem(15, button(Material.RED_STAINED_GLASS_PANE, "&fУбавить", List.of("&7ПКМ: &f-1", "&7Shift+ПКМ: &f-16")));
    inv.setItem(22, button(Material.EMERALD, "&fКупить", List.of("&7Нажмите, чтобы купить выбранное количество.")));
    inv.setItem(26, button(Material.BARRIER, "&fЗакрыть", List.of("&7Нажмите, чтобы закрыть меню.")));

    ItemStack item = ItemCodec.fromBase64(l.itemBase64);
    item.setAmount(Math.min(amount, Math.max(1, item.getMaxStackSize())));
    var meta = item.getItemMeta();
    List<String> lore = new ArrayList<>();
    lore.add(U.c("&7Продавец: &f" + l.sellerName));
    lore.add(U.c("&7Цена за 1: &f" + U.fmt((long) l.unitPrice) + " &7айлитики"));
    lore.add(U.c("&7Осталось: &f" + l.amountRemaining));
    lore.add("");
    lore.add(U.c("&7Выбрано: &f" + amount + " &7шт."));
    lore.add(U.c("&7Стоимость: &f" + U.fmt((long) (l.unitPrice * amount)) + " &7айлитики"));
    meta.setLore(lore);
    item.setItemMeta(meta);
    inv.setItem(13, item);

    p.openInventory(inv);
}

private static ItemStack filler() {
    ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta meta = it.getItemMeta();
    if (meta != null) {
        meta.setDisplayName(U.c("&7"));
        it.setItemMeta(meta);
    }
    return it;
}

private static ItemStack button(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(U.c(name));
        if (lore != null) meta.setLore(lore.stream().map(U::c).toList());
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack withLore(ItemStack it, List<String> lore) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) meta.setLore(lore.stream().map(U::c).toList());
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack tag(ItemStack it, String id) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(Keys.LISTING_ID, org.bukkit.persistence.PersistentDataType.STRING, id);
            it.setItemMeta(meta);
        }
        return it;
    }

    public static String listingIdFromItem(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(Keys.LISTING_ID, org.bukkit.persistence.PersistentDataType.STRING);
    }

    private static long hoursLeft(long expiresAtMs) {
        long ms = Math.max(0, expiresAtMs - System.currentTimeMillis());
        return ms / (60L*60L*1000L);
    }
}