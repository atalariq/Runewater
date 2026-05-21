package com.battleship.engine;

import com.battleship.model.enums.CannonballType;

public sealed interface Command permits Command.Attack, Command.Cannonball, Command.Spell, Command.Defend, Command.Potion {
    record Attack(int mageIndex) implements Command {}
    record Cannonball(CannonballType type) implements Command {}
    record Spell(int mageIndex) implements Command {}
    record Defend() implements Command {}
    record Potion() implements Command {}
}
