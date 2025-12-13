package com.illit.auction;

import org.bukkit.ChatColor;

public final class ChatColorStrip {
    private ChatColorStrip() {}
    public static String strip(String s) {
        if (s == null) return "";
        return ChatColor.stripColor(Chat.color(s));
    }
}
