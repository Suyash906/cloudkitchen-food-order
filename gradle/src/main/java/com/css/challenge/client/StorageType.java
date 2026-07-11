package com.css.challenge.client;

public enum StorageType {
    COOLER(1),
    HEATER(1),
    SHELF(2);

    private final int decayMultiplier;

    StorageType(int decayMultiplier) {
        this.decayMultiplier = decayMultiplier;
    }

    public int getDecayMultiplier() {
        return decayMultiplier;
    }
}