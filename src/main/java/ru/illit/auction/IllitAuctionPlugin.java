package ru.illit.auction;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class IllitAuctionPlugin extends JavaPlugin {

    private final Map<UUID, ViewContext> ctx = new HashMap<>();
    private final Map<UUID, GuiManager.BuyState> buy = new HashMap<>();

    private Vault vault;
    private AuctionStore store;
    private AuctionService service;
    private GuiManager gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        Keys.init(this);

        vault = new Vault();
        if (!vault.setup()) {
            getLogger().severe("Vault Economy не найден. Установи Vault + IllitMoney.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        store = new AuctionStore(new File(getDataFolder(), "listings.yml"));
        service = new AuctionService(this, store, vault);
        gui = new GuiManager(this, service);

        Bukkit.getPluginManager().registerEvents(new AuctionListener(this), this);

        bind("ah", new AhCommand(this));
        bind("ahsell", new AhSellCommand(this));
        bind("ahsearch", new AhSearchCommand(this));
        bind("illit", new IllitAhAdminCommand(this));

        getLogger().info("IllitAuction включен.");
    }

    private void bind(String name, Object exec) {
        PluginCommand c = getCommand(name);
        if (c == null) return;
        if (exec instanceof org.bukkit.command.CommandExecutor ce) c.setExecutor(ce);
        if (exec instanceof org.bukkit.command.TabCompleter tc) c.setTabCompleter(tc);
    }

    public Map<UUID, ViewContext> ctx(){ return ctx; }
    public Map<UUID, GuiManager.BuyState> buy(){ return buy; }
    public AuctionService service(){ return service; }
    public GuiManager gui(){ return gui; }
}
