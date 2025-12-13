package com.illit.auction;

import java.util.UUID;

public class ViewContext {
    public enum Type { MAIN, MY, PLAYER, SEARCH }

    public final UUID viewer;
    public Type type = Type.MAIN;
    public int page = 0;
    public SortMode sort = SortMode.DATE_NEW;

    public UUID filterSeller = null;
    public String searchQuery = null;

    public ViewContext(UUID viewer) {
        this.viewer = viewer;
    }
}
