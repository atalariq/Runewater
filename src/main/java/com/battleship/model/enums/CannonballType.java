package com.battleship.model.enums;

import com.battleship.interfaces.Describable;

/**
 * Inti dari perbaikan sistem Cannon di v4. Setiap tipe peluru punya
 * TUJUAN STRATEGIS berbeda — bukan sekadar angka damage yang berbeda.
 * Ini yang membuat aksi "Tembak Meriam" selalu relevan.
 *
 *  IRON      — Stok tak terbatas. Fallback aman kapan saja.
 *              Pilihan terbaik: musuh hampir mati, atau hemat ammo langka.
 *
 *  EXPLOSIVE — Damage x2.5. Stok terbatas, harus diisi dari reward.
 *              Pilihan terbaik: burst kill boss atau musuh high-HP.
 *
 *  CHAIN     — Damage rendah (x0.6), tapi WEAKENED 3 turn (-30% dmg musuh).
 *              Pilihan terbaik: boss high-damage, beli waktu untuk strategi.
 *
 *  GRAPESHOT — Damage menengah (x0.85) + BURNED DoT 8 HP/turn selama 3 turn.
 *              Pilihan terbaik: tidak punya Mage FIRE, atau musuh ARMORED.
 */
public enum CannonballType implements Describable {
    IRON     ("Peluru Besi",   "Damage solid. Stok tak terbatas.",                  1.0,  false, false),
    EXPLOSIVE("Peluru Ledak",  "Damage x2.5. Stok terbatas — pakai bijak!",         2.5,  false, false),
    CHAIN    ("Peluru Rantai", "Damage rendah. WEAKENED musuh 3 turn (-30% dmg).",  0.6,  false, true ),
    GRAPESHOT("Peluru Angin",  "Damage menengah + BURNED musuh 3 turn (DoT 8/t).",  0.85, true,  false);

    private final String displayName;
    private final String description;
    public final double  dmgMult;
    public final boolean appliesBurn;
    public final boolean appliesWeak;

    CannonballType(String displayName, String description,
                   double dmgMult, boolean appliesBurn, boolean appliesWeak) {
        this.displayName  = displayName;
        this.description  = description;
        this.dmgMult      = dmgMult;
        this.appliesBurn  = appliesBurn;
        this.appliesWeak  = appliesWeak;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String getFullDescription() {
        return String.format("%-14s — %s", displayName, description);
    }
}
