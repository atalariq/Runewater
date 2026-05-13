package com.battleship;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.battleship.model.Mage;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.SpellType;

class MageTest {

    @Test
    void gainXp_triggersLevelUp() {
        // Arrange
        Mage mage = new Mage("Ignis", Element.FIRE, 20, SpellType.INFERNO);

        // Act
        boolean didLevelUp = mage.gainXp(35); // 35 >= 30 (XP_PER_LEVEL)

        // Assert
        assertTrue(didLevelUp, "gainXp(35) should trigger level up");
        assertEquals(2, mage.getLevel(), "Level should be 2 after level up");
        assertEquals(5, mage.getXp(), "Remaining XP should be 5 (35-30)");
        assertEquals(27, mage.getMagicPower(), "Magic power should increase by 7");
    }

    @Test
    void gainXp_noLevelUp() {
        // Arrange
        Mage mage = new Mage("Ignis", Element.FIRE, 20, SpellType.INFERNO);

        // Act
        boolean didLevelUp = mage.gainXp(15);

        // Assert
        assertFalse(didLevelUp, "gainXp(15) should not trigger level up");
        assertEquals(1, mage.getLevel(), "Level should still be 1");
        assertEquals(15, mage.getXp(), "XP should be 15");
    }

    @Test
    void spellUsage() {
        // Arrange
        Mage mage = new Mage("Ignis", Element.FIRE, 20, SpellType.INFERNO);

        // Assert initial state
        assertFalse(mage.isSpellUsed(), "Spell should be unused initially");

        // Act
        mage.markSpellUsed();

        // Assert
        assertTrue(mage.isSpellUsed(), "Spell should be marked as used");

        // Act
        mage.resetSpell();

        // Assert
        assertFalse(mage.isSpellUsed(), "Spell should be reset to unused");
    }

    @Test
    void constructor_randomSpellType() {
        // Arrange
        Random rng = new Random(42); // seeded for determinism

        // Act
        Mage mage = new Mage("Voltus", Element.STORM, 25, rng);

        // Assert
        assertNotNull(mage.getSpellType(), "Random spell type should not be null");
        assertTrue(
            mage.getSpellType() == SpellType.CHAIN_BOLT || mage.getSpellType() == SpellType.OVERCHARGE,
            "Storm mage should have storm spell"
        );
    }

    @Test
    void upgradePower_increasesMagicPower() {
        // Arrange
        Mage mage = new Mage("Ignis", Element.FIRE, 20, SpellType.INFERNO);

        // Act
        mage.upgradePower(10);

        // Assert
        assertEquals(30, mage.getMagicPower(), "Magic power should increase by 10");
    }

    @Test
    void upgradePower_neverBelowOne() {
        // Arrange
        Mage mage = new Mage("Ignis", Element.FIRE, 5, SpellType.INFERNO);

        // Act
        mage.upgradePower(-10);

        // Assert
        assertEquals(1, mage.getMagicPower(), "Magic power should never go below 1");
    }
}
