package com.battleship.engine;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.battleship.model.enums.StatusEffect;

public record DamageResult(
    int finalDamage,
    int rawDamage,
    Map<String, Double> multiplierBreakdown,
    int reflectedDamage,
    List<StatusEffect> statusEffectsApplied
) {

    public boolean hasReflectedDamage() {
        return reflectedDamage > 0;
    }

    public Map<String, Double> multiplierBreakdown() {
        return Collections.unmodifiableMap(multiplierBreakdown);
    }

    public List<StatusEffect> statusEffectsApplied() {
        return Collections.unmodifiableList(statusEffectsApplied);
    }
}
