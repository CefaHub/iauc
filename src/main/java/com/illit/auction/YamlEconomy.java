package com.illit.auction;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class YamlEconomy implements Economy {

    private final IllitAuctionPlugin plugin;
    private final boolean enabled;
    private final long starting;
    private final File file;
    private YamlConfiguration yaml;

    public YamlEconomy(IllitAuctionPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("economy.enabled", true);
        this.starting = Math.max(0, plugin.getConfig().getLong("economy.starting-balance", 0));
        this.file = new File(plugin.getDataFolder(), "balances.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try { yaml.save(file); } catch (IOException ignored) {}
    }

    @Override public long getBalance(UUID uuid) {
        if (!enabled) return 0;
        String k = uuid.toString();
        if (!yaml.contains(k)) {
            yaml.set(k, starting);
            save();
        }
        return Math.max(0, yaml.getLong(k, starting));
    }

    @Override public void setBalance(UUID uuid, long balance) {
        if (!enabled) return;
        yaml.set(uuid.toString(), Math.max(0, balance));
        save();
    }

    @Override public boolean withdraw(UUID uuid, long amount) {
        if (!enabled) return true; // если экономика выключена — покупки бесплатные
        if (amount <= 0) return true;
        long bal = getBalance(uuid);
        if (bal < amount) return false;
        setBalance(uuid, bal - amount);
        return true;
    }

    @Override public void deposit(UUID uuid, long amount) {
        if (!enabled) return;
        if (amount <= 0) return;
        long bal = getBalance(uuid);
        setBalance(uuid, bal + amount);
    }

    @Override public boolean isEnabled() { return enabled; }
}
