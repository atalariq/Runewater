package com.battleship.model.enums;

/**
 * Tiga elemen sihir dengan logika segitiga: FIRE > STORM > WATER > FIRE
 * Setiap elemen tahu multiplier-nya terhadap elemen lain.
 */
public enum Element {
    FIRE, WATER, STORM;

    /** Multiplier damage: 2.0 super effective, 0.5 not effective, 1.0 netral. */
    public double getMultiplier(Element defender) {
        if (this == FIRE  && defender == STORM) return 2.0;
        if (this == STORM && defender == WATER) return 2.0;
        if (this == WATER && defender == FIRE)  return 2.0;
        if (this == FIRE  && defender == WATER) return 0.5;
        if (this == STORM && defender == FIRE)  return 0.5;
        if (this == WATER && defender == STORM) return 0.5;
        return 1.0;
    }

    /** Elemen yang mengalahkan elemen ini. */
    public Element weakness() {
        return this == FIRE ? WATER : this == WATER ? STORM : FIRE;
    }

    /** Simbol pendek 5 karakter untuk UI. */
    public String sym() {
        return this == FIRE ? "FIRE " : this == WATER ? "WATER" : "STORM";
    }

    /** Teks efektivitas serangan untuk ditampilkan ke pemain. */
    public String effectText(Element defender) {
        double m = getMultiplier(defender);
        return m > 1 ? "SUPER EFFECTIVE! (x2.0)" : m < 1 ? "Not Effective (x0.5)" : "Netral (x1.0)";
    }
}
