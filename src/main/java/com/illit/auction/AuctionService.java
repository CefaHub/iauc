package com.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class AuctionService {

    public enum SortMode { PRICE_ASC, DATE_NEW, DATE_OLD }

    private final AuctionStorage storage;
    private final Economy economy;

    private final long expireMs;
    private final int maxListingsPerPlayer;

    private final Map<UUID, Listing> listings = new HashMap<>();

    public AuctionService(IllitAuctionPlugin plugin, AuctionStorage storage, Economy economy) {
        this.storage = storage;
        this.economy = economy;
        this.expireMs = plugin.getConfig().getLong("listing-expire-ms", 172800000L);
        this.maxListingsPerPlayer = plugin.getConfig().getInt("max-listings-per-player", 0);

        listings.putAll(storage.loadListings());
        expireSweep();
    }

    public void reload() {
        listings.clear();
        listings.putAll(storage.loadListings());
        expireSweep();
    }

    public void saveAll() {
        for (UUID id : new ArrayList<>(listings.keySet())) {
            Listing l = listings.get(id);
            if (l == null) continue;
            storage.writeListing(l);
        }
        storage.saveListings();
        storage.saveReturns();
    }

    public void expireSweep() {
        long now = System.currentTimeMillis();
        List<Listing> expired = listings.values().stream().filter(l -> l.isExpired(now)).collect(Collectors.toList());
        if (expired.isEmpty()) return;

        for (Listing l : expired) {
            if (l.remainingAmount() > 0) {
                storage.addReturn(l.sellerUuid(), l.itemBase64(), l.remainingAmount());
            }
            listings.remove(l.id());
            storage.removeListing(l.id());
        }
        storage.saveListings();
        storage.saveReturns();
    }

    public int countActiveBySeller(UUID seller) {
        int c = 0;
        for (Listing l : listings.values()) if (l.sellerUuid().equals(seller)) c++;
        return c;
    }

    public Listing createListing(Player seller, ItemStack item, int amount, long pricePerItem) throws Exception {
        if (amount <= 0) throw new IllegalArgumentException("amount");
        if (pricePerItem < 0) throw new IllegalArgumentException("price");

        expireSweep();

        if (maxListingsPerPlayer > 0 && countActiveBySeller(seller.getUniqueId()) >= maxListingsPerPlayer) {
            throw new IllegalStateException("limit");
        }

        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis();
        long expireAt = now + expireMs;

        ItemStack stack = item.clone();
        stack.setAmount(1);
        String base64 = ItemSer.toBase64(stack);

        Listing l = new Listing(id, seller.getUniqueId(), seller.getName(), now, expireAt, base64, amount, amount, pricePerItem);
        listings.put(id, l);

        storage.writeListing(l);
        storage.saveListings();
        return l;
    }

    public List<Listing> queryListings(String filterSellerName, UUID filterSellerUuid, String search, SortMode sort) {
        expireSweep();

        String s = (search == null) ? "" : search.trim().toLowerCase();
        String sellerName = (filterSellerName == null) ? "" : filterSellerName.trim().toLowerCase();

        List<Listing> out = new ArrayList<>();
        for (Listing l : listings.values()) {
            if (filterSellerUuid != null && !l.sellerUuid().equals(filterSellerUuid)) continue;
            if (!sellerName.isEmpty() && (l.sellerName() == null || !l.sellerName().toLowerCase().contains(sellerName))) continue;

            if (!s.isEmpty()) {
                ItemStack it = l.buildDisplayItem();
                String mat = (it == null) ? "" : it.getType().name().toLowerCase();
                String dn = (it != null && it.hasItemMeta() && it.getItemMeta().hasDisplayName()) ? it.getItemMeta().getDisplayName().toLowerCase() : "";
                if (!mat.contains(s) && !dn.contains(s)) continue;
            }
            out.add(l);
        }

        Comparator<Listing> cmp;
        switch (sort) {
            case PRICE_ASC -> cmp = Comparator.comparingLong(Listing::pricePerItem).thenComparingLong(Listing::createdAtMs);
            case DATE_OLD -> cmp = Comparator.comparingLong(Listing::createdAtMs);
            case DATE_NEW -> cmp = Comparator.comparingLong(Listing::createdAtMs).reversed();
            default -> cmp = Comparator.comparingLong(Listing::createdAtMs).reversed();
        }
        out.sort(cmp);
        return out;
    }

    public Listing get(UUID id) {
        return listings.get(id);
    }

    public boolean withdrawListing(UUID id, Player actor) {
        Listing l = listings.get(id);
        if (l == null) return false;
        if (!l.sellerUuid().equals(actor.getUniqueId()) && !actor.hasPermission("illit.ah.admin")) return false;

        if (l.remainingAmount() > 0) storage.addReturn(l.sellerUuid(), l.itemBase64(), l.remainingAmount());
        listings.remove(id);
        storage.removeListing(id);
        storage.saveListings();
        storage.saveReturns();
        return true;
    }

    public boolean buy(UUID listingId, Player buyer, int qty) {
        expireSweep();

        Listing l = listings.get(listingId);
        if (l == null) return false;
        if (qty <= 0) return false;
        if (qty > l.remainingAmount()) return false;
        if (qty > 64) return false;

        long cost = l.pricePerItem() * qty;
        if (!economy.withdraw(buyer.getUniqueId(), cost)) return false;

        economy.deposit(l.sellerUuid(), cost);

        try {
            ItemStack base = ItemSer.fromBase64(l.itemBase64()).clone();
            base.setAmount(qty);
            var left = buyer.getInventory().addItem(base);
            if (!left.isEmpty()) {
                for (ItemStack it : left.values()) buyer.getWorld().dropItemNaturally(buyer.getLocation(), it);
            }
        } catch (Exception e) {
            economy.deposit(buyer.getUniqueId(), cost);
            economy.withdraw(l.sellerUuid(), cost);
            return false;
        }

        l.remainingAmount(l.remainingAmount() - qty);
        if (l.remainingAmount() <= 0) {
            listings.remove(l.id());
            storage.removeListing(l.id());
        } else {
            storage.writeListing(l);
        }
        storage.saveListings();
        return true;
    }

    public List<String> getReturns(UUID player) {
        return storage.getReturns(player);
    }

    public boolean claimReturn(UUID player, int index) {
        List<String> list = storage.getReturns(player);
        if (index < 0 || index >= list.size()) return false;
        String entry = list.get(index);
        String[] parts = entry.split("\\|");
        if (parts.length != 2) return false;

        String base64 = parts[0];
        int amount;
        try { amount = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { return false; }

        Player p = Bukkit.getPlayer(player);
        if (p == null) return false;

        try {
            ItemStack base = ItemSer.fromBase64(base64).clone();
            int leftAmt = amount;
            while (leftAmt > 0) {
                int give = Math.min(64, leftAmt);
                ItemStack stack = base.clone();
                stack.setAmount(give);
                var left = p.getInventory().addItem(stack);
                if (!left.isEmpty()) {
                    for (ItemStack it : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), it);
                }
                leftAmt -= give;
            }

            list.remove(index);
            storage.setReturns(player, list);
            storage.saveReturns();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
