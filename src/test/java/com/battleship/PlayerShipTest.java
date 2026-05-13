package com.battleship;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.battleship.model.PlayerShip;
import com.battleship.model.EnemyShip;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.StatusEffect;

class PlayerShipTest {

    @Test
    void takeDamage_reducesHp() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);

        // Act
        ship.takeDamage(30);

        // Assert
        assertEquals(70, ship.getCurrentHp(), "HP should be reduced by 30");
    }

    @Test
    void takeDamage_withShield_halvesDamage() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);
        ship.activateShield();

        // Act
        ship.takeDamage(30);

        // Assert
        assertEquals(85, ship.getCurrentHp(), "Shield should halve damage (30/2=15)");
    }

    @Test
    void heal_increasesHp() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);
        ship.takeDamage(50);

        // Act
        ship.heal(20);

        // Assert
        assertEquals(70, ship.getCurrentHp(), "HP should increase by 20");
    }

    @Test
    void heal_cappedAtMaxHp() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);

        // Act
        ship.heal(50);

        // Assert
        assertEquals(100, ship.getCurrentHp(), "HP should be capped at maxHP");
    }

    @Test
    void applyStatus_burned() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);

        // Act
        ship.applyStatus(StatusEffect.BURNED, 3);

        // Assert
        assertEquals(StatusEffect.BURNED, ship.getStatus(), "Status should be BURNED");

        // Process status
        String log = ship.processStatus();

        // Assert
        assertTrue(log.contains("BURNED"), "Log should contain BURNED");
        assertEquals(92, ship.getCurrentHp(), "BURNED should deal 8 damage");
    }

    @Test
    void applyStatus_weakened() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);
        EnemyShip enemy = new EnemyShip("Enemy", 100, 20, Element.WATER, 100, 20, EnemyTrait.NONE);

        // Act
        ship.applyStatus(StatusEffect.WEAKENED, 3);

        // Assert
        assertEquals(StatusEffect.WEAKENED, ship.getStatus(), "Status should be WEAKENED");

        // WEAKENED reduces damage by 30%
        int damage = ship.attack(enemy);
        assertEquals(14, damage, "WEAKENED should reduce damage by 30% (20*0.7=14)");
    }

    @Test
    void applyStatus_frozen_blocksOtherStatus() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);

        // Act
        ship.applyStatus(StatusEffect.FROZEN, 1);
        ship.applyStatus(StatusEffect.BURNED, 3); // Should be ignored

        // Assert
        assertEquals(StatusEffect.FROZEN, ship.getStatus(), "FROZEN should not be overwritten");
    }

    @Test
    void isAlive_trueWhenHpPositive() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);

        // Assert
        assertTrue(ship.isAlive(), "Ship should be alive with positive HP");

        // Act
        ship.takeDamage(100);

        // Assert
        assertFalse(ship.isAlive(), "Ship should be dead with 0 HP");
    }

    @Test
    void addBounty_increasesBounty() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);

        // Act
        ship.addBounty(100);

        // Assert
        assertEquals(100, ship.getBounty(), "Bounty should be 100");
    }

    @Test
    void ammoCount_initialState() {
        // Arrange
        PlayerShip ship = new PlayerShip("TestShip", 100, 20, Element.FIRE);

        // Assert
        assertEquals(-1, ship.getAmmoCount(com.battleship.model.enums.CannonballType.IRON), "Iron ammo should be unlimited (-1)");
        assertEquals(3, ship.getAmmoCount(com.battleship.model.enums.CannonballType.EXPLOSIVE), "Explosive ammo should be 3");
    }
}
