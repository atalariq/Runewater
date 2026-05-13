package com.battleship.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import com.battleship.engine.BattleEngine;
import com.battleship.model.BossShip;
import com.battleship.model.EnemyShip;
import com.battleship.model.Mage;
import com.battleship.model.PlayerShip;
import com.battleship.model.Ship;
import com.battleship.model.enums.CannonballType;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.SpellType;
import com.battleship.util.BalanceConfig;
import com.battleship.util.InputHelper;
import com.battleship.util.TerminalUi;

/**
 * Orchestrates the whole game while rendering through TerminalUi.
 */
public class GameManager {

    private PlayerShip player;
    private final InputHelper input;
    private final TerminalUi ui;
    private final Random rng = new Random();
    private int stage = 1;
    private final AtomicBoolean cannonDoubled = new AtomicBoolean(false);
    private BattleEngine battleEngine;

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

    public GameManager() {
        this.input = new InputHelper(new Scanner(System.in));
        this.ui = new TerminalUi(input);
    }

    public void start() {
        try {
            showTitle();
            showHelp();
            initPlayer();
            gameLoop();
        } finally {
            ui.shutdown();
        }
    }

    private void showTitle() {
        ui.pause("Runewater", List.of(
                "Petualangan taktikal lautan magis kini tampil seperti duel terminal klasik.",
                "",
                "Arahkan pilihan dengan panah keyboard, tekan Enter untuk mengunci komando,",
                "dan pimpin kru penyihir Anda menembus armada musuh.",
                "",
                "Selamat datang di Runewater."
        ));
    }

    private void showHelp() {
        ui.pause("Panduan", List.of(
                "Elemen: FIRE > STORM > WATER > FIRE",
                "",
                "Aksi battle:",
                "[1] Tembak Meriam  - pilih tipe peluru sesuai situasi",
                "[2] Sihir Mage     - damage elemen + bonus sinergy",
                "[3] Gunakan Jurus  - efek spesial, 1x per battle",
                "[4] Bertahan       - potong damage masuk 50% turn ini",
                "[5] Minum Potion   - heal 50 HP",
                "",
                "Sinergy:",
                "2 Mage elemen sama  -> +20% magic damage",
                "3 Mage elemen sama  -> +40% magic damage",
                "",
                "Navigasi menu memakai Arrow Keys atau angka.",
                "Setelah menang, pilih 1 dari 3 reward."
        ));
    }

    private void initPlayer() {
        String captainName = ui.promptText(
                "Pendaftaran Kapten",
                List.of(
                        "Selamat datang, kapten.",
                        "Masukkan nama kapal Anda. Kosongkan untuk nama default."
                ),
                "Nama Kapten:");

        if (captainName.isBlank()) {
            captainName = "Nusantara";
        }

        player = new PlayerShip(
                "Kapal " + captainName,
                BalanceConfig.PLAYER_HP,
                BalanceConfig.PLAYER_DAMAGE,
                Element.WATER
        );

        List<String> starterLines = new ArrayList<>();
        starterLines.add("Pilih Mage pertama dan jurus pembuka:");
        starterLines.add("[1] FIRE  + INFERNO    (Damage x3.0, sekali pakai)");
        starterLines.add("[2] FIRE  + IGNITE     (Damage + BURNED 3 turn)");
        starterLines.add("[3] WATER + TIDAL WAVE (Damage x2.0 + Heal 35 HP)");
        starterLines.add("[4] WATER + FREEZE     (Damage + FROZEN)");
        starterLines.add("[5] STORM + CHAIN BOLT (Damage x2.5 + WEAKENED)");
        starterLines.add("[6] STORM + OVERCHARGE (Semua Mage menyerang)");

        int choice = ui.promptChoice("Pendaftaran Kapten", starterLines, 1, 6, 1, "Pilihan awal (1-6):");

        SpellType[] spells = {
                SpellType.INFERNO, SpellType.IGNITE,
                SpellType.TIDAL_WAVE, SpellType.FREEZE,
                SpellType.CHAIN_BOLT, SpellType.OVERCHARGE
        };
        Element[] elements = {
                Element.FIRE, Element.FIRE,
                Element.WATER, Element.WATER,
                Element.STORM, Element.STORM
        };

        Mage startingMage = new Mage(
                randomMageName(),
                elements[choice - 1],
                BalanceConfig.START_MAGE_PWR,
                spells[choice - 1]
        );
        player.recruitMage(startingMage);

        ui.pause("Kru Awal", List.of(
                "Kapal: " + player.getName(),
                "Mage  : " + startingMage.getInfo(true),
                "Jurus : " + startingMage.getSpellType().getFullDescription(),
                "Peluru: Iron tak terbatas, Explosive x3, Chain x3, Grapeshot x3",
                "Potion: 2",
                "",
                "Armada Runewater siap berlayar."
        ));

        battleEngine = new BattleEngine(player, ui, rng, cannonDoubled);
    }

    private void gameLoop() {
        while (player.isAlive()) {
            cannonDoubled.set(false);
            boolean isBoss = (stage % 5 == 0);
            Ship currentEnemy;

            if (isBoss) {
                currentEnemy = generateBoss(stage);
                ui.pause(stageTitle(), buildBossIntro(currentEnemy));
            } else {
                currentEnemy = offerEnemyChoice(stage);
            }

            ui.pause(stageTitle(), buildPreBattleInfo(currentEnemy));
            player.resetAllSpells();

            boolean playerWon = battleEngine.runBattle(currentEnemy);
            if (!playerWon) {
                printGameOver();
                return;
            }

            EnemyShip defeatedEnemy = (EnemyShip) currentEnemy;
            player.addBounty(defeatedEnemy.getBountyReward());
            List<String> levelUps = player.grantXpToAll(defeatedEnemy.getXpReward());
            printVictory(defeatedEnemy, levelUps);
            handleReward();
            stage++;
        }
    }

    private Ship offerEnemyChoice(int stage) {
        EnemyShip[] options = new EnemyShip[3];
        options[0] = generateEnemy(stage);
        options[1] = generateEnemyDifferentElement(stage, options[0].getElement());
        options[2] = generateEliteEnemy(stage);

        List<String> lines = new ArrayList<>(buildStageOverviewLines());
        lines.add("Pilih musuh yang akan dihadapi:");
        lines.add("");

        for (int i = 0; i < options.length; i++) {
            lines.add("[" + (i + 1) + "] " + options[i].getFullDescription());
            lines.addAll(buildCounterHintLines(options[i]));
            lines.add("");
        }

        int choice = ui.promptChoice(stageTitle(), lines, 1, 3, 1, "Pilihan musuh (1-3):");
        return options[choice - 1];
    }

    private void handleReward() {
        List<RewardCard> cards = RewardCard.drawThree(rng, player);
        List<String> lines = new ArrayList<>(buildStageOverviewLines());
        lines.add("Pilih reward:");
        lines.add("");
        for (int i = 0; i < cards.size(); i++) {
            lines.add("[" + (i + 1) + "] " + cards.get(i).getDisplay());
        }

        int choice = ui.promptChoice("Reward", lines, 1, 3, 1, "Pilihan reward (1-3):");
        RewardCard chosenCard = cards.get(choice - 1);

        List<String> resultLines = new ArrayList<>();
        resultLines.add("Reward dipilih: " + chosenCard.getTitle());
        resultLines.add("");
        resultLines.addAll(applyReward(chosenCard));
        ui.pause("Reward", resultLines);
    }

    private List<String> applyReward(RewardCard card) {
        List<String> lines = new ArrayList<>();
        switch (card.getType()) {
            case RECRUIT_MAGE -> {
                if (player.getMageCount() >= 5) {
                    player.heal(45);
                    lines.add("Roster penuh. Reward diganti heal kecil.");
                    break;
                }
                Mage newMage = new Mage(randomMageName(), randomElement(), 18 + rng.nextInt(18), rng);
                player.recruitMage(newMage);
                lines.add("Mage baru bergabung: " + newMage.getInfo(true));
                lines.add("Jurus: " + newMage.getSpellType().getFullDescription());
                int count = player.countMageByElement(newMage.getElement());
                if (count >= 2) {
                    lines.add(String.format("Sinergy terbentuk: %dx %s -> +%d%% magic damage",
                            count, newMage.getElement().sym(), count >= 3 ? 40 : 20));
                }
            }
            case HEAL_SMALL -> {
                int before = player.getCurrentHp();
                player.heal(45);
                lines.add(String.format("Perbaikan cepat. HP: %d -> %d", before, player.getCurrentHp()));
            }
            case HEAL_LARGE -> {
                int before = player.getCurrentHp();
                player.heal(80);
                lines.add(String.format("Perbaikan total. HP: %d -> %d", before, player.getCurrentHp()));
            }
            case UPGRADE_CANNON -> {
                int before = player.getBaseDamage();
                player.upgradeBaseDamage(7);
                lines.add(String.format("Meriam di-upgrade. Damage: %d -> %d", before, player.getBaseDamage()));
            }
            case UPGRADE_MAGE_POWER -> {
                if (player.getRoster().isEmpty()) {
                    player.heal(45);
                    lines.add("Tidak ada Mage. Reward diganti heal kecil.");
                    break;
                }
                List<String> chooser = new ArrayList<>();
                chooser.add("Pilih Mage yang akan dilatih:");
                chooser.addAll(player.getSelectableMageRosterLines(false));
                int idx = ui.promptChoice("Latih Mage", chooser, 1, player.getMageCount(), 1, "Nomor Mage: ") - 1;
                Mage mage = player.getRoster().get(idx);
                int before = mage.getMagicPower();
                mage.upgradePower(18);
                lines.add(String.format("%s di-upgrade. Power: %d -> %d", mage.getName(), before, mage.getMagicPower()));
            }
            case ADD_POTION -> {
                if (!player.addPotion()) {
                    player.heal(45);
                    lines.add("Potion penuh. Reward diganti heal kecil.");
                } else {
                    lines.add(String.format("+1 Potion. Sisa sekarang: %d/3", player.getPotions()));
                }
            }
            case ADD_EXPLOSIVE -> {
                player.addAmmo(CannonballType.EXPLOSIVE, 3);
                lines.add("Amunisi Ledak +3. Stok: " + player.getAmmoCount(CannonballType.EXPLOSIVE));
            }
            case ADD_CHAIN -> {
                player.addAmmo(CannonballType.CHAIN, 3);
                lines.add("Amunisi Rantai +3. Stok: " + player.getAmmoCount(CannonballType.CHAIN));
            }
            case ADD_GRAPESHOT -> {
                player.addAmmo(CannonballType.GRAPESHOT, 3);
                lines.add("Amunisi Angin +3. Stok: " + player.getAmmoCount(CannonballType.GRAPESHOT));
            }
            case UPGRADE_ALL_MAGE_SMALL -> {
                for (Mage mage : player.getRoster()) {
                    mage.upgradePower(5);
                }
                lines.add("Ritual kolektif aktif. Semua Mage +5 Magic Power.");
            }
            case DOUBLE_CANNON_DMG -> {
                cannonDoubled.set(true);
                lines.add("Kapal Berkibar aktif. Cannon damage x2 di battle berikutnya.");
            }
        }
        return lines;
    }

    private void printVictory(EnemyShip enemy, List<String> levelUps) {
        List<String> lines = new ArrayList<>();
        lines.add(enemy.getName() + " tenggelam.");
        lines.add(String.format("Bounty +$%d | XP Mage +%d | Total bounty $%d",
                enemy.getBountyReward(), enemy.getXpReward(), player.getBounty()));
        if (!levelUps.isEmpty()) {
            lines.add("");
            lines.addAll(levelUps);
        }
        ui.pause("Menang", lines);
    }

    private void printGameOver() {
        List<String> lines = new ArrayList<>();
        lines.add("Kapal Anda tenggelam di stage " + stage + ".");
        lines.add("Total bounty: $" + player.getBounty());
        lines.add("");
        lines.add("Kru terakhir:");
        lines.addAll(player.getMageDisplayLines(false));
        lines.add("");
        lines.add("Terima kasih sudah berlayar di Runewater.");
        ui.pause("Runewater - Game Over", lines);
    }

    private List<String> buildStageOverviewLines() {
        List<String> lines = new ArrayList<>();
        lines.add(player.getStatusDisplay());
        lines.add(String.format(
                "Amunisi: Iron(inf) | Explosive %d | Chain %d | Grapeshot %d",
                player.getAmmoCount(CannonballType.EXPLOSIVE),
                player.getAmmoCount(CannonballType.CHAIN),
                player.getAmmoCount(CannonballType.GRAPESHOT)));
        lines.add("");
        lines.add("Kru Mage:");
        lines.addAll(player.getMageDisplayLines(false));
        lines.add("");

        for (Element el : Element.values()) {
            int count = player.countMageByElement(el);
            if (count >= 2) {
                lines.add(String.format("Sinergy %s: %dx Mage -> +%d%% magic damage",
                        el.sym(), count, count >= 3 ? 40 : 20));
            }
        }

        if (cannonDoubled.get()) {
            lines.add("Bonus tersimpan: Cannon damage x2 di battle berikutnya.");
        }
        return lines;
    }

    private List<String> buildBossIntro(Ship currentEnemy) {
        List<String> lines = new ArrayList<>(buildStageOverviewLines());
        lines.add("*** STAGE BOSS ***");
        lines.add(currentEnemy.getStatusDisplay());
        return lines;
    }

    private List<String> buildPreBattleInfo(Ship enemy) {
        List<String> lines = new ArrayList<>(buildStageOverviewLines());
        lines.add("Musuh terpilih:");
        lines.add(enemy.getStatusDisplay());
        lines.add("");

        if (enemy instanceof EnemyShip enemyShip) {
            EnemyTrait trait = enemyShip.getTrait();
            if (trait == EnemyTrait.ARMORED) {
                lines.add("TIP: Musuh berzirah, sihir Mage lebih efektif.");
            }
            if (trait == EnemyTrait.REGENERATE) {
                lines.add("TIP: Musuh beregenerasi, habisi secepat mungkin.");
            }
            if (trait == EnemyTrait.BERSERKER) {
                lines.add("TIP: Saat HP musuh turun, damage-nya akan naik.");
            }
            if (trait == EnemyTrait.THORNS) {
                lines.add("TIP: Sebagian damage sihir memantul.");
            }
        }

        lines.add("Counter elemen: " + enemy.getElement().weakness().sym());
        if (player.hasCounterMage(enemy.getElement())) {
            lines.add("Anda punya Mage counter untuk pertarungan ini.");
        }
        return lines;
    }

    private List<String> buildCounterHintLines(EnemyShip enemy) {
        List<String> lines = new ArrayList<>();
        Element counter = enemy.getElement().weakness();
        boolean hasCounter = player.hasCounterMage(enemy.getElement());
        int synergyCount = player.countMageByElement(counter);

        StringBuilder hint = new StringBuilder("Counter: ").append(counter.sym());
        if (hasCounter) {
            hint.append(" | Mage counter tersedia");
            if (synergyCount >= 2) {
                hint.append(" | Sinergy +").append(synergyCount >= 3 ? 40 : 20).append('%');
            }
        } else {
            hint.append(" | Belum punya Mage counter");
        }
        if (enemy.getTrait() != EnemyTrait.NONE) {
            hint.append(" | Sifat: ").append(enemy.getTrait().description);
        }
        lines.add(hint.toString());
        return lines;
    }

    private String stageTitle() {
        return (stage % 5 == 0) ? "Stage " + stage + " - Boss Battle" : "Stage " + stage;
    }

    private EnemyShip generateEnemy(int stage) {
        int hp = BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE;
        int damage = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
        int bounty = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE;
        int xp = BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE;
        return new EnemyShip(randomEnemyName(), hp, damage, randomElement(), bounty, xp, randomTrait());
    }

    private EnemyShip generateEnemyDifferentElement(int stage, Element exclude) {
        int hp = (int) ((BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE) * 1.15);
        int damage = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
        int bounty = (int) ((BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE) * 1.30);
        int xp = (int) ((BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE) * 1.15);

        Element element;
        do {
            element = randomElement();
        } while (element == exclude);

        return new EnemyShip(randomEnemyName(), hp, damage, element, bounty, xp, randomTrait());
    }

    private EnemyShip generateEliteEnemy(int stage) {
        int hp = (int) ((BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE) * 1.40);
        int damage = (int) ((BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE) * 1.20);
        int bounty = (int) ((BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE) * 1.80);
        int xp = (int) ((BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE) * 1.50);
        return new EnemyShip(randomEnemyName() + " [ELITE]", hp, damage, randomElement(), bounty, xp, randomTrait());
    }

    private BossShip generateBoss(int stage) {
        int baseHp = BalanceConfig.BASE_ENEMY_HP + stage * BalanceConfig.HP_SCALE;
        int baseDmg = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
        int hp = (int) (baseHp * BalanceConfig.BOSS_HP_MULT);
        int damage = (int) (baseDmg * BalanceConfig.BOSS_DMG_MULT);
        int bounty = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE + BalanceConfig.BOSS_BOUNTY_BON;
        int xp = (BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE) * 2;

        return new BossShip(
                randomEnemyName() + " [BOSS]",
                BOSS_TITLES[rng.nextInt(BOSS_TITLES.length)],
                hp, damage, randomElement(), bounty, xp, randomTrait()
        );
    }

    private EnemyTrait randomTrait() {
        EnemyTrait[] traits = EnemyTrait.values();
        int roll = rng.nextInt(traits.length + traits.length - 1);
        return roll < traits.length ? traits[roll] : EnemyTrait.NONE;
    }

    private Element randomElement() {
        return Element.values()[rng.nextInt(3)];
    }

    private String randomEnemyName() {
        return ENEMY_NAMES[rng.nextInt(ENEMY_NAMES.length)];
    }

    private String randomMageName() {
        return MAGE_NAMES[rng.nextInt(MAGE_NAMES.length)];
    }
}
