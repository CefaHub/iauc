package ru.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public final class AuctionService {

    public enum Sort { CHEAP_FIRST, EXPENSIVE_FIRST, NEW_FIRST, OLD_FIRST }

    public record BuyResult(boolean ok, String msg) {}
    public record ReclaimResult(boolean ok, String msg) {}

    private final IllitAuctionPlugin plugin;
    private final AuctionStore store;
    private final Vault vault;
    private List<Listing> listings = new ArrayList<>();

    public AuctionService(IllitAuctionPlugin plugin, AuctionStore store, Vault vault) {
        this.plugin = plugin;
        this.store = store;
        this.vault = vault;
        reload();
    }

    public synchronized void reload() {
        listings = store.loadAll();
    }

    public synchronized void flush() {
        store.saveAll(listings);
    }

    public long now() { return System.currentTimeMillis(); }

    public long expiryMs() {
        return plugin.getConfig().getLong("expire_hours", 48L) * 60L * 60L * 1000L;
    }

    public synchronized String createListing(Player seller, ItemStack item, int amount, double unitPrice) {
        String id = UUID.randomUUID().toString().replace("-", "");
        long now = now();
        long exp = now + expiryMs();
        ItemStack one = item.clone();
        one.setAmount(1);
        String base64 = ItemCodec.toBase64(one);
        listings.add(new Listing(id, seller.getUniqueId(), seller.getName(), now, exp, base64, amount, unitPrice));
        flush();
        return id;
    }

    public synchronized List<Listing> listPublic(Sort sort, String search) {
        long now = now();
        var stream = listings.stream().filter(l -> !l.expired(now));
        if (search != null && !search.isBlank()) {
            String s = search.toLowerCase();
            stream = stream.filter(l -> {
                ItemStack it = ItemCodec.fromBase64(l.itemBase64);
                String mat = it.getType().name().toLowerCase();
                String name = (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) ? it.getItemMeta().getDisplayName().toLowerCase() : "";
                return mat.contains(s) || name.contains(s) || l.sellerName.toLowerCase().contains(s);
            });
        }
        List<Listing> out = stream.collect(Collectors.toList());
        Comparator<Listing> cmp = switch (sort) {
            case CHEAP_FIRST -> Comparator.comparingDouble(l -> l.unitPrice);
            case EXPENSIVE_FIRST -> Comparator.comparingDouble((Listing l) -> l.unitPrice).reversed();
            case NEW_FIRST -> Comparator.comparingLong((Listing l) -> l.createdAtMs).reversed();
            case OLD_FIRST -> Comparator.comparingLong(l -> l.createdAtMs);
        };
        out.sort(cmp);
        return out;
    }

    public synchronized List<Listing> listMine(UUID seller) {
        return listings.stream().filter(l -> l.seller.equals(seller)).collect(Collectors.toList());
    }

    public synchronized Listing getById(String id) {
        for (Listing l : listings) if (l.id.equals(id)) return l;
        return null;
    }

    public synchronized ReclaimResult reclaim(Player p, String id) {
        Listing l = getById(id);
        if (l == null) return new ReclaimResult(false, "Лот не найден.");
        if (!l.seller.equals(p.getUniqueId())) return new ReclaimResult(false, "Это не ваш лот.");

        // give remaining items back
        ItemStack base = ItemCodec.fromBase64(l.itemBase64);
        ItemStack give = base.clone(); give.setAmount(1);
        int left = l.amountRemaining;
        while (left > 0) {
            ItemStack stack = give.clone();
            int take = Math.min(64, left);
            stack.setAmount(take);
            var rem = p.getInventory().addItem(stack);
            if (!rem.isEmpty()) {
                return new ReclaimResult(false, "Инвентарь заполнен.");
            }
            left -= take;
        }
        listings.removeIf(x -> x.id.equals(id));
        flush();
        return new ReclaimResult(true, "Лот возвращён.");
    }

    public synchronized BuyResult buy(Player buyer, String id, int amount) {
        Listing l = getById(id);
        if (l == null) return new BuyResult(false, "Лот не найден.");
        if (l.seller.equals(buyer.getUniqueId())) return new BuyResult(false, "Нельзя купить свой лот.");
        if (l.expired(now())) return new BuyResult(false, "Лот истёк. Заберите его в /ah my.");
        if (amount <= 0) return new BuyResult(false, "Некорректное количество.");
        if (amount > l.amountRemaining) return new BuyResult(false, "Столько нет в лоте.");
        if (amount > 64) return new BuyResult(false, "Нельзя больше 64 за раз.");

        double total = l.unitPrice * amount;

        if (!vault.has(buyer, total)) return new BuyResult(false, "Не хватает денег.");
        if (!vault.withdraw(buyer, total)) return new BuyResult(false, "Списание не удалось.");

        OfflinePlayer seller = Bukkit.getOfflinePlayer(l.seller);
        vault.deposit(seller, total);

        // give items
        ItemStack base = ItemCodec.fromBase64(l.itemBase64);
        ItemStack give = base.clone();
        give.setAmount(amount);
        var rem = buyer.getInventory().addItem(give);
        if (!rem.isEmpty()) {
            // rollback money best-effort
            vault.deposit(buyer, total);
            vault.withdraw(seller, total);
            return new BuyResult(false, "Инвентарь заполнен.");
        }

        int newLeft = l.amountRemaining - amount;
        listings.removeIf(x -> x.id.equals(id));
        if (newLeft > 0) {
            listings.add(new Listing(l.id, l.seller, l.sellerName, l.createdAtMs, l.expiresAtMs, l.itemBase64, newLeft, l.unitPrice));
        }
        flush();
        return new BuyResult(true, "Покупка успешна.");
    }
}
