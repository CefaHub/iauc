package com.illit.auction;

import org.bukkit.ChatColor;

public final class CC {
    private CC() {}

    public static String t(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
