package com.illit.auction;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionStore {

    private final IllitAuctionPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    private final Map<String, Listing> listings = new ConcurrentHashMap<>();

    public AuctionStore(IllitAuctionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "listings.yml");
        load();
    }

    public synchronized void load() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.yaml = YamlConfiguration.loadConfiguration(file);
        listings.clear();

        var sec = yaml.getConfigurationSection("listings");
        if (sec == null) return;

        for (String id : sec.getKeys(false)) {
            String path = "listings." + id + ".";
            try {
                UUID seller = UUID.fromString(yaml.getString(path + "seller", ""));
                String sellerName = yaml.getString(path + "sellerName", "unknown");
                long createdAt = yaml.getLong(path + "createdAtMs", System.currentTimeMillis());
                String item = yaml.getString(path + "item", "");
                String materialKey = yaml.getString(path + "materialKey", "");
                String displayKey = yaml.getString(path + "displayKey", "");
                int amt = yaml.getInt(path + "amountRemaining", 1);
                double unitPrice = yaml.getDouble(path + "unitPrice", 0.0);

                Listing l = new Listing(id, seller, sellerName, createdAt, item, materialKey, displayKey, amt, unitPrice);
                if (l.amountRemaining > 0) listings.put(id, l);
            } catch (Exception ignored) {}
        }
    }

    public synchronized void saveAll() {
        yaml.set("listings", null);
        for (Listing l : listings.values()) {
            String path = "listings." + l.id + ".";
            yaml.set(path + "seller", l.seller.toString());
            yaml.set(path + "sellerName", l.sellerName);
            yaml.set(path + "createdAtMs", l.createdAtMs);
            yaml.set(path + "item", l.itemBase64);
            yaml.set(path + "materialKey", l.materialKey);
            yaml.set(path + "displayKey", l.displayKey);
            yaml.set(path + "amountRemaining", l.amountRemaining);
            yaml.set(path + "unitPrice", l.unitPrice);
        }
        try { yaml.save(file); } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить listings.yml: " + e.getMessage());
        }
    }

    public Collection<Listing> all() { return listings.values(); }
    public Listing get(String id) { return listings.get(id); }
    public void put(Listing l) { listings.put(l.id, l); }
    public void remove(String id) { listings.remove(id); }

    public String nextId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
