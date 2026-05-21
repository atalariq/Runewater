package com.battleship.system;

import java.util.Random;

import com.battleship.model.BossShip;
import com.battleship.model.EnemyShip;
import com.battleship.model.Mage;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.EnemyVariant;
import com.battleship.util.BalanceConfig;

public class StageFactory {

    private static final String[] ENEMY_NAMES = {
            "Galleon Merah", "Fregat Vortex", "Brigantine Badai", "Junk Hantu",
            "Karakoa Iblis", "Schooner Kutukan", "Barque Neraka", "Dhow Kegelapan",
            "Frigate Bayangan", "Brig Kutukan"
    };

    private static final String[] BOSS_TITLES = {
            "Raja Tua Laut", "Sang Leviathan", "Dewa Badai", "Si Mata Satu",
            "Laksamana Kegelapan", "Naga Samudra", "Hantu Laut Dalam"
    };

    private static final String[] MAGE_NAMES = {
            "Ignis", "Aquara", "Voltus", "Pyra", "Hydra", "Tempest",
            "Ember", "Cascade", "Thunder", "Inferno", "Torrent", "Zephyr",
            "Cinder", "Deluge", "Gale", "Blaze", "Surge", "Frost", "Ash", "Mist"
    };

    private final Random rng;

    public StageFactory(Random rng) {
        this.rng = rng;
    }

    public EnemyShip generateEnemy(int stage, EnemyVariant variant, Element excludeElement) {
        int baseHp = BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE;
        int baseDmg = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
        int baseBounty = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE;
        int baseXp = BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE;

        int hp = (int) (baseHp * variant.getHpMultiplier());
        int damage = (int) (baseDmg * variant.getDamageMultiplier());
        int bounty = switch (variant) {
            case NORMAL -> baseBounty;
            case DIFFERENT_ELEMENT -> (int) (baseBounty * 1.30);
            case ELITE -> (int) (baseBounty * 1.80);
        };
        int xp = switch (variant) {
            case NORMAL -> baseXp;
            case DIFFERENT_ELEMENT -> (int) (baseXp * 1.15);
            case ELITE -> (int) (baseXp * 1.50);
        };

        String name = switch (variant) {
            case NORMAL, DIFFERENT_ELEMENT -> randomEnemyName();
            case ELITE -> randomEnemyName() + " [ELITE]";
        };

        Element element;
        if (variant == EnemyVariant.DIFFERENT_ELEMENT) {
            do {
                element = randomElement();
            } while (element == excludeElement);
        } else {
            element = randomElement();
        }

        return new EnemyShip(name, hp, damage, element, bounty, xp, randomTrait());
    }

    public BossShip generateBoss(int stage) {
        int baseHp = BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE;
        int baseDmg = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
        int hp = (int) (baseHp * BalanceConfig.BOSS_HP_MULT);
        int damage = (int) (baseDmg * BalanceConfig.BOSS_DMG_MULT);
        int bounty = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE + BalanceConfig.BOSS_BOUNTY_BON;
        int xp = (BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE) * 2;

        return new BossShip(
                randomEnemyName() + " [BOSS]",
                randomBossTitle(),
                hp, damage, randomElement(), bounty, xp, randomTrait()
        );
    }

    public Mage generateMage(int basePower) {
        return new Mage(randomMageName(), randomElement(), basePower, rng);
    }

    public String randomMageName() {
        return MAGE_NAMES[rng.nextInt(MAGE_NAMES.length)];
    }

    private String randomEnemyName() {
        return ENEMY_NAMES[rng.nextInt(ENEMY_NAMES.length)];
    }

    private String randomBossTitle() {
        return BOSS_TITLES[rng.nextInt(BOSS_TITLES.length)];
    }

    private Element randomElement() {
        return Element.values()[rng.nextInt(Element.values().length)];
    }

    private EnemyTrait randomTrait() {
        EnemyTrait[] traits = EnemyTrait.values();
        int roll = rng.nextInt(traits.length + traits.length - 1);
        return roll < traits.length ? traits[roll] : EnemyTrait.NONE;
    }
}
