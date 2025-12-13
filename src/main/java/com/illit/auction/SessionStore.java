package com.illit.auction;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    public GuiSession get(Player p) {
        return sessions.computeIfAbsent(p.getUniqueId(), (k) -> new GuiSession(GuiType.AUCTION));
    }
}
