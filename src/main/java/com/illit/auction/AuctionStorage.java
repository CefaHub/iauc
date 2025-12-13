package com.illit.auction;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AuctionStorage {

    private final IllitAuctionPlugin plugin;
    private final File listingsFile;
    private final File returnsFile;

    private YamlConfiguration listingsYaml;
    private YamlConfiguration returnsYaml;

    public AuctionStorage(IllitAuctionPlugin plugin) {
        this.plugin = plugin;
        this.listingsFile = new File(plugin.getDataFolder(), "listings.yml");
        this.returnsFile = new File(plugin.getDataFolder(), "returns.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        try {
            if (!listingsFile.exists()) listingsFile.createNewFile();
            if (!returnsFile.exists()) returnsFile.createNewFile();
        } catch (IOException ignored) {}

        listingsYaml = YamlConfiguration.loadConfiguration(listingsFile);
        returnsYaml = YamlConfiguration.loadConfiguration(returnsFile);
    }

    public void saveListings() {
        try { listingsYaml.save(listingsFile); } catch (IOException ignored) {}
    }

    public void saveReturns() {
        try { returnsYaml.save(returnsFile); } catch (IOException ignored) {}
    }

    public Map<UUID, Listing> loadListings() {
        Map<UUID, Listing> out = new HashMap<>();
        for (String key : listingsYaml.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection s = listingsYaml.getConfigurationSection(key);
                if (s == null) continue;

                UUID seller = UUID.fromString(s.getString("sellerUuid", ""));
                String sellerName = s.getString("sellerName", "unknown");
                long created = s.getLong("createdAtMs", 0);
                long expire = s.getLong("expireAtMs", 0);
                String item = s.getString("item", "");
                int total = s.getInt("totalAmount", 1);
                int remaining = s.getInt("remainingAmount", total);
                long ppi = s.getLong("pricePerItem", 0);

                out.put(id, new Listing(id, seller, sellerName, created, expire, item, total, remaining, ppi));
            } catch (Exception ignored) {}
        }
        return out;
    }

    public void writeListing(Listing l) {
        String key = l.id().toString();
        listingsYaml.set(key + ".sellerUuid", l.sellerUuid().toString());
        listingsYaml.set(key + ".sellerName", l.sellerName());
        listingsYaml.set(key + ".createdAtMs", l.createdAtMs());
        listingsYaml.set(key + ".expireAtMs", l.expireAtMs());
        listingsYaml.set(key + ".item", l.itemBase64());
        listingsYaml.set(key + ".totalAmount", l.totalAmount());
        listingsYaml.set(key + ".remainingAmount", l.remainingAmount());
        listingsYaml.set(key + ".pricePerItem", l.pricePerItem());
    }

    public void removeListing(UUID id) {
        listingsYaml.set(id.toString(), null);
    }

    // returns: list of "base64|amount"
    public List<String> getReturns(UUID player) {
        return new ArrayList<>(returnsYaml.getStringList(player.toString()));
    }

    public void addReturn(UUID player, String itemBase64, int amount) {
        List<String> list = getReturns(player);
        list.add(itemBase64 + "|" + amount);
        returnsYaml.set(player.toString(), list);
    }

    public void setReturns(UUID player, List<String> encoded) {
        returnsYaml.set(player.toString(), encoded);
    }
}
