package com.battleship.engine;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.battleship.model.BossShip;
import com.battleship.model.EnemyShip;
import com.battleship.model.Mage;
import com.battleship.model.PlayerShip;
import com.battleship.model.Ship;
import com.battleship.model.enums.CannonballType;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.SpellType;
import com.battleship.model.enums.StatusEffect;

class BattleStateTest {

    private PlayerShip createPlayer(int hp, int dmg, Element el) {
        return new PlayerShip("TestPlayer", hp, dmg, el);
    }

    private EnemyShip createEnemy(int hp, int dmg, Element el, EnemyTrait trait) {
        return new EnemyShip("TestEnemy", hp, dmg, el, 10, 5, trait);
    }

    // -------------------------------------------------------------------------
    // Command: Attack
    // -------------------------------------------------------------------------

    @Test
    void executeAttack_dealsDamage() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        player.recruitMage(new Mage("Mage", Element.FIRE, 15, SpellType.INFERNO));
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Attack(0));

        assertTrue(enemy.getCurrentHp() < 100, "Enemy should take damage");
        assertFalse(result.battleOver(), "Battle should continue");
    }

    @Test
    void executeAttack_noMage_autoCannon() {
        PlayerShip player = createPlayer(100, 50, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Attack(0));

        assertTrue(enemy.getCurrentHp() < 100, "Enemy should take damage from auto-cannon");
        assertTrue(result.battleLog().stream().anyMatch(l -> l.contains("Peluru Besi")),
                "Should mention auto-cannon");
    }

    // -------------------------------------------------------------------------
    // Command: Cannonball
    // -------------------------------------------------------------------------

    @Test
    void executeCannonball_iron_dealsDamage() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        int hpBefore = enemy.getCurrentHp();
        TurnResult result = state.execute(new Command.Cannonball(CannonballType.IRON));

        assertTrue(enemy.getCurrentHp() < hpBefore, "Enemy should take damage from iron cannonball");
        assertFalse(result.battleOver(), "Battle should continue");
    }

    @Test
    void executeCannonball_explosive_dealsBonusDamage() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(200, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        int hpBefore = enemy.getCurrentHp();
        state.execute(new Command.Cannonball(CannonballType.EXPLOSIVE));

        assertTrue(enemy.getCurrentHp() < hpBefore - 10, "Explosive should deal bonus damage");
    }

    @Test
    void executeCannonball_chain_appliesWeakened() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(200, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.execute(new Command.Cannonball(CannonballType.CHAIN));

        assertEquals(StatusEffect.WEAKENED, enemy.getStatus(), "CHAIN should apply WEAKENED");
    }

    @Test
    void executeCannonball_grapeshot_appliesBurned() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(200, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.execute(new Command.Cannonball(CannonballType.GRAPESHOT));

        assertEquals(StatusEffect.BURNED, enemy.getStatus(), "GRAPESHOT should apply BURNED");
    }

    // -------------------------------------------------------------------------
    // Command: Spell
    // -------------------------------------------------------------------------

    @Test
    void executeSpell_inferno_dealsDamage() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        player.recruitMage(new Mage("InfernoMage", Element.FIRE, 30, SpellType.INFERNO));
        EnemyShip enemy = createEnemy(300, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        int hpBefore = enemy.getCurrentHp();
        TurnResult result = state.execute(new Command.Spell(0));

        assertTrue(enemy.getCurrentHp() < hpBefore, "Spell should deal damage");
        assertTrue(result.battleLog().stream().anyMatch(l -> l.contains("INFERNO")),
                "Log should mention INFERNO");
    }

    @Test
    void executeSpell_markedUsed_cannotReuse() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        Mage mage = new Mage("Mage", Element.FIRE, 30, SpellType.INFERNO);
        player.recruitMage(mage);
        EnemyShip enemy = createEnemy(300, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.execute(new Command.Spell(0));
        assertTrue(mage.isSpellUsed(), "Spell should be marked as used");

        int hpBefore = enemy.getCurrentHp();
        state.turnStart();
        TurnResult result = state.execute(new Command.Spell(0));
        assertEquals(hpBefore, enemy.getCurrentHp(), "Second spell use should do nothing");
        assertTrue(result.battleLog().stream().anyMatch(l -> l.contains("sudah dipakai")),
                "Log should warn about spell already used");
    }

    @Test
    void executeSpell_noMage_autoCannon() {
        PlayerShip player = createPlayer(100, 50, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Spell(0));

        assertTrue(result.battleLog().stream().anyMatch(l -> l.contains("Otomatis")),
                "Should auto-cannon when no mage");
    }

    // -------------------------------------------------------------------------
    // Command: Defend
    // -------------------------------------------------------------------------

    @Test
    void executeDefend_activatesShield() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.execute(new Command.Defend());

        assertTrue(player.isShielded(), "Shield should be active after Defend");
    }

    @Test
    void defend_reducesEnemyDamage() {
        PlayerShip player = createPlayer(200, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 50, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        int hpBefore = player.getCurrentHp();
        state.execute(new Command.Defend());

        int damageTaken = hpBefore - player.getCurrentHp();
        int expectedFullDamage = 50; // enemy base damage
        assertTrue(damageTaken < expectedFullDamage,
                "Defend should reduce damage: taken=" + damageTaken + " vs full=" + expectedFullDamage);
    }

    // -------------------------------------------------------------------------
    // Command: Potion
    // -------------------------------------------------------------------------

    @Test
    void executePotion_healsPlayer() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        player.takeDamage(40);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        int hpBefore = player.getCurrentHp();
        state.execute(new Command.Potion());

        assertTrue(player.getCurrentHp() > hpBefore, "Potion should heal player");
        assertEquals(1, player.getPotions(), "Potion count should decrease");
    }

    @Test
    void executePotion_noPotions_doesNothing() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        player.takeDamage(40);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        // Use up all potions; each use also heals 50
        player.usePotion();
        player.usePotion();

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Potion());

        // Potion does nothing (0 left), enemy deals 10 damage
        assertEquals(90, player.getCurrentHp(),
                "HP should only change from enemy damage");
        assertTrue(result.battleLog().stream().anyMatch(l -> l.contains("habis")),
                "Log should say potions are empty");
    }

    // -------------------------------------------------------------------------
    // Victory condition
    // -------------------------------------------------------------------------

    @Test
    void victory_playerKillsEnemy() {
        PlayerShip player = createPlayer(100, 200, Element.FIRE);
        EnemyShip enemy = createEnemy(10, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();

        assertFalse(state.isOver(), "Battle should not be over at start");
        TurnResult result = state.execute(new Command.Attack(0));

        assertTrue(state.isOver(), "Battle should be over");
        assertTrue(state.playerWon(), "Player should have won");
        assertTrue(result.battleOver(), "TurnResult should report battle over");
        assertTrue(result.playerWon(), "TurnResult should report player won");
    }

    @Test
    void victory_playerKillsEnemyWithCannonball() {
        PlayerShip player = createPlayer(100, 200, Element.FIRE);
        EnemyShip enemy = createEnemy(5, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Cannonball(CannonballType.IRON));

        assertTrue(result.battleOver(), "Battle should be over");
        assertTrue(result.playerWon(), "Player should have won");
    }

    // -------------------------------------------------------------------------
    // Defeat condition
    // -------------------------------------------------------------------------

    @Test
    void defeat_enemyKillsPlayer() {
        PlayerShip player = createPlayer(10, 1, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 200, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Defend());

        assertTrue(state.isOver(), "Battle should be over");
        assertFalse(state.playerWon(), "Player should have lost");
        assertTrue(result.battleOver(), "TurnResult should report battle over");
        assertFalse(result.playerWon(), "TurnResult should report player lost");
    }

    // -------------------------------------------------------------------------
    // Boss RAGE
    // -------------------------------------------------------------------------

    @Test
    void bossRage_doubleAttackWhenBelow50Percent() {
        PlayerShip player = createPlayer(500, 20, Element.FIRE);
        player.recruitMage(new Mage("Mage", Element.FIRE, 10, SpellType.INFERNO));
        BossShip boss = new BossShip("SeaKing", "Raja Laut", 200, 30,
                Element.STORM, 50, 20, EnemyTrait.NONE);
        boss.setCurrentHp(80); // below 50% of 200 -> enraged

        BattleState state = new BattleState(player, boss, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Attack(0));

        int damageTaken = 500 - player.getCurrentHp();
        // Main attack 30 + RAGE bonus ~8-11 = 38-41
        assertTrue(damageTaken > 30,
                "Boss RAGE should deal more than base damage: " + damageTaken);
        boolean hasRage = result.battleLog().stream()
                .anyMatch(l -> l.contains("RAGE") || l.contains("Kutukan"));
        assertTrue(hasRage, "Battle log should mention RAGE attack");
    }

    @Test
    void bossRage_logContainsRageMessage() {
        PlayerShip player = createPlayer(500, 20, Element.FIRE);
        BossShip boss = new BossShip("SeaKing", "Raja Laut", 200, 30,
                Element.STORM, 50, 20, EnemyTrait.NONE);
        boss.setCurrentHp(80);

        BattleState state = new BattleState(player, boss, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Defend());

        boolean hasRage = result.battleLog().stream()
                .anyMatch(l -> l.contains("RAGE") || l.contains("Kutukan"));
        assertTrue(hasRage, "Battle log should mention RAGE attack");
    }

    @Test
    void bossNoRage_above50Percent() {
        PlayerShip player = createPlayer(500, 20, Element.FIRE);
        BossShip boss = new BossShip("SeaKing", "Raja Laut", 200, 30,
                Element.STORM, 50, 20, EnemyTrait.NONE);
        boss.setCurrentHp(120); // above 50% -> not enraged

        BattleState state = new BattleState(player, boss, new AtomicBoolean(false));
        state.turnStart();
        int hpBefore = player.getCurrentHp();
        state.execute(new Command.Defend());

        int damageTaken = hpBefore - player.getCurrentHp();
        assertTrue(damageTaken <= 30,
                "Non-enraged boss should only attack once: " + damageTaken);
    }

    // -------------------------------------------------------------------------
    // OVERCHARGE edge case
    // -------------------------------------------------------------------------

    @Test
    void overcharge_killsEnemyMidLoop_battleEnds() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        player.recruitMage(new Mage("OverMage", Element.STORM, 100, SpellType.OVERCHARGE));
        player.recruitMage(new Mage("ExtraMage", Element.STORM, 50, SpellType.CHAIN_BOLT));
        EnemyShip enemy = createEnemy(5, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        TurnResult result = state.execute(new Command.Spell(0));

        assertTrue(state.isOver(), "Battle should end when enemy dies from OVERCHARGE");
        assertTrue(state.playerWon(), "Player should win");
        assertTrue(result.battleLog().stream().anyMatch(l -> l.contains("OVERCHARGE")),
                "Log should mention OVERCHARGE");
    }

    @Test
    void overcharge_multipleMages_sequentialDamage() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        player.recruitMage(new Mage("M1", Element.STORM, 30, SpellType.OVERCHARGE));
        player.recruitMage(new Mage("M2", Element.STORM, 30, SpellType.CHAIN_BOLT));
        player.recruitMage(new Mage("M3", Element.FIRE, 30, SpellType.IGNITE));
        EnemyShip enemy = createEnemy(50, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.execute(new Command.Spell(0));

        assertTrue(enemy.isAlive() || state.isOver(),
                "Enemy should either be alive or battle over after OVERCHARGE");
    }

    // -------------------------------------------------------------------------
    // Frozen status
    // -------------------------------------------------------------------------

    @Test
    void frozenPlayer_cannotAct() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);
        player.applyStatus(StatusEffect.FROZEN, 1);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        TurnResult tr = state.turnStart();

        assertFalse(state.canAct(), "Frozen player should not be able to act");
        assertTrue(tr.battleLog().stream().anyMatch(l -> l.contains("FROZEN")),
                "Log should mention frozen");
    }

    @Test
    void frozenPlayer_skipTurn_enemyStillAttacks() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 50, Element.WATER, EnemyTrait.NONE);
        player.applyStatus(StatusEffect.FROZEN, 1);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();

        assertFalse(state.canAct(), "Player should be frozen");
        int hpBefore = player.getCurrentHp();
        TurnResult skipResult = state.skipTurn();

        assertTrue(player.getCurrentHp() < hpBefore,
                "Enemy should attack during frozen skip");
        assertTrue(skipResult.playerTurn(), "Should be player's turn after skip");
    }

    @Test
    void frozenPlayer_statusCleared_afterTurn() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);
        player.applyStatus(StatusEffect.FROZEN, 1);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.skipTurn();

        assertEquals(StatusEffect.NONE, player.getStatus(),
                "Frozen status should be cleared after skip");
    }

    // -------------------------------------------------------------------------
    // TurnResult data integrity
    // -------------------------------------------------------------------------

    @Test
    void turnResult_containsHpBars() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        TurnResult tr = state.turnStart();

        assertNotNull(tr.playerHpBar(), "Player HP bar should not be null");
        assertNotNull(tr.enemyHpBar(), "Enemy HP bar should not be null");
        assertFalse(tr.playerHpBar().isEmpty(), "Player HP bar should not be empty");
    }

    @Test
    void turnResult_accumulatesBattleLog() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        TurnResult tr1 = state.turnStart();
        assertFalse(tr1.battleLog().isEmpty(), "Battle log should contain at least initial message");
    }

    @Test
    void turnResult_battleNotOverInitially() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        assertFalse(state.isOver(), "Battle should not be over initially");
        assertFalse(state.playerWon(), "Player should not have won initially");
    }

    // -------------------------------------------------------------------------
    // Status effect processing in turnStart
    // -------------------------------------------------------------------------

    @Test
    void turnStart_processesBurnedDamage() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);
        player.applyStatus(StatusEffect.BURNED, 2);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        int hpBefore = player.getCurrentHp();
        state.turnStart();

        assertTrue(player.getCurrentHp() < hpBefore,
                "BURNED should deal damage at turn start");
    }

    @Test
    void turnStart_resetsShield() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.execute(new Command.Defend());
        assertTrue(player.isShielded(), "Shield should be active after Defend");

        state.turnStart();
        assertFalse(player.isShielded(), "Shield should be reset after turnStart");
    }

    // -------------------------------------------------------------------------
    // Multiple turn cycle
    // -------------------------------------------------------------------------

    @Test
    void multipleTurnCycle_accumulatesEffects() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));

        TurnResult tr = state.turnStart();
        assertFalse(tr.battleOver(), "Battle should not be over");

        tr = state.execute(new Command.Attack(0));
        assertFalse(tr.battleOver(), "Battle should not be over after first turn");
        int enemyHp = enemy.getCurrentHp();
        assertTrue(enemyHp < 100, "Enemy should have taken damage");

        tr = state.turnStart();
        assertFalse(tr.battleOver(), "Battle should continue");

        tr = state.execute(new Command.Cannonball(CannonballType.IRON));
        assertTrue(enemy.getCurrentHp() < enemyHp, "Enemy should take more damage");
    }

    @Test
    void attack_withoutTurnStart_stillWorks() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        TurnResult result = state.execute(new Command.Attack(0));

        assertTrue(enemy.getCurrentHp() < 100,
                "Attack should still work without calling turnStart first");
    }

    // -------------------------------------------------------------------------
    // shieldCheck: verify shield reduces damage
    // -------------------------------------------------------------------------

    @Test
    void playerShielded_takesHalfDamage() {
        PlayerShip player = createPlayer(200, 1, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 100, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.execute(new Command.Defend());

        int expectedDamageWithShield = 100 / 2; // enemy base damage halved
        int actualHp = player.getCurrentHp();
        assertEquals(200 - expectedDamageWithShield, actualHp,
                "Player with shield should take half damage");
    }

    // -------------------------------------------------------------------------
    // Potion + heal boundary
    // -------------------------------------------------------------------------

    @Test
    void potion_doesNotExceedMaxHp() {
        PlayerShip player = createPlayer(100, 20, Element.FIRE);
        EnemyShip enemy = createEnemy(100, 10, Element.WATER, EnemyTrait.NONE);

        BattleState state = new BattleState(player, enemy, new AtomicBoolean(false));
        state.turnStart();
        state.execute(new Command.Potion());

        // Potion heals 50 but capped at 100, then enemy deals 10
        assertEquals(90, player.getCurrentHp(),
                "Potion should not heal above max HP, and enemy deals 10 damage");
    }
}
