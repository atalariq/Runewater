package com.battleship.ui;

import java.util.List;
import java.util.Map;
import com.battleship.model.enums.CannonballType;

public record BattleViewModel(
    String playerName,
    int playerHp,
    int playerMaxHp,
    boolean playerShielded,
    String playerStatus,
    String enemyName,
    int enemyHp,
    int enemyMaxHp,
    String enemyElementSym,
    String enemyTraitName,
    String enemyStatus,
    boolean enemyEnraged,
    String stageInfo,
    List<String> battleLog,
    List<MageInfo> roster,
    Map<CannonballType, Integer> ammo,
    int potions,
    List<CommandInfo> availableCommands,
    CommandInfo selectedCommand
) {}
