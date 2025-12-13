package ru.illit.auction;

import java.util.UUID;

public final class Listing {
    public final String id;
    public final UUID seller;
    public final String sellerName;
    public final long createdAtMs;
    public final long expiresAtMs;
    public final String itemBase64;
    public final int amountRemaining;
    public final double unitPrice;

    public Listing(String id, UUID seller, String sellerName, long createdAtMs, long expiresAtMs,
                   String itemBase64, int amountRemaining, double unitPrice) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.createdAtMs = createdAtMs;
        this.expiresAtMs = expiresAtMs;
        this.itemBase64 = itemBase64;
        this.amountRemaining = amountRemaining;
        this.unitPrice = unitPrice;
    }

    public boolean expired(long nowMs) { return nowMs >= expiresAtMs; }
}
