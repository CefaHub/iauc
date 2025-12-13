package com.illit.auction;

import org.bukkit.OfflinePlayer;

public interface EconomyBridge {
    boolean isAvailable();
    boolean has(OfflinePlayer player, double amount);
    boolean withdraw(OfflinePlayer player, double amount);
    void deposit(OfflinePlayer player, double amount);
    String format(double amount);
}
