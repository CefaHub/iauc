package com.illit.auction;

import org.bukkit.plugin.java.JavaPlugin;

public final class IllitAuctionPlugin extends JavaPlugin {

    private static IllitAuctionPlugin INSTANCE;

    private AuctionStorage storage;
    private YamlEconomy economy;
    private AuctionService service;
    private GuiManager gui;
    private SessionStore sessions;

    private String prefix;

    public static IllitAuctionPlugin instance() {
        return INSTANCE;
    }

    public String prefix() {
        return prefix;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        saveDefaultConfig();
        this.prefix = CC.t(getConfig().getString("prefix", "&7[&f&lILLIT AH&7]&r "));

        this.storage = new AuctionStorage(this);
        this.economy = new YamlEconomy(this);
        this.service = new AuctionService(this, storage, economy);
        this.sessions = new SessionStore();
        this.gui = new GuiManager(this, service, economy);

        getCommand("ah").setExecutor(new AhCommand(this, service, gui, sessions));
        getCommand("illit").setExecutor(new IllitAdminCommand(this, service, economy));

        getServer().getPluginManager().registerEvents(new GuiListener(this, service, gui, sessions), this);

        long periodTicks = 20L * 60L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            try { service.expireSweep(); } catch (Exception ignored) {}
        }, periodTicks, periodTicks);

        getLogger().info("IllitAuction включён.");
    }

    @Override
    public void onDisable() {
        try { service.saveAll(); } catch (Exception ignored) {}
        getLogger().info("IllitAuction выключен.");
    }
}
