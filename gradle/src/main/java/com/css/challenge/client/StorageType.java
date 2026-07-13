package com.css.challenge.client;

public enum StorageType {
    COOLER(1, Action.COOLER),
    HEATER(1, Action.HEATER),
    SHELF(2, Action.SHELF);

    private final int decayMultiplier;
    private final String actionTarget;

    StorageType(int decayMultiplier, String actionTarget) {
        this.decayMultiplier = decayMultiplier;
        this.actionTarget = actionTarget;
    }

    public int getDecayMultiplier() {
        return decayMultiplier;
    }

    public String getActionTarget() {
        return actionTarget;
    }
}