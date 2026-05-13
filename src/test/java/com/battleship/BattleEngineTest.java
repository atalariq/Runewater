package com.battleship;

import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.battleship.engine.BattleEngine;
import com.battleship.model.PlayerShip;
import com.battleship.model.enums.Element;
import com.battleship.util.InputHelper;
import com.battleship.util.TerminalUi;

class BattleEngineTest {

    @Test
    void constructor_createsEngine() {
        // Arrange
        PlayerShip player = new PlayerShip("Test", 100, 20, Element.FIRE);
        InputHelper input = new InputHelper(new Scanner(System.in));
        TerminalUi ui = new TerminalUi(input);
        Random rng = new Random(42);
        AtomicBoolean cannonDoubled = new AtomicBoolean(false);

        // Act
        BattleEngine engine = new BattleEngine(player, ui, rng, cannonDoubled);

        // Assert
        assertNotNull(engine, "BattleEngine should be created successfully");
    }

    @Test
    void victoryCondition_playerWins() {
        // Arrange - Player with high HP vs weak enemy
        PlayerShip player = new PlayerShip("Test", 100, 50, Element.FIRE);
        com.battleship.model.EnemyShip enemy = new com.battleship.model.EnemyShip(
            "WeakEnemy", 10, 1, Element.WATER, 10, 5, com.battleship.model.enums.EnemyTrait.NONE);

        // Act - Player attacks enemy
        player.attack(enemy);

        // Assert
        assertFalse(enemy.isAlive(), "Enemy should be dead after taking 50 damage");
        assertTrue(player.isAlive(), "Player should still be alive");
    }

    @Test
    void victoryCondition_enemyWins() {
        // Arrange - Weak player vs strong enemy
        PlayerShip player = new PlayerShip("Test", 10, 1, Element.FIRE);
        com.battleship.model.EnemyShip enemy = new com.battleship.model.EnemyShip(
            "StrongEnemy", 100, 50, Element.WATER, 10, 5, com.battleship.model.enums.EnemyTrait.NONE);

        // Act - Enemy attacks player
        enemy.attack(player);

        // Assert
        assertFalse(player.isAlive(), "Player should be dead after taking 50 damage");
        assertTrue(enemy.isAlive(), "Enemy should still be alive");
    }
}
