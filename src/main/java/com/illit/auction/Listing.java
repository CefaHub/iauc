package com.illit.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Listing {
    private final UUID id;
    private final UUID sellerUuid;
    private String sellerName;
    private final long createdAtMs;
    private final long expireAtMs;

    private final String itemBase64;
    private final int totalAmount;
    private int remainingAmount;

    // price per item (integer units)
    private final long pricePerItem;

    public Listing(UUID id, UUID sellerUuid, String sellerName, long createdAtMs, long expireAtMs,
                   String itemBase64, int totalAmount, int remainingAmount, long pricePerItem) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.createdAtMs = createdAtMs;
        this.expireAtMs = expireAtMs;
        this.itemBase64 = itemBase64;
        this.totalAmount = totalAmount;
        this.remainingAmount = remainingAmount;
        this.pricePerItem = pricePerItem;
    }

    public UUID id() { return id; }
    public UUID sellerUuid() { return sellerUuid; }
    public String sellerName() { return sellerName; }
    public void sellerName(String v) { this.sellerName = v; }
    public long createdAtMs() { return createdAtMs; }
    public long expireAtMs() { return expireAtMs; }
    public String itemBase64() { return itemBase64; }
    public int totalAmount() { return totalAmount; }
    public int remainingAmount() { return remainingAmount; }
    public void remainingAmount(int v) { this.remainingAmount = v; }
    public long pricePerItem() { return pricePerItem; }

    public boolean isExpired(long nowMs) {
        return nowMs >= expireAtMs;
    }

    public ItemStack buildDisplayItem() {
        try {
            ItemStack base = ItemSer.fromBase64(itemBase64);
            ItemStack clone = base.clone();
            clone.setAmount(Math.max(1, Math.min(64, remainingAmount)));
            return clone;
        } catch (Exception e) {
            return null;
        }
    }
}
