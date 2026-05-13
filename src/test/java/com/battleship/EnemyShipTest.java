package com.battleship;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.battleship.model.EnemyShip;
import com.battleship.model.PlayerShip;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;

class EnemyShipTest {

    @Test
    void applyArmorReduction_reducesPhysicalDamage() {
        // Arrange
        EnemyShip enemy = new EnemyShip("ArmoredEnemy", 100, 20, Element.FIRE, 100, 20, EnemyTrait.ARMORED);

        // Act
        int reduced = enemy.applyArmorReduction(100, true);

        // Assert
        assertEquals(65, reduced, "ARMORED should reduce physical damage by 35%");
    }

    @Test
    void applyArmorReduction_doesNotReduceMagicDamage() {
        // Arrange
        EnemyShip enemy = new EnemyShip("ArmoredEnemy", 100, 20, Element.FIRE, 100, 20, EnemyTrait.ARMORED);

        // Act
        int reduced = enemy.applyArmorReduction(100, false);

        // Assert
        assertEquals(100, reduced, "ARMORED should not reduce magic damage");
    }

    @Test
    void getBerserkerDamage_increasesWhenLowHp() {
        // Arrange
        EnemyShip enemy = new EnemyShip("Berserker", 100, 20, Element.FIRE, 100, 20, EnemyTrait.BERSERKER);
        enemy.takeDamage(60); // HP now 40 (< 50%)

        // Act
        int damage = enemy.getBerserkerDamage(20);

        // Assert
        assertEquals(30, damage, "BERSERKER should increase damage by 50% when HP < 50%");
    }

    @Test
    void getBerserkerDamage_normalWhenHighHp() {
        // Arrange
        EnemyShip enemy = new EnemyShip("Berserker", 100, 20, Element.FIRE, 100, 20, EnemyTrait.BERSERKER);
        enemy.takeDamage(30); // HP now 70 (> 50%)

        // Act
        int damage = enemy.getBerserkerDamage(20);

        // Assert
        assertEquals(20, damage, "BERSERKER should not increase damage when HP > 50%");
    }

    @Test
    void processTrait_regenerateHeals() {
        // Arrange
        EnemyShip enemy = new EnemyShip("Regenerator", 100, 20, Element.FIRE, 100, 20, EnemyTrait.REGENERATE);
        enemy.takeDamage(20); // HP now 80

        // Act
        String log = enemy.processTrait();

        // Assert
        assertTrue(log.contains("REGEN"), "REGENERATE should produce regen log");
        assertEquals(90, enemy.getCurrentHp(), "REGENERATE should heal 10 HP");
    }

    @Test
    void processTrait_noneDoesNothing() {
        // Arrange
        EnemyShip enemy = new EnemyShip("Normal", 100, 20, Element.FIRE, 100, 20, EnemyTrait.NONE);

        // Act
        String log = enemy.processTrait();

        // Assert
        assertTrue(log.isEmpty(), "NONE trait should produce no log");
    }

    @Test
    void thornsTrait_exists() {
        // Arrange
        EnemyShip enemy = new EnemyShip("Thorny", 100, 20, Element.FIRE, 100, 20, EnemyTrait.THORNS);

        // Assert
        assertEquals(EnemyTrait.THORNS, enemy.getTrait(), "Enemy should have THORNS trait");
    }
}
