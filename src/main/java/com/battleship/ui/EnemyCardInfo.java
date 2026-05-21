package com.battleship.ui;

public record EnemyCardInfo(
    String name,
    int hp,
    int maxHp,
    String elementSym,
    String traitName,
    int bounty,
    int damage,
    boolean isElite
) {}
