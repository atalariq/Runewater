package com.battleship.model;

import com.battleship.interfaces.Describable;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.StatusEffect;

/**
 * Kapal musuh yang di-generate setiap stage. Punya EnemyTrait (sifat pasif)
 * yang muncul acak dan memaksa pemain menyesuaikan strategi.
 * AI: selalu menyerang dengan serangan fisik dasar per giliran.
 */
public class EnemyShip extends Ship implements Describable {

    private int        bountyReward;
    private int        xpReward;
    private EnemyTrait trait;

    public EnemyShip(String name, int maxHp, int baseDamage, Element element,
                     int bountyReward, int xpReward, EnemyTrait trait) {
        super(name, maxHp, baseDamage, element);
        this.bountyReward = bountyReward;
        this.xpReward     = xpReward;
        this.trait        = trait;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int        getBountyReward() { return bountyReward; }
    public int        getXpReward()     { return xpReward; }
    public EnemyTrait getTrait()        { return trait; }

    // -------------------------------------------------------------------------
    // Trait Processing
    // -------------------------------------------------------------------------

    public String processTrait() {
        if (trait != EnemyTrait.REGENERATE) return "";
        int healAmount = 10;
        setCurrentHp(getCurrentHp() + healAmount);
        return String.format("  [REGEN] %s memulihkan %d HP!%n", getName(), healAmount);
    }

    /** Hitung damage keluar dengan bonus BERSERKER jika aktif. */
    public int getBerserkerDamage(int rawDamage) {
        if (trait == EnemyTrait.BERSERKER && (double) getCurrentHp() / getMaxHp() < 0.5) {
            return (int)(rawDamage * 1.5);
        }
        return rawDamage;
    }

    /** Kurangi damage fisik yang masuk jika musuh ARMORED. */
    public int applyArmorReduction(int damage, boolean isPhysical) {
        if (trait == EnemyTrait.ARMORED && isPhysical) {
            return (int)(damage * 0.65);
        }
        return damage;
    }

    // -------------------------------------------------------------------------
    // Ship Abstract Methods
    // -------------------------------------------------------------------------

    @Override
    public void takeTurn(Ship opponent) {
        System.out.print(takeTurnWithLog(opponent));
    }

    public String takeTurnWithLog(Ship opponent) {
        StringBuilder log = new StringBuilder();
        String traitLog = processTrait();
        if (!traitLog.isEmpty()) log.append(traitLog);

        int rawDamage   = effectiveDamage(getBaseDamage());
        int finalDamage = getBerserkerDamage(rawDamage);
        opponent.takeDamage(finalDamage);

        String berserkerTag = (trait == EnemyTrait.BERSERKER && finalDamage > rawDamage)
                ? " [BERSERKER!]" : "";
        log.append(String.format("  [MUSUH] %s menyerang! Damage: %d%s%n",
                getName(), finalDamage, berserkerTag));
        return log.toString();
    }

    // -------------------------------------------------------------------------
    // Describable Implementation
    // -------------------------------------------------------------------------

    @Override
    public String getFullDescription() {
        String statusTag = (getStatus() != StatusEffect.NONE) ? getStatus().getTag() : "";
        return String.format(
            "  %-24s | %s%s | Elemen: %-5s | Dmg: ~%d | Sifat: %-12s | Bounty: $%d",
            getName(), getHpBar(), statusTag,
            getElement().sym(), getBaseDamage(), trait.displayName, bountyReward);
    }

    @Override
    public String getStatusDisplay() {
        return getFullDescription();
    }
}
