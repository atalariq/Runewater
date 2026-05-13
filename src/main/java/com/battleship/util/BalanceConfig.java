package com.battleship.util;

/**
 * Semua konstanta balance game di satu tempat.
 *
 * Keuntungan memisahkan ini: untuk menyesuaikan difficulty,
 * cukup membuka file ini — tidak perlu mencari angka di seluruh codebase.
 *
 * Formula scaling musuh:
 *   HP musuh     = BASE_ENEMY_HP  + (stage * HP_SCALE)
 *   Damage musuh = BASE_ENEMY_DMG + (stage * DMG_SCALE)
 *   Boss HP      = HP musuh       * BOSS_HP_MULT
 *   Boss Damage  = Damage musuh   * BOSS_DMG_MULT
 */
public class BalanceConfig {

    // ── Player awal ──────────────────────────────────────────────────────────
    public static final int    PLAYER_HP       = 120;
    public static final int    PLAYER_DAMAGE   = 18;
    public static final int    START_MAGE_PWR  = 22;

    // ── Scaling musuh per stage ───────────────────────────────────────────────
    public static final int    BASE_ENEMY_HP   = 55;
    public static final int    HP_SCALE        = 10;
    public static final int    BASE_ENEMY_DMG  = 10;
    public static final int    DMG_SCALE       = 2;

    // ── Boss multiplier (diterapkan ke stat enemy biasa) ──────────────────────
    public static final double BOSS_HP_MULT    = 2.2;
    public static final double BOSS_DMG_MULT   = 1.5;

    // ── XP & Bounty reward ────────────────────────────────────────────────────
    public static final int    BASE_XP         = 20;
    public static final int    XP_SCALE        = 3;
    public static final int    BASE_BOUNTY     = 120;
    public static final int    BOUNTY_SCALE    = 45;
    public static final int    BOSS_BOUNTY_BON = 400;
}
