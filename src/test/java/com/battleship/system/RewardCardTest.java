package com.battleship.system;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import com.battleship.model.Mage;
import com.battleship.model.PlayerShip;
import com.battleship.model.enums.CannonballType;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.SpellType;
import com.battleship.system.RewardCard.Type;

class RewardCardTest {

    private PlayerShip makePlayer() {
        return new PlayerShip("TestKapal", 100, 20, Element.WATER);
    }

    private RewardCard card(Type type) {
        // Build a card of the given type from the pool
        return RewardCard.buildPool().stream()
                .filter(c -> c.getType() == type)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Card type not in pool: " + type));
    }

    @Test
    void healSmall_restores45Hp() {
        PlayerShip p = makePlayer();
        p.takeDamage(60);
        int before = p.getCurrentHp();
        card(Type.HEAL_SMALL).apply(p);
        assertEquals(before + 45, p.getCurrentHp());
    }

    @Test
    void healSmall_capsAtMaxHp() {
        PlayerShip p = makePlayer();
        p.takeDamage(10);
        card(Type.HEAL_SMALL).apply(p);
        assertEquals(p.getMaxHp(), p.getCurrentHp());
    }

    @Test
    void healLarge_restores80Hp() {
        PlayerShip p = makePlayer();
        p.takeDamage(100);
        card(Type.HEAL_LARGE).apply(p);
        assertEquals(80, p.getCurrentHp());
    }

    @Test
    void upgradeCannon_adds7BaseDamage() {
        PlayerShip p = makePlayer();
        int before = p.getBaseDamage();
        card(Type.UPGRADE_CANNON).apply(p);
        assertEquals(before + 7, p.getBaseDamage());
    }

    @Test
    void recruitMage_addsMageToRoster() {
        PlayerShip p = makePlayer();
        int before = p.getMageCount();
        card(Type.RECRUIT_MAGE).apply(p);
        assertEquals(before + 1, p.getMageCount());
    }

    @Test
    void recruitMage_whenRosterFull_heals45() {
        PlayerShip p = makePlayer();
        // Fill roster to max (5)
        for (int i = 0; i < 5; i++) {
            Mage m = new Mage("Test" + i, Element.FIRE, 10, SpellType.INFERNO);
            assertTrue(p.recruitMage(m), "recruit should succeed");
        }
        p.takeDamage(50);
        int hpBefore = p.getCurrentHp();
        card(Type.RECRUIT_MAGE).apply(p);
        assertEquals(5, p.getMageCount()); // no new mage
        assertEquals(hpBefore + 45, p.getCurrentHp());
    }

    @Test
    void addPotion_incrementsPotionCount() {
        PlayerShip p = makePlayer();
        int before = p.getPotions();
        card(Type.ADD_POTION).apply(p);
        assertEquals(before + 1, p.getPotions());
    }

    @Test
    void addPotion_whenFull_heals45() {
        PlayerShip p = makePlayer();
        // Use existing potions then refill to max
        while (p.getPotions() < 3) {
            p.addPotion();
        }
        assertEquals(3, p.getPotions());
        p.takeDamage(50);
        int hpBefore = p.getCurrentHp();
        card(Type.ADD_POTION).apply(p);
        assertEquals(3, p.getPotions()); // no extra potion
        assertEquals(hpBefore + 45, p.getCurrentHp());
    }

    @Test
    void addExplosive_gives3Ammo() {
        PlayerShip p = makePlayer();
        int before = p.getAmmoCount(CannonballType.EXPLOSIVE);
        card(Type.ADD_EXPLOSIVE).apply(p);
        assertEquals(before + 3, p.getAmmoCount(CannonballType.EXPLOSIVE));
    }

    @Test
    void addChain_gives3Ammo() {
        PlayerShip p = makePlayer();
        int before = p.getAmmoCount(CannonballType.CHAIN);
        card(Type.ADD_CHAIN).apply(p);
        assertEquals(before + 3, p.getAmmoCount(CannonballType.CHAIN));
    }

    @Test
    void addGrapeshot_gives3Ammo() {
        PlayerShip p = makePlayer();
        int before = p.getAmmoCount(CannonballType.GRAPESHOT);
        card(Type.ADD_GRAPESHOT).apply(p);
        assertEquals(before + 3, p.getAmmoCount(CannonballType.GRAPESHOT));
    }

    @Test
    void upgradeAllMageSmall_boostsAllMages() {
        PlayerShip p = makePlayer();
        Mage m1 = new Mage("Ignis", Element.FIRE, 10, SpellType.INFERNO);
        Mage m2 = new Mage("Aquara", Element.WATER, 15, SpellType.FREEZE);
        p.recruitMage(m1);
        p.recruitMage(m2);
        card(Type.UPGRADE_ALL_MAGE_SMALL).apply(p);
        assertEquals(15, m1.getMagicPower());
        assertEquals(20, m2.getMagicPower());
    }

    @Test
    void doubleCannonDmg_returnsMessage() {
        PlayerShip p = makePlayer();
        List<String> result = card(Type.DOUBLE_CANNON_DMG).apply(p);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Kapal Berkibar"));
    }

    @Test
    void upgradeMagePower_fallbackWhenRosterEmpty() {
        PlayerShip p = makePlayer();
        p.takeDamage(50);
        int hpBefore = p.getCurrentHp();
        card(Type.UPGRADE_MAGE_POWER).apply(p);
        assertEquals(hpBefore + 45, p.getCurrentHp());
    }

    @Test
    void applyMageUpgrade_boostsSpecificMage() {
        PlayerShip p = makePlayer();
        Mage m = new Mage("Voltus", Element.STORM, 20, SpellType.CHAIN_BOLT);
        p.recruitMage(m);
        int before = m.getMagicPower();
        card(Type.UPGRADE_MAGE_POWER).applyMageUpgrade(p, 0);
        assertEquals(before + 18, m.getMagicPower());
    }

    @Test
    void drawThree_returnsThreeCards() {
        PlayerShip p = makePlayer();
        List<RewardCard> drawn = RewardCard.drawThree(new Random(42), p);
        assertEquals(3, drawn.size());
    }

    @Test
    void drawThree_cardsAreUnique() {
        PlayerShip p = makePlayer();
        List<RewardCard> drawn = RewardCard.drawThree(new Random(99), p);
        assertEquals(3, drawn.stream().map(RewardCard::getType).distinct().count());
    }

    @Test
    void buildPool_containsAllTypes() {
        List<RewardCard> pool = RewardCard.buildPool();
        for (Type t : Type.values()) {
            assertTrue(pool.stream().anyMatch(c -> c.getType() == t),
                    "Pool should contain " + t);
        }
    }
}
