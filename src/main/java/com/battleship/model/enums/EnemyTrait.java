package com.battleship.model.enums;

/**
 * Sifat pasif musuh. Muncul acak setiap stage, memaksa pemain
 * membaca situasi dan menyesuaikan strategi.
 *
 *  ARMORED   — Semua damage fisik (cannon) berkurang 35%.
 *              Counter: utamakan sihir Mage.
 *  REGENERATE— Pulihkan 10 HP di awal gilirannya.
 *              Counter: burst damage tinggi dalam sedikit turn.
 *  BERSERKER — Damage +50% saat HP < 50% (fase amuk).
 *              Counter: bunuh sebelum HP-nya drop ke 50%.
 *  THORNS    — Memantulkan 15% damage sihir kembali ke pemain.
 *              Counter: cannon atau jurus non-sihir (Inferno, Explosive).
 */
public enum EnemyTrait {
    NONE      ("Biasa",       "Tidak ada sifat khusus."),
    ARMORED   ("Berzirah",    "Semua damage fisik (cannon) berkurang 35%."),
    REGENERATE("Regenerasi",  "Pulihkan 10 HP di awal gilirannya."),
    BERSERKER ("Berserker",   "Damage +50% saat HP < 50% (fase amuk)."),
    THORNS    ("Berduri",     "Memantulkan 15% damage sihir kembali ke pemain.");

    public final String displayName;
    public final String description;

    EnemyTrait(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
