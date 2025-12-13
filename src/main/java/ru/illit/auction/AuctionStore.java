package ru.illit.auction;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class AuctionStore {
    private final File file;
    private YamlConfiguration cfg;

    public AuctionStore(File file) {
        this.file = file;
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void reload() { this.cfg = YamlConfiguration.loadConfiguration(file); }

    public synchronized void save() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (!file.exists()) file.createNewFile();
        cfg.save(file);
    }

    public synchronized List<Listing> loadAll() {
        reload();
        List<Listing> out = new ArrayList<>();
        var sec = cfg.getConfigurationSection("listings");
        if (sec == null) return out;
        for (String id : sec.getKeys(false)) {
            var s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                UUID seller = UUID.fromString(s.getString("seller", ""));
                String sellerName = s.getString("sellerName", "Unknown");
                long created = s.getLong("createdAtMs", 0L);
                long expires = s.getLong("expiresAtMs", 0L);
                String item = s.getString("itemBase64", "");
                int amount = s.getInt("amountRemaining", 0);
                double price = s.getDouble("unitPrice", 0D);
                if (amount <= 0 || item.isEmpty()) continue;
                out.add(new Listing(id, seller, sellerName, created, expires, item, amount, price));
            } catch (Exception ignored) {}
        }
        return out;
    }

    public synchronized void saveAll(List<Listing> listings) {
        cfg.set("listings", null);
        for (Listing l : listings) {
            String p = "listings." + l.id + ".";
            cfg.set(p + "seller", l.seller.toString());
            cfg.set(p + "sellerName", l.sellerName);
            cfg.set(p + "createdAtMs", l.createdAtMs);
            cfg.set(p + "expiresAtMs", l.expiresAtMs);
            cfg.set(p + "itemBase64", l.itemBase64);
            cfg.set(p + "amountRemaining", l.amountRemaining);
            cfg.set(p + "unitPrice", l.unitPrice);
        }
        try { save(); } catch (Exception ignored) {}
    }
}
