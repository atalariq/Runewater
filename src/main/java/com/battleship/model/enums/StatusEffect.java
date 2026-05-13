package com.battleship.model.enums;

/**
 * Efek status yang bisa menempel ke kapal selama pertempuran.
 *
 *  BURNED   — Kehilangan 8 HP per giliran selama beberapa turn (DoT).
 *  WEAKENED — Damage keluar berkurang 30% selama beberapa turn.
 *  FROZEN   — Skip giliran berikutnya DAN kebal terhadap status lain.
 */
public enum StatusEffect {
    NONE, BURNED, WEAKENED, FROZEN;

    /** Tag singkat untuk ditampilkan di HP bar. */
    public String getTag() {
        switch (this) {
            case BURNED:   return "[BURNED]";
            case WEAKENED: return "[WEAKENED]";
            case FROZEN:   return "[FROZEN]";
            default:       return "";
        }
    }
}
