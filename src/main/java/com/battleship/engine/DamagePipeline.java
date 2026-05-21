package com.battleship.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.StatusEffect;

public final class DamagePipeline {

    private DamagePipeline() {}

    public static DamageResult resolve(AttackContext ctx) {
        double totalMult = 1.0;
        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("base", 1.0);

        if (ctx.attackType() == AttackType.MAGIC) {
            breakdown.put("element", ctx.elementMultiplier());
            totalMult *= ctx.elementMultiplier();

            breakdown.put("synergy", ctx.synergyMultiplier());
            totalMult *= ctx.synergyMultiplier();
        }

        if (ctx.attacker().isWeakened()) {
            breakdown.put("weakened", 0.70);
            totalMult *= 0.70;
        }

        if (ctx.isBerserkerActive()) {
            breakdown.put("berserker", 1.50);
            totalMult *= 1.50;
        }

        if (ctx.attackType() == AttackType.PHYSICAL
            && ctx.defenderTrait() == EnemyTrait.ARMORED) {
            breakdown.put("armored", 0.65);
            totalMult *= 0.65;
        }

        int finalDamage = (int) (ctx.baseDamage() * totalMult);

        int reflectedDamage = 0;
        if (ctx.attackType() == AttackType.MAGIC
            && ctx.defenderTrait() == EnemyTrait.THORNS) {
            reflectedDamage = (int) (finalDamage * 0.15);
        }

        List<StatusEffect> statuses = new ArrayList<>();
        if (ctx.cannonballType() != null) {
            if (ctx.cannonballType().appliesBurn) {
                statuses.add(StatusEffect.BURNED);
            }
            if (ctx.cannonballType().appliesWeak) {
                statuses.add(StatusEffect.WEAKENED);
            }
        }

        return new DamageResult(finalDamage, ctx.baseDamage(), breakdown, reflectedDamage, statuses);
    }
}
