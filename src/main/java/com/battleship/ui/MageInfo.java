package com.battleship.ui;

public record MageInfo(
    String name,
    int level,
    String elementSym,
    int power,
    int xp,
    String spellTypeName,
    boolean spellUsed
) {}
