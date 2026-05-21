package com.battleship.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.battleship.ui.BattleViewModel;
import com.battleship.ui.CommandInfo;
import com.battleship.ui.MageInfo;
import com.battleship.ui.ShipRenderer;
import com.battleship.util.TerminalUi;

public class BattleEngine {

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
        BattleState state = new BattleState(player, enemy, cannonDoubled);
        TurnResult tr = state.turnStart();

        while (!state.isOver()) {
            if (state.canAct()) {
                Command cmd = resolveCommand(enemy, tr.battleLog());
                if (cmd == null) continue;
                tr = state.execute(cmd);
            } else {
                tr = state.skipTurn();
            }

            if (!state.isOver()) {
                tr = state.turnStart();
            }
        }

        List<String> endLines = new ArrayList<>();
        endLines.addAll(tr.battleLog());
        endLines.add("");
        endLines.add(tr.playerWon() ? "Kemenangan!" : "Kekalahan!");
        ui.pause("Battle Result", endLines);
        return state.playerWon();
    }

    private Command resolveCommand(Ship enemy, List<String> battleLog) {
        BattleViewModel vm = buildViewModel(enemy, battleLog);
        ui.showBattle(vm);

        while (true) {
            int cmdNum = ui.promptMainCommand();
            if (cmdNum == 0) continue;

            Command result = trySubCommand(cmdNum, enemy);
            if (result != null) return result;

            ui.showBattle(vm);
        }
    }

    private Command trySubCommand(int cmdNum, Ship enemy) {
        return switch (cmdNum) {
            case 1 -> promptCannonball(enemy);
            case 2 -> promptMagicAttack(enemy);
            case 3 -> promptSpellAttack(enemy);
            case 4 -> new Command.Defend();
            case 5 -> new Command.Potion();
            default -> null;
        };
    }

    private Command promptCannonball(Ship enemy) {
        List<String> items = new ArrayList<>();
        for (CannonballType type : CannonballType.values()) {
            String stock = (type == CannonballType.IRON)
                    ? "∞ (tak terbatas)"
                    : "" + player.getAmmoCount(type);
            items.add(type.getDisplayName() + "  stok: " + stock);
        }

        String bonus = "";
        if (enemy instanceof EnemyShip es && es.getTrait() != EnemyTrait.NONE) {
            bonus = " [" + es.getTrait().displayName + "]";
        }
        if (cannonDoubled.get()) {
            bonus += " [x2 aktif]";
        }

        int choice = ui.showOverlay("Pilih Peluru" + bonus, items);
        if (choice == 0) return null;
        return new Command.Cannonball(CannonballType.values()[choice - 1]);
    }

    private Command promptMagicAttack(Ship enemy) {
        if (player.getRoster().isEmpty()) {
            return new Command.Attack(0);
        }

        List<String> items = new ArrayList<>();
        items.add("Elemen musuh: " + enemy.getElement().sym()
                + " | Counter: " + enemy.getElement().weakness().sym());
        items.add("");
        for (int i = 0; i < player.getRoster().size(); i++) {
            Mage m = player.getRoster().get(i);
            items.add(String.format("Lv.%d %s %s  Pwr:%d",
                    m.getLevel(), m.getElement().sym(), m.getName(), m.getMagicPower()));
        }
        items.add("");
        for (Element el : Element.values()) {
            int count = player.countMageByElement(el);
            if (count >= 2) {
                items.add("Sinergy " + el.sym() + ": +" + (count >= 3 ? 40 : 20) + "%");
            }
        }

        int choice = ui.showOverlay("Pilih Mage untuk Sihir", items);
        if (choice == 0) return null;
        int mageIdx = choice - 1;
        if (mageIdx < 0 || mageIdx >= player.getMageCount()) return null;
        return new Command.Attack(mageIdx);
    }

    private Command promptSpellAttack(Ship enemy) {
        if (player.getRoster().isEmpty()) {
            return new Command.Attack(0);
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < player.getRoster().size(); i++) {
            Mage m = player.getRoster().get(i);
            String status = m.isSpellUsed() ? "[USED]" : "[READY]";
            items.add(String.format("Lv.%d %s %s  %s %s",
                    m.getLevel(), m.getElement().sym(), m.getName(),
                    m.getSpellType().getDisplayName(), status));
        }

        int choice = ui.showOverlay("Pilih Mage untuk Jurus", items);
        if (choice == 0) return null;
        int mageIdx = choice - 1;
        if (mageIdx < 0 || mageIdx >= player.getMageCount()) return null;
        return new Command.Spell(mageIdx);
    }

    private BattleViewModel buildViewModel(Ship enemy, List<String> battleLog) {
        String playerStatus = player.getStatus() != com.battleship.model.enums.StatusEffect.NONE
                ? player.getStatus().getTag() : "";
        String enemyStatus = enemy.getStatus() != com.battleship.model.enums.StatusEffect.NONE
                ? enemy.getStatus().getTag() : "";
        String enemyTraitName = (enemy instanceof EnemyShip es) ? es.getTrait().displayName : "";
        boolean enemyEnraged = (enemy instanceof BossShip boss) && boss.isEnraged();

        List<MageInfo> roster = new ArrayList<>();
        for (Mage m : player.getRoster()) {
            roster.add(new MageInfo(m.getName(), m.getLevel(), m.getElement().sym(),
                    m.getMagicPower(), m.getXp(), m.getSpellType().getDisplayName(), m.isSpellUsed()));
        }

        Map<CannonballType, Integer> ammo = new HashMap<>();
        for (CannonballType t : CannonballType.values()) {
            ammo.put(t, player.getAmmoCount(t));
        }

        List<CommandInfo> cmds = List.of(
                new CommandInfo(1, "Tembak"),
                new CommandInfo(2, "Sihir"),
                new CommandInfo(3, "Jurus"),
                new CommandInfo(4, "Bertahan"),
                new CommandInfo(5, "Potion (" + player.getPotions() + ")")
        );

        return new BattleViewModel(
                player.getName(),
                player.getCurrentHp(),
                player.getMaxHp(),
                player.isShielded(),
                playerStatus,
                enemy.getName(),
                enemy.getCurrentHp(),
                enemy.getMaxHp(),
                enemy.getElement().sym(),
                enemyTraitName,
                enemyStatus,
                enemyEnraged,
                "Stage: " + enemy.getName(),
                battleLog,
                roster,
                ammo,
                player.getPotions(),
                cmds,
                null
        );
    }
}
