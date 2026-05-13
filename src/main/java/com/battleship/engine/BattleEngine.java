package com.battleship.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.battleship.model.BossShip;
import com.battleship.model.EnemyShip;
import com.battleship.model.Mage;
import com.battleship.model.PlayerShip;
import com.battleship.model.Ship;
import com.battleship.model.enums.CannonballType;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.StatusEffect;
import com.battleship.util.TerminalUi;

/**
 * Handles battle flow while delegating rendering and input to TerminalUi.
 */
public class BattleEngine {

    private static final int MAX_LOG_LINES = 14;

    private final PlayerShip player;
    private final TerminalUi ui;
    private final Random rng;
    private final AtomicBoolean cannonDoubled;

    public BattleEngine(PlayerShip player, TerminalUi ui, Random rng, AtomicBoolean cannonDoubled) {
        this.player = player;
        this.ui = ui;
        this.rng = rng;
        this.cannonDoubled = cannonDoubled;
    }

    public boolean runBattle(Ship enemy) {
        boolean playerTurn = true;
        List<String> battleLog = new ArrayList<>();
        addLog(battleLog, "Pertempuran dimulai melawan " + enemy.getName() + ".");

        while (player.isAlive() && enemy.isAlive()) {
            if (playerTurn) {
                String statusLog = player.processStatus();
                if (!statusLog.isEmpty()) addWrappedLog(battleLog, statusLog);

                if (player.isFrozen()) {
                    addLog(battleLog, "[FROZEN] Anda membeku! Giliran dilewati.");
                    player.applyStatus(StatusEffect.NONE, 0);
                    playerTurn = false;
                    ui.pause("Battle", buildBattleLines(enemy, battleLog, List.of("Giliran Anda terlewati.")));
                    continue;
                }
                player.resetShield();
            } else {
                String statusLog = enemy.processStatus();
                if (!statusLog.isEmpty()) addWrappedLog(battleLog, statusLog);

                if (enemy.isFrozen()) {
                    addLog(battleLog, "[FROZEN] " + enemy.getName() + " membeku! Giliran dilewati.");
                    enemy.applyStatus(StatusEffect.NONE, 0);
                    playerTurn = true;
                    if (!enemy.isAlive()) break;
                    ui.pause("Battle", buildBattleLines(enemy, battleLog, List.of("Musuh gagal bergerak.")));
                    continue;
                }
            }

            if (!enemy.isAlive() || !player.isAlive()) break;

            if (playerTurn) {
                doPlayerTurn(enemy, battleLog);
            } else {
                addWrappedLog(battleLog, resolveEnemyTurn(enemy));
            }

            playerTurn = !playerTurn;
            if (!enemy.isAlive() || !player.isAlive()) break;
            ui.pause("Battle", buildBattleLines(enemy, battleLog, List.of("Pertempuran berlanjut...")));
        }

        return player.isAlive();
    }

    public void doPlayerTurn(Ship enemy, List<String> battleLog) {
        List<String> actions = List.of(
                "[1] Tembak Meriam",
                "[2] Sihir Mage",
                "[3] Gunakan Jurus",
                "[4] Bertahan",
                String.format("[5] Minum Potion (%d tersisa)", player.getPotions())
        );

        int choice = ui.promptChoice(
                "Battle",
                buildBattleLines(enemy, battleLog, actions),
                1, 5, 1,
                "Pilih aksi (1-5):");

        switch (choice) {
            case 1 -> doCannonball(enemy, battleLog);
            case 2 -> doMagicAttack(enemy, battleLog);
            case 3 -> doSpellAttack(enemy, battleLog);
            case 4 -> {
                player.activateShield();
                addLog(battleLog, "Kapal bersiap bertahan. Damage masuk -50% giliran ini.");
            }
            case 5 -> {
                if (!player.usePotion()) {
                    addLog(battleLog, "Potion habis. Aksi terlewati.");
                } else {
                    addLog(battleLog, String.format(
                            "Minum potion. HP sekarang %d/%d | Potion tersisa: %d",
                            player.getCurrentHp(), player.getMaxHp(), player.getPotions()));
                }
            }
            default -> addLog(battleLog, "Aksi tidak dikenali.");
        }
    }

    public void doCannonball(Ship enemy, List<String> battleLog) {
        List<String> lines = new ArrayList<>(buildBattleLines(enemy, battleLog, List.of("Pilih tipe peluru:")));
        CannonballType[] types = CannonballType.values();

        for (int i = 0; i < types.length; i++) {
            CannonballType type = types[i];
            String stock = (type == CannonballType.IRON) ? "tak terbatas" : "stok: " + player.getAmmoCount(type);
            lines.add(String.format("[%d] %s | %s", i + 1, type.getFullDescription(), stock));
        }

        if (enemy instanceof EnemyShip enemyShip) {
            EnemyTrait trait = enemyShip.getTrait();
            if (trait == EnemyTrait.ARMORED) {
                lines.add("TIP: Musuh berzirah, cannon fisik berkurang 35%.");
            }
            if (trait == EnemyTrait.REGENERATE) {
                lines.add("TIP: Musuh beregenerasi, burst damage lebih aman.");
            }
        }

        if (cannonDoubled.get()) {
            lines.add("BONUS: Cannon damage x2 aktif untuk serangan ini.");
        }

        int choice = ui.promptChoice("Battle", lines, 1, 4, 1, "Pilih peluru (1-4):");
        CannonballType chosen = types[choice - 1];

        int damage = player.fireCannonball(chosen, enemy);
        if (damage < 0) {
            addLog(battleLog, "Stok " + chosen.getDisplayName() + " habis. Otomatis pakai Peluru Besi.");
            damage = player.fireCannonball(CannonballType.IRON, enemy);
            chosen = CannonballType.IRON;
        }

        if (enemy instanceof EnemyShip enemyShip) {
            damage = enemyShip.applyArmorReduction(damage, true);
        }

        if (cannonDoubled.get()) {
            damage *= 2;
            cannonDoubled.set(false);
        }

        addLog(battleLog, String.format("Menembakkan %s. Damage tercatat: %d", chosen.getDisplayName(), damage));
        if (chosen.appliesBurn) {
            addLog(battleLog, enemy.getName() + " terkena BURNED selama 3 turn.");
        }
        if (chosen.appliesWeak) {
            addLog(battleLog, enemy.getName() + " terkena WEAKENED selama 3 turn.");
        }
    }

    public void doMagicAttack(Ship enemy, List<String> battleLog) {
        if (player.getRoster().isEmpty()) {
            int damage = player.fireCannonball(CannonballType.IRON, enemy);
            addLog(battleLog, "Tidak ada Mage. Otomatis menembak Peluru Besi: " + damage + " damage.");
            return;
        }

        List<String> lines = new ArrayList<>(buildBattleLines(enemy, battleLog, List.of(
                "Pilih Mage untuk menyerang:",
                "Elemen musuh: " + enemy.getElement().sym() + " | Counter: " + enemy.getElement().weakness().sym()
        )));
        lines.addAll(player.getSelectableMageRosterLines(false));
        lines.addAll(buildSynergyLines());

        int mageIndex = ui.promptChoice("Battle", lines, 1, player.getMageCount(), 1, "Nomor Mage: ") - 1;
        Mage mage = player.getRoster().get(mageIndex);

        double elemMult = mage.getElement().getMultiplier(enemy.getElement());
        double synergyMult = player.getSynergyMult(mage.getElement());
        int damage = player.castMagic(enemy, mage);

        if (enemy instanceof EnemyShip enemyShip && enemyShip.getTrait() == EnemyTrait.THORNS) {
            int reflected = (int) (damage * 0.15);
            player.takeDamage(reflected);
            addLog(battleLog, "[BERDURI] " + reflected + " damage dipantulkan ke kapal Anda.");
        }

        addLog(battleLog, mage.getName() + " melepaskan sihir " + mage.getElement().sym() + ".");
        addLog(battleLog, mage.getElement().effectText(enemy.getElement()));
        if (synergyMult > 1.0) {
            addLog(battleLog, "[SINERGY +" + (int) ((synergyMult - 1.0) * 100) + "%]");
        }
        addLog(battleLog, String.format("Damage: %d (x%.1f elemen, x%.2f sinergy)", damage, elemMult, synergyMult));
    }

    public void doSpellAttack(Ship enemy, List<String> battleLog) {
        if (player.getRoster().isEmpty()) {
            int damage = player.fireCannonball(CannonballType.IRON, enemy);
            addLog(battleLog, "Tidak ada Mage. Otomatis menembak Peluru Besi: " + damage + " damage.");
            return;
        }

        List<String> lines = new ArrayList<>(buildBattleLines(enemy, battleLog, List.of("Pilih Mage untuk Jurus:")));
        lines.addAll(player.getSelectableMageRosterLines(true));
        int mageIndex = ui.promptChoice("Battle", lines, 1, player.getMageCount(), 1, "Nomor Mage: ") - 1;
        Mage mage = player.getRoster().get(mageIndex);

        if (mage.isSpellUsed()) {
            addLog(battleLog, "Jurus " + mage.getSpellType().getDisplayName() + " sudah dipakai.");
            return;
        }

        String spellResult = player.castSpell(enemy, mage);
        addWrappedLog(battleLog, spellResult);

        if (enemy instanceof EnemyShip enemyShip && enemyShip.getTrait() == EnemyTrait.THORNS) {
            addLog(battleLog, "[BERDURI] Sebagian efek jurus memantul ke kapal Anda.");
        }
    }

    private String resolveEnemyTurn(Ship enemy) {
        if (enemy instanceof BossShip bossShip) {
            return bossShip.takeTurnWithLog(player);
        }
        if (enemy instanceof EnemyShip enemyShip) {
            return enemyShip.takeTurnWithLog(player);
        }
        enemy.takeTurn(player);
        return enemy.getName() + " bergerak.";
    }

    private List<String> buildBattleLines(Ship enemy, List<String> battleLog, List<String> actionLines) {
        List<String> lines = new ArrayList<>();
        lines.add("PLAYER : " + player.getHpBar() + (player.isShielded() ? " [SHIELD]" : ""));
        lines.add("MUSUH  : " + enemy.getHpBar());
        lines.add("");
        lines.add("Stage battle melawan " + enemy.getName() + " | Elemen: " + enemy.getElement().sym());
        lines.add("");
        lines.add("Log tempur:");
        if (battleLog.isEmpty()) {
            lines.add("  Belum ada aksi.");
        } else {
            for (String logLine : battleLog) {
                lines.add("  " + logLine);
            }
        }
        if (actionLines != null && !actionLines.isEmpty()) {
            lines.add("");
            lines.add("Pilihan:");
            lines.addAll(actionLines);
        }
        return lines;
    }

    private List<String> buildSynergyLines() {
        List<String> lines = new ArrayList<>();
        for (Element el : Element.values()) {
            int count = player.countMageByElement(el);
            if (count >= 2) {
                lines.add(String.format("SINERGY %s: %dx Mage -> +%d%% magic damage",
                        el.sym(), count, count >= 3 ? 40 : 20));
            }
        }
        return lines;
    }

    private void addWrappedLog(List<String> battleLog, String block) {
        String[] lines = block.split("\\R");
        for (String line : lines) {
            if (!line.isBlank()) {
                addLog(battleLog, line.trim());
            }
        }
    }

    private void addLog(List<String> battleLog, String line) {
        battleLog.add(line);
        while (battleLog.size() > MAX_LOG_LINES) {
            battleLog.remove(0);
        }
    }
}
