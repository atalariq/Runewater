package com.battleship.engine;

import java.util.ArrayList;
import java.util.List;
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

public class BattleState {

    private static final int MAX_LOG = 14;

    private final PlayerShip player;
    private final Ship enemy;
    private final AtomicBoolean cannonDoubled;
    private final List<String> battleLog = new ArrayList<>();

    private boolean playerFrozen;

    public BattleState(PlayerShip player, Ship enemy, AtomicBoolean cannonDoubled) {
        this.player = player;
        this.enemy = enemy;
        this.cannonDoubled = cannonDoubled;
        addLog("Pertempuran dimulai melawan " + enemy.getName() + ".");
    }

    public TurnResult turnStart() {
        List<String> log = new ArrayList<>();
        playerFrozen = player.isFrozen();
        String statusLog = player.processStatus();
        if (!statusLog.isEmpty()) addWrappedLog(log, statusLog);
        player.resetShield();
        if (playerFrozen) {
            log.add("[FROZEN] Anda membeku! Giliran dilewati.");
        }
        return buildResult(log, false, true, "");
    }

    public TurnResult execute(Command cmd) {
        List<String> log = new ArrayList<>();
        handleCommand(cmd, log);

        if (!enemy.isAlive()) {
            addLog("Musuh kalah!");
            return buildResult(log, true, true, "Kemenangan!");
        }

        doEnemyTurn(log);

        if (!player.isAlive()) {
            addLog("Kapal Anda tenggelam!");
            return buildResult(log, true, false, "Kekalahan!");
        }

        return buildResult(log, false, true, "");
    }

    public TurnResult skipTurn() {
        List<String> log = new ArrayList<>();
        log.add("[FROZEN] Giliran dilewati karena membeku.");
        player.applyStatus(StatusEffect.NONE, 0);
        playerFrozen = false;

        if (!enemy.isAlive()) {
            addLog("Musuh kalah!");
            return buildResult(log, true, true, "Kemenangan!");
        }

        doEnemyTurn(log);

        if (!player.isAlive()) {
            addLog("Kapal Anda tenggelam!");
            return buildResult(log, true, false, "Kekalahan!");
        }

        return buildResult(log, false, true, "");
    }

    public boolean canAct() {
        return !playerFrozen;
    }

    public boolean isOver() {
        return !player.isAlive() || !enemy.isAlive();
    }

    public boolean playerWon() {
        return player.isAlive() && !enemy.isAlive();
    }

    private void handleCommand(Command cmd, List<String> log) {
        if (cmd instanceof Command.Attack a) {
            doMagicAttack(a.mageIndex(), log);
        } else if (cmd instanceof Command.Cannonball c) {
            doCannonball(c.type(), log);
        } else if (cmd instanceof Command.Spell s) {
            doSpellAttack(s.mageIndex(), log);
        } else if (cmd instanceof Command.Defend) {
            player.activateShield();
            log.add("Kapal bersiap bertahan. Damage masuk -50% giliran ini.");
        } else if (cmd instanceof Command.Potion) {
            if (!player.usePotion()) {
                log.add("Potion habis. Aksi terlewati.");
            } else {
                log.add(String.format("Minum potion. HP sekarang %d/%d | Potion tersisa: %d",
                        player.getCurrentHp(), player.getMaxHp(), player.getPotions()));
            }
        }
    }

    private void doCannonball(CannonballType type, List<String> log) {
        double extraMult = cannonDoubled.get() ? 2.0 : 1.0;
        cannonDoubled.set(false);

        int damage = player.fireCannonball(type, enemy, extraMult);
        if (damage < 0) {
            log.add("Stok " + type.getDisplayName() + " habis. Otomatis pakai Peluru Besi.");
            damage = player.fireCannonball(CannonballType.IRON, enemy, extraMult);
            type = CannonballType.IRON;
        }

        log.add("Menembakkan " + type.getDisplayName() + ". Damage tercatat: " + damage);
        if (type.appliesBurn) {
            log.add(enemy.getName() + " terkena BURNED selama 3 turn.");
        }
        if (type.appliesWeak) {
            log.add(enemy.getName() + " terkena WEAKENED selama 3 turn.");
        }
    }

    private void doMagicAttack(int mageIndex, List<String> log) {
        if (player.getRoster().isEmpty()) {
            int damage = player.fireCannonball(CannonballType.IRON, enemy);
            log.add("Tidak ada Mage. Otomatis menembak Peluru Besi: " + damage + " damage.");
            return;
        }

        Mage mage = player.getRoster().get(mageIndex);

        double elemMult = mage.getElement().getMultiplier(enemy.getElement());
        double synergyMult = player.getSynergyMult(mage.getElement());
        int baseDmg = player.getBaseDamage() + mage.getMagicPower();

        EnemyTrait defenderTrait = (enemy instanceof EnemyShip es) ? es.getTrait() : EnemyTrait.NONE;
        AttackContext ctx = new AttackContext(
            player, enemy, AttackType.MAGIC, baseDmg,
            elemMult, synergyMult, null, null, mage.getElement(),
            defenderTrait, false
        );
        DamageResult result = DamagePipeline.resolve(ctx);
        enemy.takeDamage(result.finalDamage());

        if (result.hasReflectedDamage()) {
            player.takeDamage(result.reflectedDamage());
            log.add("[BERDURI] " + result.reflectedDamage() + " damage dipantulkan ke kapal Anda.");
        }

        log.add(mage.getName() + " melepaskan sihir " + mage.getElement().sym() + ".");
        log.add(mage.getElement().effectText(enemy.getElement()));
        if (synergyMult > 1.0) {
            log.add("[SINERGY +" + (int) ((synergyMult - 1.0) * 100) + "%]");
        }
        log.add(String.format("Damage: %d (x%.1f elemen, x%.2f sinergy)", result.finalDamage(), elemMult, synergyMult));
    }

    private void doSpellAttack(int mageIndex, List<String> log) {
        if (player.getRoster().isEmpty()) {
            int damage = player.fireCannonball(CannonballType.IRON, enemy);
            log.add("Tidak ada Mage. Otomatis menembak Peluru Besi: " + damage + " damage.");
            return;
        }

        Mage mage = player.getRoster().get(mageIndex);

        if (mage.isSpellUsed()) {
            log.add("Jurus " + mage.getSpellType().getDisplayName() + " sudah dipakai.");
            return;
        }

        String spellResult = player.castSpell(enemy, mage);
        addWrappedLog(log, spellResult);
    }

    private void doEnemyTurn(List<String> log) {
        String statusLog = enemy.processStatus();
        if (!statusLog.isEmpty()) addWrappedLog(log, statusLog);

        if (enemy.isFrozen()) {
            enemy.applyStatus(StatusEffect.NONE, 0);
            log.add("[FROZEN] " + enemy.getName() + " membeku! Giliran dilewati.");
            return;
        }

        String enemyLog = resolveEnemyTurn();
        if (!enemyLog.isEmpty()) addWrappedLog(log, enemyLog);
    }

    private String resolveEnemyTurn() {
        if (enemy instanceof BossShip bossShip) {
            return bossShip.takeTurnWithLog(player);
        }
        if (enemy instanceof EnemyShip enemyShip) {
            return enemyShip.takeTurnWithLog(player);
        }
        enemy.takeTurn(player);
        return enemy.getName() + " bergerak.";
    }

    private TurnResult buildResult(List<String> newEntries, boolean battleOver, boolean playerTurn, String actionDesc) {
        for (String entry : newEntries) {
            addLog(entry);
        }
        return new TurnResult(
            player.getHpBar(),
            enemy.getHpBar(),
            playerTurn,
            List.copyOf(battleLog),
            battleOver,
            playerWon(),
            actionDesc
        );
    }

    private void addLog(String line) {
        battleLog.add(line);
        while (battleLog.size() > MAX_LOG) {
            battleLog.remove(0);
        }
    }

    private void addWrappedLog(List<String> log, String block) {
        String[] lines = block.split("\\R");
        for (String line : lines) {
            if (!line.isBlank()) {
                log.add(line.trim());
            }
        }
    }
}
