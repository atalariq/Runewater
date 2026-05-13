package com.battleship.model;

import com.battleship.model.enums.EnemyTrait;

/**
 * Boss yang muncul setiap kelipatan 5 stage. Berbeda dari EnemyShip biasa:
 *  - Punya judul (bossTitle) untuk narasi
 *  - Fase RAGE (HP < 50%): menyerang DUA kali per giliran
 *  - Serangan kedua (Kutukan Laut) lebih lemah tapi tetap signifikan
 */
public class BossShip extends EnemyShip {

    private final String bossTitle;
    private final int    bonusDamage;

    public BossShip(String name, String bossTitle, int maxHp, int baseDamage,
                    com.battleship.model.enums.Element element, int bountyReward, int xpReward, EnemyTrait trait) {
        super(name, maxHp, baseDamage, element, bountyReward, xpReward, trait);
        this.bossTitle   = bossTitle;
        this.bonusDamage = baseDamage / 3;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String  getBossTitle() { return bossTitle; }

    public boolean isEnraged()    { return (double) getCurrentHp() / getMaxHp() < 0.5; }

    // -------------------------------------------------------------------------
    // Ship Abstract Methods
    // -------------------------------------------------------------------------

    @Override
    public void takeTurn(Ship opponent) {
        System.out.print(takeTurnWithLog(opponent));
    }

    @Override
    public String takeTurnWithLog(Ship opponent) {
        StringBuilder log = new StringBuilder();
        String traitLog = processTrait();
        if (!traitLog.isEmpty()) log.append(traitLog);

        log.append(String.format("%n  [BOSS] %s -- %s bergerak!%n", bossTitle, getName()));

        int rawDamage   = effectiveDamage(getBaseDamage());
        int finalDamage = getBerserkerDamage(rawDamage);
        opponent.takeDamage(finalDamage);

        String berserkerTag = (getTrait() == EnemyTrait.BERSERKER && finalDamage > rawDamage)
                ? " [BERSERKER!]" : "";
        log.append(String.format("  >> Serangan Utama: %d damage%s%n", finalDamage, berserkerTag));

        if (isEnraged() && opponent.isAlive()) {
            int rageDamage = Math.max(1,
                    (int)(bonusDamage * (0.85 + RNG.nextDouble() * 0.30)));
            opponent.takeDamage(rageDamage);
            log.append(String.format("  >> [RAGE] Kutukan Laut: %d damage tambahan!%n", rageDamage));
        }
        return log.toString();
    }

    // -------------------------------------------------------------------------
    // Describable Implementation
    // -------------------------------------------------------------------------

    @Override
    public String getFullDescription() {
        String rageTag = isEnraged() ? " *** ENRAGED! ***" : "";
        return String.format(
            "  [BOSS] %s -- %-18s%s%n  HP: %s | Elemen: %-5s | Dmg: ~%d | Sifat: %s",
            bossTitle, getName(), rageTag,
            getHpBar(), getElement().sym(), getBaseDamage(), getTrait().displayName);
    }

    @Override
    public String getStatusDisplay() {
        return getFullDescription();
    }
}
