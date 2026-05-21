package com.battleship.model;

import com.battleship.engine.AttackContext;
import com.battleship.engine.AttackType;
import com.battleship.engine.DamagePipeline;
import com.battleship.engine.DamageResult;
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

        boolean isBerserker = getTrait() == EnemyTrait.BERSERKER
                && (double) getCurrentHp() / getMaxHp() < 0.5;

        AttackContext ctx = new AttackContext(
            this, opponent, AttackType.PHYSICAL, getBaseDamage(),
            1.0, 1.0, null, null, null,
            EnemyTrait.NONE,
            isBerserker
        );
        DamageResult result = DamagePipeline.resolve(ctx);
        opponent.takeDamage(result.finalDamage());

        String berserkerTag = isBerserker ? " [BERSERKER!]" : "";
        log.append(String.format("  >> Serangan Utama: %d damage%s%n", result.finalDamage(), berserkerTag));

        if (isEnraged() && opponent.isAlive()) {
            int rageDamage = Math.max(1,
                    (int)(bonusDamage * (0.85 + RNG.nextDouble() * 0.30)));
            opponent.takeDamage(rageDamage);
            log.append(String.format("  >> [RAGE] Kutukan Laut: %d damage tambahan!%n", rageDamage));
        }
        return log.toString();
    }


}
