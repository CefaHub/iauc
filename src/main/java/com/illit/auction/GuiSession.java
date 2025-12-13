package com.illit.auction;

import java.util.UUID;

public class GuiSession {
    public GuiType type;
    public int page = 0;
    public AuctionService.SortMode sort = AuctionService.SortMode.DATE_NEW;
    public String search = "";
    public UUID filterSeller = null;
    public String filterSellerName = "";
    public UUID selectedListing = null;
    public int selectedQty = 1;

    public GuiSession(GuiType type) {
        this.type = type;
    }
}
