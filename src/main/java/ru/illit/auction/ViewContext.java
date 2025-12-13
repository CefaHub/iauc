package ru.illit.auction;

public final class ViewContext {
    public int page = 0;
    public AuctionService.Sort sort = AuctionService.Sort.NEW_FIRST;
    public String search = "";
    public ViewContext() {}
}
