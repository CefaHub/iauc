package ru.illit.auction;

import org.bukkit.NamespacedKey;

public final class Keys {
    private Keys(){}
    public static NamespacedKey LISTING_ID;

    public static void init(IllitAuctionPlugin plugin) {
        LISTING_ID = new NamespacedKey(plugin, "listing_id");
    }
}
