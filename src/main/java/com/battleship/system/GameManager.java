package com.battleship.system;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import com.battleship.engine.BattleEngine;
import com.battleship.model.EnemyShip;
import com.battleship.model.Mage;
import com.battleship.model.PlayerShip;
import com.battleship.model.Ship;
import com.battleship.model.enums.CannonballType;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.EnemyVariant;
import com.battleship.model.enums.SpellType;
import com.battleship.ui.EnemyCardInfo;
import com.battleship.ui.MageInfo;
import com.battleship.ui.RewardCardInfo;
import com.battleship.ui.RewardViewModel;
import com.battleship.ui.ShipRenderer;
import com.battleship.ui.StageViewModel;
import com.battleship.ui.TitleViewModel;
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
    private final StageFactory factory;

    public GameManager() {
        this.input = new InputHelper(new Scanner(System.in));
        this.ui = new TerminalUi(input);
        this.factory = new StageFactory(rng);
    }

    public void start() {
        try {
            List<String> starterLines = List.of(
                "Pilih Mage pertama dan jurus pembuka:",
                "[1] FIRE  + INFERNO    (Damage x3.0, sekali pakai)",
                "[2] FIRE  + IGNITE     (Damage + BURNED 3 turn)",
                "[3] WATER + TIDAL WAVE (Damage x2.0 + Heal 35 HP)",
                "[4] WATER + FREEZE     (Damage + FROZEN)",
                "[5] STORM + CHAIN BOLT (Damage x2.5 + WEAKENED)",
                "[6] STORM + OVERCHARGE (Semua Mage menyerang)"
            );
            TitleViewModel titleVm = new TitleViewModel("", starterLines, null, "Masukkan nama kapten:");
            int mageChoice = ui.showTitleScreen(titleVm);
            String captainName = ui.getLastCaptainName();
            initPlayer(captainName, mageChoice);
            gameLoop();
        } finally {
            ui.shutdown();
        }
    }

    private void initPlayer(String captainName, int mageChoice) {
        if (captainName.isBlank()) {
            captainName = "Nusantara";
        }

        player = new PlayerShip(
                "Kapal " + captainName,
                BalanceConfig.PLAYER_HP,
                BalanceConfig.PLAYER_DAMAGE,
                Element.WATER
        );

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
                factory.randomMageName(),
                elements[mageChoice - 1],
                BalanceConfig.START_MAGE_PWR,
                spells[mageChoice - 1]
        );
        player.recruitMage(startingMage);

        ui.showInfo(new TitleViewModel("", List.of(
                "Kapal: " + player.getName(),
                "Mage  : " + startingMage.getInfo(true),
                "Jurus : " + startingMage.getSpellType().getFullDescription(),
                "Peluru: Iron tak terbatas, Explosive x3, Chain x3, Grapeshot x3",
                "Potion: 2",
                "",
                "Armada Runewater siap berlayar."
        ), null, null));

        battleEngine = new BattleEngine(player, ui, rng, cannonDoubled);
    }

    private void gameLoop() {
        while (player.isAlive()) {
            cannonDoubled.set(false);
            boolean isBoss = (stage % 5 == 0);
            Ship currentEnemy;

            if (isBoss) {
                currentEnemy = factory.generateBoss(stage);
                List<String> lines = new ArrayList<>();
                lines.add(ShipRenderer.renderPlayerStatus(player));
                lines.add(String.format(
                        "Amunisi: Iron(inf) | Explosive %d | Chain %d | Grapeshot %d",
                        player.getAmmoCount(CannonballType.EXPLOSIVE),
                        player.getAmmoCount(CannonballType.CHAIN),
                        player.getAmmoCount(CannonballType.GRAPESHOT)));
                lines.add("");
                lines.add("Kru Mage:");
                lines.addAll(ShipRenderer.renderMageRosterPlain(player.getRoster(), false));
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
                lines.add("*** STAGE BOSS ***");
                lines.add(ShipRenderer.renderEnemyStatus((EnemyShip) currentEnemy));
                ui.showInfo(new TitleViewModel(stageTitle(), lines, null, null));
            } else {
                currentEnemy = offerEnemyChoice(stage);
            }

            List<String> preLines = new ArrayList<>();
            preLines.add(ShipRenderer.renderPlayerStatus(player));
            preLines.add(String.format(
                    "Amunisi: Iron(inf) | Explosive %d | Chain %d | Grapeshot %d",
                    player.getAmmoCount(CannonballType.EXPLOSIVE),
                    player.getAmmoCount(CannonballType.CHAIN),
                    player.getAmmoCount(CannonballType.GRAPESHOT)));
            preLines.add("");
            preLines.add("Kru Mage:");
            preLines.addAll(ShipRenderer.renderMageRosterPlain(player.getRoster(), false));
            preLines.add("");
            for (Element el : Element.values()) {
                int count = player.countMageByElement(el);
                if (count >= 2) {
                    preLines.add(String.format("Sinergy %s: %dx Mage -> +%d%% magic damage",
                            el.sym(), count, count >= 3 ? 40 : 20));
                }
            }
            if (cannonDoubled.get()) {
                preLines.add("Bonus tersimpan: Cannon damage x2 di battle berikutnya.");
            }
            preLines.add("Musuh terpilih:");
            preLines.add(ShipRenderer.renderEnemyStatus((EnemyShip) currentEnemy));
            preLines.add("");
            if (currentEnemy instanceof EnemyShip enemyShip) {
                EnemyTrait trait = enemyShip.getTrait();
                if (trait == EnemyTrait.ARMORED) {
                    preLines.add("TIP: Musuh berzirah, sihir Mage lebih efektif.");
                }
                if (trait == EnemyTrait.REGENERATE) {
                    preLines.add("TIP: Musuh beregenerasi, habisi secepat mungkin.");
                }
                if (trait == EnemyTrait.BERSERKER) {
                    preLines.add("TIP: Saat HP musuh turun, damage-nya akan naik.");
                }
                if (trait == EnemyTrait.THORNS) {
                    preLines.add("TIP: Sebagian damage sihir memantul.");
                }
            }
            preLines.add("Counter elemen: " + currentEnemy.getElement().weakness().sym());
            if (player.hasCounterMage(currentEnemy.getElement())) {
                preLines.add("Anda punya Mage counter untuk pertarungan ini.");
            }
            ui.showInfo(new TitleViewModel(stageTitle(), preLines, null, null));

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
        while (true) {
            EnemyShip[] options = new EnemyShip[3];
            options[0] = factory.generateEnemy(stage, EnemyVariant.NORMAL, null);
            options[1] = factory.generateEnemy(stage, EnemyVariant.DIFFERENT_ELEMENT, options[0].getElement());
            options[2] = factory.generateEnemy(stage, EnemyVariant.ELITE, null);

            List<EnemyCardInfo> enemies = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                EnemyShip opt = options[i];
                enemies.add(new EnemyCardInfo(
                    opt.getName(),
                    opt.getCurrentHp(),
                    opt.getMaxHp(),
                    opt.getElement().sym(),
                    opt.getTrait().displayName,
                    opt.getBountyReward(),
                    opt.getBaseDamage(),
                    i == 2
                ));
            }

            List<String> counterHints = new ArrayList<>();
            for (EnemyShip opt : options) {
                Element counter = opt.getElement().weakness();
                boolean hasCounter = player.hasCounterMage(opt.getElement());
                int synergy = player.countMageByElement(counter);

                StringBuilder sb = new StringBuilder();
                sb.append("Counter: ").append(counter.sym()).append(" | ");
                if (hasCounter) {
                    sb.append("Tersedia");
                    if (synergy >= 2) {
                        sb.append(" | Sinergy +").append(synergy >= 3 ? 40 : 20).append('%');
                    }
                } else {
                    sb.append("Belum punya");
                }
                counterHints.add(sb.toString());
            }

            StageViewModel vm = new StageViewModel(
                ShipRenderer.renderPlayerStatus(player),
                stageTitle(),
                enemies,
                counterHints
            );

            int choice = ui.showStageSelect(vm);
            if (choice >= 1 && choice <= 3) {
                return options[choice - 1];
            }
        }
    }

    private void handleReward() {
        while (true) {
            List<RewardCard> cards = RewardCard.drawThree(rng, player);

            List<MageInfo> rosterInfo = new ArrayList<>();
            for (Mage m : player.getRoster()) {
                rosterInfo.add(new MageInfo(
                    m.getName(), m.getLevel(), m.getElement().sym(),
                    m.getMagicPower(), m.getXp(),
                    m.getSpellType().getDisplayName(), m.isSpellUsed()
                ));
            }

            Map<CannonballType, Integer> ammoMap = new EnumMap<>(CannonballType.class);
            for (CannonballType ct : CannonballType.values()) {
                ammoMap.put(ct, player.getAmmoCount(ct));
            }

            List<RewardCardInfo> cardInfos = new ArrayList<>();
            for (RewardCard card : cards) {
                cardInfos.add(new RewardCardInfo(
                    card.getTitle(), card.getDescription(),
                    rewardRarityStars(card.getWeight())
                ));
            }

            RewardViewModel vm = new RewardViewModel(
                String.format("HP: %d/%d | Cannon: %d | Bounty: $%d",
                    player.getCurrentHp(), player.getMaxHp(),
                    player.getBaseDamage(), player.getBounty()),
                rosterInfo,
                ammoMap,
                player.getPotions(),
                cardInfos
            );

            int choice = ui.showRewardSelect(vm);
            if (choice < 1 || choice > 3) continue;

            RewardCard chosenCard = cards.get(choice - 1);

            List<String> resultLines = new ArrayList<>();
            resultLines.add("Reward dipilih: " + chosenCard.getTitle());
            resultLines.add("");

            if (chosenCard.getType() == RewardCard.Type.UPGRADE_MAGE_POWER
                    && !player.getRoster().isEmpty()) {
                List<String> chooser = new ArrayList<>();
                chooser.add("Pilih Mage yang akan dilatih:");
                chooser.addAll(ShipRenderer.renderMageRosterSelectable(player.getRoster(), false));
                int idx = ui.promptChoice("Latih Mage", chooser, 1, player.getMageCount(), 1, "Nomor Mage: ") - 1;
                resultLines.addAll(chosenCard.applyMageUpgrade(player, idx));
            } else {
                resultLines.addAll(chosenCard.apply(player));
            }

            if (chosenCard.getType() == RewardCard.Type.DOUBLE_CANNON_DMG) {
                cannonDoubled.set(true);
            }

            ui.showInfo(new TitleViewModel("Reward", resultLines, null, null));
            break;
        }
    }

    private static String rewardRarityStars(int weight) {
        if (weight <= 2) return "\u2605\u2605\u2605\u2605\u2605";
        if (weight <= 4) return "\u2605\u2605\u2605\u2605\u2606";
        if (weight <= 6) return "\u2605\u2605\u2605\u2606\u2606";
        if (weight <= 9) return "\u2605\u2605\u2606\u2606\u2606";
        return "\u2605\u2606\u2606\u2606\u2606";
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
        ui.showInfo(new TitleViewModel("Menang", lines, null, null));
    }

    private void printGameOver() {
        List<String> lines = new ArrayList<>();
        lines.add("Kapal Anda tenggelam di stage " + stage + ".");
        lines.add("Total bounty: $" + player.getBounty());
        lines.add("");
        lines.add("Kru terakhir:");
        lines.addAll(ShipRenderer.renderMageRosterPlain(player.getRoster(), false));
        lines.add("");
        lines.add("Terima kasih sudah berlayar di Runewater.");
        ui.showInfo(new TitleViewModel("Runewater - Game Over", lines, null, null));
    }

    private String stageTitle() {
        return (stage % 5 == 0) ? "Stage " + stage + " - Boss Battle" : "Stage " + stage;
    }
}
