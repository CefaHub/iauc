package com.illit.auction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class Holder implements InventoryHolder {
    public final String kind;
    public final String extra;

    public Holder(String kind, String extra) {
        this.kind = kind;
        this.extra = extra;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
