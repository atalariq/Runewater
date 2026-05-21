package com.battleship.engine;

import java.util.List;

public record TurnResult(
    String playerHpBar,
    String enemyHpBar,
    boolean playerTurn,
    List<String> battleLog,
    boolean battleOver,
    boolean playerWon,
    String actionDescription
) {}
