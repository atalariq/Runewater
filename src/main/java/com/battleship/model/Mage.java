package com.battleship.model;

import java.util.Random;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.SpellType;

/**
 * Merepresentasikan satu penyihir kru kapal. Setiap Mage punya:
 *  - Elemen (FIRE/WATER/STORM) yang menentukan keunggulan vs musuh
 *  - SpellType unik yang bisa dipakai sekali per battle
 *  - Level & XP yang naik setelah tiap battle dimenangkan
 */
public class Mage {

    private final String    name;
    private final Element   element;
    private final SpellType spellType;
    private       int       magicPower;
    private       int       level;
    private       int       xp;
    private       boolean   spellUsed;

    private static final int XP_PER_LEVEL = 30;

    private static final SpellType[] FIRE_SPELLS  = { SpellType.INFERNO,    SpellType.IGNITE      };
    private static final SpellType[] WATER_SPELLS = { SpellType.TIDAL_WAVE, SpellType.FREEZE      };
    private static final SpellType[] STORM_SPELLS = { SpellType.CHAIN_BOLT, SpellType.OVERCHARGE  };

    /** Constructor untuk Mage dengan jurus acak sesuai elemennya. */
    public Mage(String name, Element element, int magicPower, Random rng) {
        this.name       = name;
        this.element    = element;
        this.magicPower = magicPower;
        this.level      = 1;
        this.xp         = 0;
        this.spellUsed  = false;

        SpellType[] pool = element == Element.FIRE  ? FIRE_SPELLS
                         : element == Element.WATER ? WATER_SPELLS
                         : STORM_SPELLS;
        this.spellType = pool[rng.nextInt(pool.length)];
    }

    /** Constructor untuk Mage dengan jurus spesifik. */
    public Mage(String name, Element element, int magicPower, SpellType spellType) {
        this.name       = name;
        this.element    = element;
        this.magicPower = magicPower;
        this.level      = 1;
        this.xp         = 0;
        this.spellUsed  = false;
        this.spellType  = spellType;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String    getName()        { return name; }
    public Element   getElement()     { return element; }
    public int       getMagicPower()  { return magicPower; }
    public int       getLevel()       { return level; }
    public int       getXp()          { return xp; }
    public SpellType getSpellType()   { return spellType; }
    public boolean   isSpellUsed()    { return spellUsed; }

    // -------------------------------------------------------------------------
    // Mutators
    // -------------------------------------------------------------------------

    public void markSpellUsed() { this.spellUsed = true; }
    public void resetSpell()    { this.spellUsed = false; }

    public void upgradePower(int amount) {
        this.magicPower = Math.max(1, magicPower + amount);
    }

    /** Berikan XP ke Mage ini. Jika cukup, otomatis naik level (+7 magicPower). */
    public boolean gainXp(int amount) {
        xp += amount;
        if (xp >= XP_PER_LEVEL) {
            xp         -= XP_PER_LEVEL;
            level++;
            magicPower += 7;
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    public String getInfo(boolean withSpell) {
        String spellPart = withSpell
            ? String.format(" | %s %s",
                spellType.getDisplayName(), spellUsed ? "[USED]" : "[READY]")
            : "";
        return String.format("Lv.%d %s %-8s Pwr:%-3d XP:%d/%d%s",
                level, element.sym(), name, magicPower, xp, XP_PER_LEVEL, spellPart);
    }
}
