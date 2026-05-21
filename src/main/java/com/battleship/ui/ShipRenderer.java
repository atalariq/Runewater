package com.battleship.ui;

import java.util.ArrayList;
import java.util.List;

import com.battleship.model.BossShip;
import com.battleship.model.EnemyShip;
import com.battleship.model.Mage;
import com.battleship.model.PlayerShip;
import com.battleship.model.enums.StatusEffect;

public final class ShipRenderer {

    private ShipRenderer() {}

    // -------------------------------------------------------------------------
    // Mage Roster Rendering
    // -------------------------------------------------------------------------

    public static List<String> renderMageRosterSelectable(List<Mage> roster, boolean showSpell) {
        List<String> lines = new ArrayList<>();
        if (roster.isEmpty()) {
            lines.add("    (Tidak ada Mage di roster)");
            return lines;
        }
        for (int i = 0; i < roster.size(); i++) {
            lines.add(String.format("    [%d] %s", i + 1, roster.get(i).getInfo(showSpell)));
        }
        return lines;
    }

    public static List<String> renderMageRosterPlain(List<Mage> roster, boolean showSpell) {
        List<String> lines = new ArrayList<>();
        if (roster.isEmpty()) {
            lines.add("    (Tidak ada Mage di roster)");
            return lines;
        }
        for (Mage mage : roster) {
            lines.add("    - " + mage.getInfo(showSpell));
        }
        return lines;
    }

    // -------------------------------------------------------------------------
    // Player Ship Rendering
    // -------------------------------------------------------------------------

    public static String renderPlayerDescription(PlayerShip ship) {
        return String.format(
            "Kapal: %s | HP: %d/%d | Cannon: %d | Mage: %d/%d | Potion: %d | Bounty: $%d",
            ship.getName(), ship.getCurrentHp(), ship.getMaxHp(), ship.getBaseDamage(),
            ship.getMageCount(), 5, ship.getPotions(), ship.getBounty());
    }

    public static String renderPlayerStatus(PlayerShip ship) {
        String shieldTag = ship.isShielded() ? " [SHIELD]" : "";
        return String.format(
            "  [PLAYER] %s%s | HP: %s | Cannon: %d | Mage: %d/%d | Potion: %d | $%d",
            ship.getName(), shieldTag, renderHpBar(ship.getCurrentHp(), ship.getMaxHp(), ship.getStatus()),
            ship.getBaseDamage(), ship.getMageCount(), 5, ship.getPotions(), ship.getBounty());
    }

    // -------------------------------------------------------------------------
    // Enemy Ship Rendering
    // -------------------------------------------------------------------------

    public static String renderEnemyDescription(EnemyShip ship) {
        if (ship instanceof BossShip boss) {
            String rageTag = boss.isEnraged() ? " *** ENRAGED! ***" : "";
            return String.format(
                "  [BOSS] %s -- %-18s%s%n  HP: %s | Elemen: %-5s | Dmg: ~%d | Sifat: %s",
                boss.getBossTitle(), boss.getName(), rageTag,
                renderHpBar(boss.getCurrentHp(), boss.getMaxHp(), boss.getStatus()),
                boss.getElement().sym(), boss.getBaseDamage(), boss.getTrait().displayName);
        }
        String statusTag = (ship.getStatus() != StatusEffect.NONE) ? ship.getStatus().getTag() : "";
        return String.format(
            "  %-24s | %s%s | Elemen: %-5s | Dmg: ~%d | Sifat: %-12s | Bounty: $%d",
            ship.getName(), renderHpBar(ship.getCurrentHp(), ship.getMaxHp(), ship.getStatus()),
            statusTag, ship.getElement().sym(), ship.getBaseDamage(),
            ship.getTrait().displayName, ship.getBountyReward());
    }

    public static String renderEnemyStatus(EnemyShip ship) {
        return renderEnemyDescription(ship);
    }

    // -------------------------------------------------------------------------
    // HP Bar
    // -------------------------------------------------------------------------

    public static String renderHpBar(int currentHp, int maxHp, StatusEffect status) {
        int barLength = 20;
        int safeMax = Math.max(1, maxHp);
        int filled = (int) ((double) currentHp / safeMax * barLength);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) bar.append(i < filled ? "#" : ".");
        bar.append("]");
        String statusTag = (status != StatusEffect.NONE) ? " " + status.getTag() : "";
        return String.format("%s %d/%d%s", bar, currentHp, maxHp, statusTag);
    }
}
