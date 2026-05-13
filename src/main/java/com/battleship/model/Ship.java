package com.battleship.model;

import java.util.Random;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.StatusEffect;

/**
 * Base class untuk semua kapal dalam game. Mendefinisikan atribut dasar,
 * sistem HP, sistem status efek, dan dua variasi serangan (overloading).
 * Subclass WAJIB mengimplementasikan takeTurn() dan getStatusDisplay().
 */
public abstract class Ship {

    private String       name;
    private int          maxHp;
    private int          currentHp;
    private int          baseDamage;
    private Element      element;
    private StatusEffect status;
    private int          statusDuration;

    protected static final Random RNG = new Random();

    public Ship(String name, int maxHp, int baseDamage, Element element) {
        this.name           = name;
        this.maxHp          = maxHp;
        this.currentHp      = maxHp;
        this.baseDamage     = baseDamage;
        this.element        = element;
        this.status         = StatusEffect.NONE;
        this.statusDuration = 0;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String       getName()        { return name; }
    public int          getMaxHp()       { return maxHp; }
    public int          getCurrentHp()   { return currentHp; }
    public int          getBaseDamage()  { return baseDamage; }
    public Element      getElement()     { return element; }
    public StatusEffect getStatus()      { return status; }
    public boolean      isAlive()        { return currentHp > 0; }
    public boolean      isFrozen()       { return status == StatusEffect.FROZEN; }
    public boolean      isWeakened()     { return status == StatusEffect.WEAKENED; }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(hp, maxHp));
    }

    public void setBaseDamage(int damage) {
        this.baseDamage = Math.max(1, damage);
    }

    public void takeDamage(int damage) {
        this.currentHp = Math.max(0, currentHp - Math.max(0, damage));
    }

    /**
     * Terapkan status efek ke kapal ini.
     * FROZEN kebal terhadap status lain — tidak bisa di-overwrite.
     */
    public void applyStatus(StatusEffect newStatus, int duration) {
        if (this.status == StatusEffect.FROZEN && newStatus != StatusEffect.NONE) return;
        this.status         = newStatus;
        this.statusDuration = duration;
    }

    // -------------------------------------------------------------------------
    // Status Effect Processing
    // -------------------------------------------------------------------------

    /**
     * Proses semua status efek aktif di awal giliran kapal ini.
     * @return String log semua efek yang terjadi.
     */
    public String processStatus() {
        if (status == StatusEffect.NONE) return "";
        StringBuilder log = new StringBuilder();

        switch (status) {
            case BURNED:
                int burnDmg = 8;
                takeDamage(burnDmg);
                statusDuration--;
                log.append(String.format("  [BURNED]   %s terbakar! -%d HP (sisa %d turn)%n",
                        name, burnDmg, statusDuration));
                break;

            case WEAKENED:
                statusDuration--;
                log.append(String.format("  [WEAKENED] %s masih lemah (%d turn lagi).%n",
                        name, statusDuration));
                break;

            case FROZEN:
                statusDuration--;
                log.append(String.format("  [FROZEN]   %s membeku! Giliran dilewati.%n", name));
                break;

            default:
                break;
        }

        if (statusDuration <= 0) {
            log.append(String.format("  [STATUS]   Efek %s pada %s habis.%n",
                    status.name(), name));
            status = StatusEffect.NONE;
        }

        return log.toString();
    }

    // -------------------------------------------------------------------------
    // Damage Calculation
    // -------------------------------------------------------------------------

    protected int effectiveDamage(int rawDamage) {
        return isWeakened() ? (int)(rawDamage * 0.70) : rawDamage;
    }

    // -------------------------------------------------------------------------
    // Method Overloading — Attack
    // -------------------------------------------------------------------------

    /** Serangan fisik/meriam. Netral, tidak ada elemen. */
    public int attack(Ship target) {
        int damage = effectiveDamage(baseDamage);
        target.takeDamage(damage);
        return damage;
    }

    /** Serangan sihir via Mage. Damage dipengaruhi elemen. */
    public int attack(Ship target, Mage mage) {
        double multiplier = mage.getElement().getMultiplier(target.getElement());
        int    rawDamage  = (int)((baseDamage + mage.getMagicPower()) * multiplier);
        int    damage     = effectiveDamage(rawDamage);
        target.takeDamage(damage);
        return damage;
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    /** HP bar visual berbasis teks dengan tag status efek jika aktif. */
    public String getHpBar() {
        int           barLength = 20;
        int           filled    = (int)((double) currentHp / maxHp * barLength);
        StringBuilder bar       = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) bar.append(i < filled ? "#" : ".");
        bar.append("]");
        String statusTag = (status != StatusEffect.NONE) ? " " + status.getTag() : "";
        return String.format("%s %d/%d%s", bar, currentHp, maxHp, statusTag);
    }

    // -------------------------------------------------------------------------
    // Abstract Methods
    // -------------------------------------------------------------------------

    public abstract void takeTurn(Ship opponent);

    public abstract String getStatusDisplay();
}
