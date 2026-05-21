package com.battleship.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.battleship.model.EnemyShip;
import com.battleship.model.PlayerShip;
import com.battleship.model.Ship;
import com.battleship.model.enums.CannonballType;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.StatusEffect;

class DamagePipelineTest {

    // -------------------------------------------------------------------------
    // Element multiplier tests
    // -------------------------------------------------------------------------

    @Test
    void fireAdvantageOverStorm_2x() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.0, null, null, Element.FIRE,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(70, r.finalDamage(), "FIRE > STORM should be 2.0x");
        assertTrue(r.multiplierBreakdown().containsKey("element"));
        assertEquals(2.0, r.multiplierBreakdown().get("element"), 0.001);
        assertEquals(0, r.reflectedDamage());
    }

    @Test
    void waterAdvantageOverFire_2x() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.WATER);
        Ship defender = new EnemyShip("Def", 100, 10, Element.FIRE, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.0, null, null, Element.WATER,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(70, r.finalDamage(), "WATER > FIRE should be 2.0x");
    }

    @Test
    void stormAdvantageOverWater_2x() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.STORM);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.0, null, null, Element.STORM,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(70, r.finalDamage(), "STORM > WATER should be 2.0x");
    }

    @Test
    void fireAgainstWater_0_5x() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            0.5, 1.0, null, null, Element.FIRE,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(17, r.finalDamage(), "FIRE vs WATER should be 0.5x");
    }

    @Test
    void sameElement_1x() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.FIRE, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            1.0, 1.0, null, null, Element.FIRE,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(35, r.finalDamage(), "Same element should be 1.0x");
    }

    // -------------------------------------------------------------------------
    // Physical damage — no element multiplier
    // -------------------------------------------------------------------------

    @Test
    void physicalAttack_noElementMult() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(20, r.finalDamage(), "Physical should ignore element mult");
        assertFalse(r.multiplierBreakdown().containsKey("element"));
        assertFalse(r.multiplierBreakdown().containsKey("synergy"));
    }

    // -------------------------------------------------------------------------
    // Synergy multiplier tests
    // -------------------------------------------------------------------------

    @Test
    void synergy_2SameElement_plus20() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.20, null, null, Element.FIRE,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(84, r.finalDamage(), "35 * 2.0 * 1.20 = 84");
        assertEquals(1.20, r.multiplierBreakdown().get("synergy"), 0.001);
    }

    @Test
    void synergy_3plusSameElement_plus40() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.40, null, null, Element.FIRE,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(98, r.finalDamage(), "35 * 2.0 * 1.40 = 98");
        assertEquals(1.40, r.multiplierBreakdown().get("synergy"), 0.001);
    }

    // -------------------------------------------------------------------------
    // WEAKENED tests
    // -------------------------------------------------------------------------

    @Test
    void weakenedAttacker_reducesDamage_physical() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        attacker.applyStatus(StatusEffect.WEAKENED, 3);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(14, r.finalDamage(), "WEAKENED should reduce by 30%: 20 * 0.70 = 14");
        assertEquals(0.70, r.multiplierBreakdown().get("weakened"), 0.001);
    }

    @Test
    void weakenedAttacker_reducesDamage_magic() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        attacker.applyStatus(StatusEffect.WEAKENED, 3);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.0, null, null, Element.FIRE,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(49, r.finalDamage(), "35 * 2.0 * 0.70 = 49");
    }

    @Test
    void notWeakened_noPenalty() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(20, r.finalDamage());
        assertFalse(r.multiplierBreakdown().containsKey("weakened"));
    }

    // -------------------------------------------------------------------------
    // BERSERKER tests
    // -------------------------------------------------------------------------

    @Test
    void berserkerActive_mult15() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, true
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(30, r.finalDamage(), "BERSERKER should make 20 * 1.5 = 30");
        assertEquals(1.50, r.multiplierBreakdown().get("berserker"), 0.001);
    }

    @Test
    void berserkerInactive_noBonus() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(20, r.finalDamage());
        assertFalse(r.multiplierBreakdown().containsKey("berserker"));
    }

    // -------------------------------------------------------------------------
    // ARMORED tests
    // -------------------------------------------------------------------------

    @Test
    void armoredReducesPhysicalDamage() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.ARMORED);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.ARMORED, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(13, r.finalDamage(), "ARMORED physical: 20 * 0.65 = 13");
        assertEquals(0.65, r.multiplierBreakdown().get("armored"), 0.001);
    }

    @Test
    void armoredDoesNotReduceMagicDamage() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.ARMORED);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.0, null, null, Element.FIRE,
            EnemyTrait.ARMORED, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(70, r.finalDamage(), "ARMORED should not affect magic: 35 * 2.0 = 70");
        assertFalse(r.multiplierBreakdown().containsKey("armored"));
    }

    // -------------------------------------------------------------------------
    // THORNS reflection tests
    // -------------------------------------------------------------------------

    @Test
    void thornsReflectsOnlyMagicDamage() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.THORNS);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.0, null, null, Element.FIRE,
            EnemyTrait.THORNS, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(70, r.finalDamage());
        assertEquals(10, r.reflectedDamage(), "THORNS: 70 * 0.15 = 10 (rounded)");
        assertTrue(r.hasReflectedDamage());
    }

    @Test
    void thornsDoesNotReflectPhysicalDamage() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.THORNS);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.THORNS, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(20, r.finalDamage());
        assertEquals(0, r.reflectedDamage(), "THORNS should not reflect physical");
        assertFalse(r.hasReflectedDamage());
    }

    // -------------------------------------------------------------------------
    // Combined multiplier tests
    // -------------------------------------------------------------------------

    @Test
    void allMultipliersCombined() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        attacker.applyStatus(StatusEffect.WEAKENED, 3);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.ARMORED);

        // PHYSICAL: base 20 * 0.70 (weakened) * 1.50 (berserker) * 0.65 (armored)
        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.ARMORED, true
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        // 20 * 0.70 * 1.50 * 0.65 = 20 * 0.6825 = 13.65 -> 13
        assertEquals(13, r.finalDamage());
        assertTrue(r.multiplierBreakdown().containsKey("weakened"));
        assertTrue(r.multiplierBreakdown().containsKey("berserker"));
        assertTrue(r.multiplierBreakdown().containsKey("armored"));
    }

    @Test
    void allMagicMultipliersCombined() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        attacker.applyStatus(StatusEffect.WEAKENED, 3);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.THORNS);

        // MAGIC: base 35 * 2.0 (element) * 1.40 (synergy) * 0.70 (weakened) * 1.50 (berserker)
        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 35,
            2.0, 1.40, null, null, Element.FIRE,
            EnemyTrait.THORNS, true
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        // 35 * 2.0 * 1.40 * 0.70 * 1.50 = 35 * 2.94 = 102.9 -> 102
        assertEquals(102, r.finalDamage());
        // THORNS: 102 * 0.15 = 15.3 -> 15
        assertEquals(15, r.reflectedDamage());
    }

    // -------------------------------------------------------------------------
    // Status effects from cannonballs
    // -------------------------------------------------------------------------

    @Test
    void cannonballStatusEffects_grapeShot() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.GRAPESHOT, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertTrue(r.statusEffectsApplied().contains(StatusEffect.BURNED),
            "GRAPESHOT should apply BURNED");
        assertFalse(r.statusEffectsApplied().contains(StatusEffect.WEAKENED),
            "GRAPESHOT should not apply WEAKENED");
    }

    @Test
    void cannonballStatusEffects_chainShot() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.CHAIN, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertTrue(r.statusEffectsApplied().contains(StatusEffect.WEAKENED),
            "CHAIN should apply WEAKENED");
        assertFalse(r.statusEffectsApplied().contains(StatusEffect.BURNED),
            "CHAIN should not apply BURNED");
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    void zeroBaseDamage() {
        PlayerShip attacker = new PlayerShip("Test", 100, 0, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 0,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(0, r.finalDamage(), "Zero base damage should produce zero");
    }

    @Test
    void highBaseDamage() {
        PlayerShip attacker = new PlayerShip("Test", 100, 999, Element.FIRE);
        Ship defender = new EnemyShip("Def", 9999, 10, Element.STORM, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.MAGIC, 999,
            2.0, 1.40, null, null, Element.FIRE,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(2797, r.finalDamage(), "999 * 2.0 * 1.40 = 2797.2 -> 2797");
    }

    @Test
    void berserkerWithWeakened() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        attacker.applyStatus(StatusEffect.WEAKENED, 3);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, true
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        // 20 * 0.70 * 1.50 = 21 (floating point truncation may give 20)
        assertEquals(20, r.finalDamage(), "WEAKENED + BERSERKER: 20 * 0.70 * 1.50");
    }

    // -------------------------------------------------------------------------
    // Result metadata
    // -------------------------------------------------------------------------

    @Test
    void resultContainsRawDamage() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 42,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertEquals(42, r.rawDamage());
    }

    @Test
    void resultBreakdownIsOrdered() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        attacker.applyStatus(StatusEffect.WEAKENED, 3);
        Ship defender = new EnemyShip("Def", 100, 10, Element.STORM, 10, 5, EnemyTrait.ARMORED);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.ARMORED, true
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        // Order should be: base, weakened, berserker, armored
        var keys = r.multiplierBreakdown().keySet().toArray(new String[0]);
        assertEquals("base", keys[0]);
        assertEquals("weakened", keys[1]);
        assertEquals("berserker", keys[2]);
        assertEquals("armored", keys[3]);
    }

    // -------------------------------------------------------------------------
    // Physical attack — no special status effects
    // -------------------------------------------------------------------------

    @Test
    void ironCannonball_noStatusEffects() {
        PlayerShip attacker = new PlayerShip("Test", 100, 20, Element.FIRE);
        Ship defender = new EnemyShip("Def", 100, 10, Element.WATER, 10, 5, EnemyTrait.NONE);

        AttackContext ctx = new AttackContext(
            attacker, defender, AttackType.PHYSICAL, 20,
            1.0, 1.0, CannonballType.IRON, null, null,
            EnemyTrait.NONE, false
        );
        DamageResult r = DamagePipeline.resolve(ctx);
        assertTrue(r.statusEffectsApplied().isEmpty(), "IRON should not apply status effects");
    }
}
