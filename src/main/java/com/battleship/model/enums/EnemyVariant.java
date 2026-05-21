package com.battleship.model.enums;

public enum EnemyVariant {
    NORMAL(1.0, 1.0),
    DIFFERENT_ELEMENT(1.15, 1.0),
    ELITE(1.40, 1.20);

    private final double hpMultiplier;
    private final double damageMultiplier;

    EnemyVariant(double hpMultiplier, double damageMultiplier) {
        this.hpMultiplier = hpMultiplier;
        this.damageMultiplier = damageMultiplier;
    }

    public double getHpMultiplier() { return hpMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
}
