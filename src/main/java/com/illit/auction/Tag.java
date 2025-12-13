package com.illit.auction;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class Tag {
    private Tag() {}

    public static final String KEY_LISTING = "listing";
    public static final String KEY_RETURN = "return_idx";

    public static ItemStack tagListing(ItemStack it, UUID listingId) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(IllitAuctionPlugin.instance(), KEY_LISTING),
                    PersistentDataType.STRING, listingId.toString());
            it.setItemMeta(meta);
        }
        return it;
    }

    public static UUID getListing(ItemStack it) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        String v = meta.getPersistentDataContainer().get(new NamespacedKey(IllitAuctionPlugin.instance(), KEY_LISTING),
                PersistentDataType.STRING);
        if (v == null) return null;
        try { return UUID.fromString(v); } catch (Exception e) { return null; }
    }

    public static ItemStack tagReturn(ItemStack it, int idx) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(IllitAuctionPlugin.instance(), KEY_RETURN),
                    PersistentDataType.INTEGER, idx);
            it.setItemMeta(meta);
        }
        return it;
    }

    public static Integer getReturnIdx(ItemStack it) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(new NamespacedKey(IllitAuctionPlugin.instance(), KEY_RETURN),
                PersistentDataType.INTEGER);
    }
}
