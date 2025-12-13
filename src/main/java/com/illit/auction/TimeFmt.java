package com.illit.auction;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeFmt {
    private TimeFmt() {}

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public static String fmt(long epochMs) {
        return DF.format(Instant.ofEpochMilli(epochMs));
    }
}
