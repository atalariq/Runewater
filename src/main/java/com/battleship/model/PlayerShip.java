package com.battleship.model;

import java.util.*;
import com.battleship.interfaces.Describable;
import com.battleship.interfaces.MagicCastable;
import com.battleship.model.enums.*;

/**
 * Kapal yang dikendalikan pemain. Menyimpan roster Mage (maks 5),
 * inventori cannonball, potion, dan mengelola sistem sinergy elemen.
 */
public class PlayerShip extends Ship implements MagicCastable, Describable {

    private final List<Mage>                  roster;
    private static final int                  MAX_MAGE    = 5;

    private final Map<CannonballType, Integer> ammo;
    private static final int                  MAX_AMMO    = 8;

    private       int     bounty;
    private       int     potions;
    private       boolean shieldActive;
    private static final int MAX_POTIONS = 3;
    private static final int POTION_HEAL = 50;

    public PlayerShip(String name, int maxHp, int baseDamage, Element element) {
        super(name, maxHp, baseDamage, element);
        roster       = new ArrayList<>();
        ammo         = new EnumMap<>(CannonballType.class);
        bounty       = 0;
        potions      = 2;
        shieldActive = false;

        ammo.put(CannonballType.IRON,      -1);
        ammo.put(CannonballType.EXPLOSIVE,  3);
        ammo.put(CannonballType.CHAIN,      3);
        ammo.put(CannonballType.GRAPESHOT,  3);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public List<Mage> getRoster()   { return Collections.unmodifiableList(roster); }
    public int  getMageCount()      { return roster.size(); }
    public int  getBounty()         { return bounty; }
    public int  getPotions()        { return potions; }
    public boolean isShielded()     { return shieldActive; }

    public int getAmmoCount(CannonballType type) {
        Integer val = ammo.get(type);
        return val == null ? 0 : val;
    }

    // -------------------------------------------------------------------------
    // Mutators
    // -------------------------------------------------------------------------

    public void addBounty(int amount)         { bounty += amount; }
    public void heal(int amount)              { setCurrentHp(getCurrentHp() + amount); }
    public void upgradeBaseDamage(int amount) { setBaseDamage(getBaseDamage() + amount); }
    public void activateShield()              { shieldActive = true; }
    public void resetShield()                 { shieldActive = false; }

    @Override
    public void takeDamage(int damage) {
        super.takeDamage(shieldActive ? damage / 2 : damage);
    }

    public boolean usePotion() {
        if (potions <= 0) return false;
        potions--;
        heal(POTION_HEAL);
        return true;
    }

    public boolean addPotion() {
        if (potions >= MAX_POTIONS) return false;
        potions++;
        return true;
    }

    public boolean recruitMage(Mage mage) {
        if (roster.size() >= MAX_MAGE) return false;
        roster.add(mage);
        return true;
    }

    public void addAmmo(CannonballType type, int amount) {
        if (type == CannonballType.IRON) return;
        int current = getAmmoCount(type);
        ammo.put(type, Math.min(MAX_AMMO, current + amount));
    }

    public void resetAllSpells() {
        for (Mage m : roster) m.resetSpell();
    }

    public List<String> grantXpToAll(int xp) {
        List<String> messages = new ArrayList<>();
        for (Mage m : roster) {
            if (m.gainXp(xp)) {
                messages.add(String.format("  *** LEVEL UP! %s -> Lv.%d (Pwr: %d)",
                        m.getName(), m.getLevel(), m.getMagicPower()));
            }
        }
        return messages;
    }

    // -------------------------------------------------------------------------
    // Sinergy System
    // -------------------------------------------------------------------------

    public double getSynergyMult(Element element) {
        int count = countMageByElement(element);
        return count >= 3 ? 1.40 : count >= 2 ? 1.20 : 1.00;
    }

    public int countMageByElement(Element element) {
        int count = 0;
        for (Mage m : roster) {
            if (m.getElement() == element) count++;
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Cannonball System
    // -------------------------------------------------------------------------

    public int fireCannonball(CannonballType type, Ship target) {
        int stock = getAmmoCount(type);
        if (type != CannonballType.IRON && stock <= 0) return -1;

        if (type != CannonballType.IRON) ammo.put(type, stock - 1);

        int damage = effectiveDamage((int)(getBaseDamage() * type.dmgMult));

        if (type.appliesBurn) target.applyStatus(StatusEffect.BURNED,   3);
        if (type.appliesWeak) target.applyStatus(StatusEffect.WEAKENED, 3);

        target.takeDamage(damage);
        return damage;
    }

    // -------------------------------------------------------------------------
    // MagicCastable Implementation
    // -------------------------------------------------------------------------

    @Override
    public int castMagic(Ship target, Mage mage) {
        double elemMult    = mage.getElement().getMultiplier(target.getElement());
        double synergyMult = getSynergyMult(mage.getElement());
        int    rawDamage   = (int)((getBaseDamage() + mage.getMagicPower()) * elemMult * synergyMult);
        int    damage      = effectiveDamage(rawDamage);
        target.takeDamage(damage);
        return damage;
    }

    @Override
    public String castSpell(Ship target, Mage mage) {
        if (mage.isSpellUsed()) {
            return "  [!] Jurus " + mage.getSpellType().getDisplayName() + " sudah dipakai battle ini!";
        }

        mage.markSpellUsed();
        double synergyMult = getSynergyMult(mage.getElement());
        int    basePower   = getBaseDamage() + mage.getMagicPower();
        StringBuilder result = new StringBuilder();

        switch (mage.getSpellType()) {
            case INFERNO: {
                int damage = (int)(basePower * 3.0 * synergyMult);
                target.takeDamage(damage);
                result.append(String.format(
                        "  *** INFERNO! %s membakar segalanya! Damage: %d ***",
                        mage.getName(), damage));
                break;
            }
            case IGNITE: {
                double elemMult = mage.getElement().getMultiplier(target.getElement());
                int    damage   = (int)(basePower * elemMult * synergyMult);
                target.takeDamage(damage);
                target.applyStatus(StatusEffect.BURNED, 3);
                result.append(String.format(
                        "  *** IGNITE! Damage: %d + %s BURNED 3 turn! ***",
                        damage, target.getName()));
                break;
            }
            case TIDAL_WAVE: {
                double elemMult = mage.getElement().getMultiplier(target.getElement());
                int    damage   = (int)(basePower * 2.0 * elemMult * synergyMult);
                target.takeDamage(damage);
                int hpBefore = getCurrentHp();
                heal(35);
                result.append(String.format(
                        "  *** TIDAL WAVE! Damage: %d + Healed +%d HP ***",
                        damage, getCurrentHp() - hpBefore));
                break;
            }
            case FREEZE: {
                double elemMult = mage.getElement().getMultiplier(target.getElement());
                int    damage   = (int)(basePower * elemMult * synergyMult);
                target.takeDamage(damage);
                target.applyStatus(StatusEffect.FROZEN, 1);
                result.append(String.format(
                        "  *** FREEZE! Damage: %d + %s FROZEN (skip + kebal status)! ***",
                        damage, target.getName()));
                break;
            }
            case CHAIN_BOLT: {
                double elemMult = mage.getElement().getMultiplier(target.getElement());
                int    damage   = (int)(basePower * 2.5 * elemMult * synergyMult);
                target.takeDamage(damage);
                target.applyStatus(StatusEffect.WEAKENED, 3);
                result.append(String.format(
                        "  *** CHAIN BOLT! Damage: %d + %s WEAKENED 3 turn! ***",
                        damage, target.getName()));
                break;
            }
            case OVERCHARGE: {
                result.append("  *** OVERCHARGE! Semua Mage menyerang serentak!\n");
                int totalDamage = 0;
                for (Mage m : roster) {
                    double elemMult = m.getElement().getMultiplier(target.getElement());
                    int    hit      = (int)((getBaseDamage() + m.getMagicPower()) * 1.2 * elemMult);
                    target.takeDamage(hit);
                    totalDamage += hit;
                    result.append(String.format("    - %s (%s): %d damage%n",
                            m.getName(), m.getElement().sym(), hit));
                    if (!target.isAlive()) break;
                }
                result.append(String.format("  Total OVERCHARGE: %d damage!", totalDamage));
                break;
            }
            default:
                result.append("  (Jurus tidak dikenali)");
        }

        return result.toString();
    }

    @Override
    public boolean hasCounterMage(Element enemyElement) {
        Element counter = enemyElement.weakness();
        for (Mage m : roster) {
            if (m.getElement() == counter) return true;
        }
        return false;
    }

    @Override
    public void displayMageRoster(boolean showSpell) {
        for (String line : getSelectableMageRosterLines(showSpell)) {
            System.out.println(line);
        }
    }

    public List<String> getMageRosterLines(boolean showSpell) {
        return getSelectableMageRosterLines(showSpell);
    }

    public List<String> getSelectableMageRosterLines(boolean showSpell) {
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

    public List<String> getMageDisplayLines(boolean showSpell) {
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
    // Describable Implementation
    // -------------------------------------------------------------------------

    @Override
    public String getFullDescription() {
        return String.format(
            "Kapal: %s | HP: %d/%d | Cannon: %d | Mage: %d/%d | Potion: %d | Bounty: $%d",
            getName(), getCurrentHp(), getMaxHp(), getBaseDamage(), roster.size(), MAX_MAGE, potions, bounty);
    }

    // -------------------------------------------------------------------------
    // Ship Abstract Methods
    // -------------------------------------------------------------------------

    @Override
    public void takeTurn(Ship opponent) {
        // Input dikelola di GameManager.doPlayerTurn()
    }

    @Override
    public String getStatusDisplay() {
        String shieldTag = shieldActive ? " [SHIELD]" : "";
        return String.format(
            "  [PLAYER] %s%s | HP: %s | Cannon: %d | Mage: %d/%d | Potion: %d | $%d",
            getName(), shieldTag, getHpBar(), getBaseDamage(),
            roster.size(), MAX_MAGE, potions, bounty);
    }
}
