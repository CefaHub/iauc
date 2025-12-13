package com.illit.auction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class GuiItems {
    private GuiItems() {}

    public static ItemStack button(Material m, String name, List<String> lore) {
        ItemStack is = new ItemStack(m);
        ItemMeta im = is.getItemMeta();
        if (im != null) {
            im.setDisplayName(Chat.color(name));
            if (lore != null) im.setLore(lore.stream().map(Chat::color).toList());
            is.setItemMeta(im);
        }
        return is;
    }
}
