package com.illit.auction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

public class VaultEconomyBridge implements EconomyBridge {

    private final Economy eco;

    public VaultEconomyBridge(Economy eco) {
        this.eco = eco;
    }

    @Override public boolean isAvailable() { return eco != null; }
    @Override public boolean has(OfflinePlayer player, double amount) { return eco.has(player, amount); }
    @Override public boolean withdraw(OfflinePlayer player, double amount) { return eco.withdrawPlayer(player, amount).transactionSuccess(); }
    @Override public void deposit(OfflinePlayer player, double amount) { eco.depositPlayer(player, amount); }
    @Override public String format(double amount) { return eco.format(amount); }
}
