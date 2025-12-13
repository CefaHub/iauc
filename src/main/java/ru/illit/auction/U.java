package ru.illit.auction;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class U {
    private U() {}
    public static String c(String s){ return ChatColor.translateAlternateColorCodes('&', s); }

    private static final DecimalFormat DF;
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator(' ');
        DF = new DecimalFormat("#,##0", sym);
        DF.setGroupingUsed(true);
    }

    public static String fmt(long v){ return DF.format(Math.max(0, v)); }
}
