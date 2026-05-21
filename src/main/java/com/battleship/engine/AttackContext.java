package com.battleship.engine;

import com.battleship.model.Ship;
import com.battleship.model.enums.CannonballType;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.SpellType;

public record AttackContext(
    Ship attacker,
    Ship defender,
    AttackType attackType,
    int baseDamage,
    double elementMultiplier,
    double synergyMultiplier,
    CannonballType cannonballType,
    SpellType spellType,
    Element mageElement,
    EnemyTrait defenderTrait,
    boolean isBerserkerActive
) {}
