package com.illit.auction;

import java.util.UUID;

public interface Economy {
    long getBalance(UUID uuid);
    void setBalance(UUID uuid, long balance);
    boolean withdraw(UUID uuid, long amount);
    void deposit(UUID uuid, long amount);
    boolean isEnabled();
}
