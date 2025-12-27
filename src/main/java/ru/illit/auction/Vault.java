package ru.illit.auction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class Vault {
    private Economy eco;

    public boolean setup() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        eco = rsp.getProvider();
        return eco != null;
    }

    public Economy economy(){ return eco; }

    public boolean has(OfflinePlayer p, double amount) {
        return eco != null && eco.has(p, amount);
    }

    public boolean withdraw(OfflinePlayer p, double amount) {
        return eco != null && eco.withdrawPlayer(p, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer p, double amount) {
        return eco != null && eco.depositPlayer(p, amount).transactionSuccess();
    }

    public String format(double amount) {
        return eco != null ? eco.format(amount) : String.valueOf(amount);
    }
}
