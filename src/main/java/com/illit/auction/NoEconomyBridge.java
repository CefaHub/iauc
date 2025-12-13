package com.illit.auction;

import org.bukkit.OfflinePlayer;

public class NoEconomyBridge implements EconomyBridge {
    @Override public boolean isAvailable() { return false; }
    @Override public boolean has(OfflinePlayer player, double amount) { return false; }
    @Override public boolean withdraw(OfflinePlayer player, double amount) { return false; }
    @Override public void deposit(OfflinePlayer player, double amount) {}
    @Override public String format(double amount) { return String.valueOf(amount); }
}
