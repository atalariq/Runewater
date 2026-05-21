package com.battleship;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import com.battleship.model.BossShip;
import com.battleship.model.EnemyShip;
import com.battleship.model.Mage;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyVariant;
import com.battleship.system.StageFactory;
import com.battleship.util.BalanceConfig;

class StageFactoryTest {

    private final StageFactory factory = new StageFactory(new Random(42));

    @Test
    void normalEnemy_hasCorrectStatsForStage() {
        for (int stage = 1; stage <= 20; stage++) {
            EnemyShip enemy = factory.generateEnemy(stage, EnemyVariant.NORMAL, null);

            int expectedHp = BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE;
            int expectedDmg = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
            int expectedBounty = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE;
            int expectedXp = BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE;

            assertEquals(expectedHp, enemy.getMaxHp(), "NORMAL HP mismatch at stage " + stage);
            assertEquals(expectedDmg, enemy.getBaseDamage(), "NORMAL DMG mismatch at stage " + stage);
            assertEquals(expectedBounty, enemy.getBountyReward(), "NORMAL bounty mismatch at stage " + stage);
            assertEquals(expectedXp, enemy.getXpReward(), "NORMAL XP mismatch at stage " + stage);
        }
    }

    @Test
    void differentElementEnemy_excludesGivenElement() {
        for (int stage = 1; stage <= 20; stage++) {
            EnemyShip enemy = factory.generateEnemy(stage, EnemyVariant.DIFFERENT_ELEMENT, Element.FIRE);
            assertNotEquals(Element.FIRE, enemy.getElement(),
                    "DIFFERENT_ELEMENT should not be FIRE at stage " + stage);
        }
    }

    @Test
    void differentElementEnemy_excludesDifferentElements() {
        factory.generateEnemy(1, EnemyVariant.DIFFERENT_ELEMENT, Element.WATER);
        factory.generateEnemy(1, EnemyVariant.DIFFERENT_ELEMENT, Element.STORM);
    }

    @Test
    void differentElementEnemy_hasCorrectStats() {
        for (int stage = 1; stage <= 20; stage++) {
            EnemyShip enemy = factory.generateEnemy(stage, EnemyVariant.DIFFERENT_ELEMENT, Element.WATER);

            int baseHp = BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE;
            int baseDmg = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
            int baseBounty = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE;
            int baseXp = BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE;

            assertEquals((int) (baseHp * 1.15), enemy.getMaxHp(),
                    "DIFFERENT_ELEMENT HP mismatch at stage " + stage);
            assertEquals(baseDmg, enemy.getBaseDamage(),
                    "DIFFERENT_ELEMENT DMG mismatch at stage " + stage);
            assertEquals((int) (baseBounty * 1.30), enemy.getBountyReward(),
                    "DIFFERENT_ELEMENT bounty mismatch at stage " + stage);
            assertEquals((int) (baseXp * 1.15), enemy.getXpReward(),
                    "DIFFERENT_ELEMENT XP mismatch at stage " + stage);
        }
    }

    @Test
    void eliteEnemy_hasCorrectStats() {
        for (int stage = 1; stage <= 20; stage++) {
            EnemyShip enemy = factory.generateEnemy(stage, EnemyVariant.ELITE, null);

            int baseHp = BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE;
            int baseDmg = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
            int baseBounty = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE;
            int baseXp = BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE;

            assertEquals((int) (baseHp * 1.40), enemy.getMaxHp(),
                    "ELITE HP mismatch at stage " + stage);
            assertEquals((int) (baseDmg * 1.20), enemy.getBaseDamage(),
                    "ELITE DMG mismatch at stage " + stage);
            assertEquals((int) (baseBounty * 1.80), enemy.getBountyReward(),
                    "ELITE bounty mismatch at stage " + stage);
            assertEquals((int) (baseXp * 1.50), enemy.getXpReward(),
                    "ELITE XP mismatch at stage " + stage);
        }
    }

    @Test
    void eliteEnemy_hasEliteSuffix() {
        EnemyShip enemy = factory.generateEnemy(1, EnemyVariant.ELITE, null);
        assertTrue(enemy.getName().contains("[ELITE]"),
                "ELITE enemy name should contain [ELITE] suffix");
    }

    @Test
    void boss_hasCorrectStatsAtMilestones() {
        int[] bossStages = {5, 10, 15, 20};
        for (int stage : bossStages) {
            BossShip boss = factory.generateBoss(stage);

            int baseHp = BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE;
            int baseDmg = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
            int expectedHp = (int) (baseHp * BalanceConfig.BOSS_HP_MULT);
            int expectedDmg = (int) (baseDmg * BalanceConfig.BOSS_DMG_MULT);
            int expectedBounty = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE
                    + BalanceConfig.BOSS_BOUNTY_BON;
            int expectedXp = (BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE) * 2;

            assertEquals(expectedHp, boss.getMaxHp(), "BOSS HP mismatch at stage " + stage);
            assertEquals(expectedDmg, boss.getBaseDamage(), "BOSS DMG mismatch at stage " + stage);
            assertEquals(expectedBounty, boss.getBountyReward(), "BOSS bounty mismatch at stage " + stage);
            assertEquals(expectedXp, boss.getXpReward(), "BOSS XP mismatch at stage " + stage);
            assertTrue(boss.getName().contains("[BOSS]"),
                    "BOSS name should contain [BOSS] suffix at stage " + stage);
        }
    }

    @Test
    void boss_hasBossTitle() {
        BossShip boss = factory.generateBoss(5);
        assertNotNull(boss.getBossTitle());
        assertFalse(boss.getBossTitle().isBlank());
    }

    @Test
    void generateMage_hasCorrectPower() {
        Mage mage = factory.generateMage(22);
        assertEquals(22, mage.getMagicPower());
    }

    @Test
    void generateMage_randomizesNameAndElement() {
        Mage mage1 = factory.generateMage(30);
        Mage mage2 = factory.generateMage(30);
        assertNotNull(mage1.getName());
        assertNotNull(mage1.getElement());
        assertEquals(30, mage1.getMagicPower());
    }
}
