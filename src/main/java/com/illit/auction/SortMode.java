package com.illit.auction;

public enum SortMode {
    PRICE_ASC,
    DATE_NEW,
    DATE_OLD;

    public SortMode next() {
        return switch (this) {
            case PRICE_ASC -> DATE_NEW;
            case DATE_NEW -> DATE_OLD;
            case DATE_OLD -> PRICE_ASC;
        };
    }
}
