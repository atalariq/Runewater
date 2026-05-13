package com.battleship.system;

import java.util.*;
import com.battleship.model.PlayerShip;

/**
 * Sistem reward terinspirasi Balatro — pool besar kartu, 3 dipilih acak
 * dengan weighted probability setiap kali pemain menang.
 *
 * Kartu langka (weight rendah) bisa muncul kapan saja, menciptakan momen
 * kejutan dan mendorong pemain untuk beradaptasi.
 *
 * ENUM Type berada di dalam kelas ini karena tightly coupled —
 * tidak ada kelas lain yang perlu tahu Type tanpa konteks RewardCard.
 */
public class RewardCard {

    public enum Type {
        RECRUIT_MAGE,           // Tambah Mage baru ke roster
        HEAL_SMALL,             // Heal 45 HP
        HEAL_LARGE,             // Heal 80 HP
        UPGRADE_CANNON,         // +7 base damage permanen
        UPGRADE_MAGE_POWER,     // +18 magic power ke Mage pilihan
        ADD_POTION,             // +1 potion (maks 3)
        ADD_EXPLOSIVE,          // +3 Peluru Ledak
        ADD_CHAIN,              // +3 Peluru Rantai
        ADD_GRAPESHOT,          // +3 Peluru Angin
        UPGRADE_ALL_MAGE_SMALL, // +5 power ke SEMUA Mage
        DOUBLE_CANNON_DMG       // [LANGKA] Cannon x2 di battle berikutnya
    }

    private final Type   type;
    private final String title;
    private final String description;
    private final int    weight;

    private RewardCard(Type type, String title, String description, int weight) {
        this.type        = type;
        this.title       = title;
        this.description = description;
        this.weight      = weight;
    }

    public Type   getType()        { return type; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public int    getWeight()      { return weight; }

    public String getDisplay() {
        return String.format("%-28s -- %s", title, description);
    }

    // -------------------------------------------------------------------------
    // Pool & Drawing
    // -------------------------------------------------------------------------

    public static List<RewardCard> buildPool() {
        List<RewardCard> pool = new ArrayList<>();

        pool.add(new RewardCard(Type.RECRUIT_MAGE,
                "Rekrut Mage Baru",      "Tambah Mage acak ke roster (maks 5).",          10));
        pool.add(new RewardCard(Type.HEAL_SMALL,
                "Perbaikan Cepat",       "Pulihkan 45 HP.",                               12));
        pool.add(new RewardCard(Type.HEAL_LARGE,
                "Perbaikan Total",       "Pulihkan 80 HP.",                                6));
        pool.add(new RewardCard(Type.UPGRADE_CANNON,
                "Upgrade Meriam",        "+7 Base Damage permanen.",                      10));
        pool.add(new RewardCard(Type.UPGRADE_MAGE_POWER,
                "Latih Mage",            "+18 Magic Power ke Mage pilihan.",               9));
        pool.add(new RewardCard(Type.ADD_POTION,
                "Stok Potion",           "+1 Potion (heal 50 HP, maks 3).",               8));
        pool.add(new RewardCard(Type.ADD_EXPLOSIVE,
                "Amunisi Ledak x3",      "+3 Peluru Ledak (damage x2.5).",               8));
        pool.add(new RewardCard(Type.ADD_CHAIN,
                "Amunisi Rantai x3",     "+3 Peluru Rantai (WEAKENED 3 turn).",           8));
        pool.add(new RewardCard(Type.ADD_GRAPESHOT,
                "Amunisi Angin x3",      "+3 Peluru Angin (BURNED DoT).",                8));
        pool.add(new RewardCard(Type.UPGRADE_ALL_MAGE_SMALL,
                "Ritual Kolektif",       "+5 Magic Power ke SEMUA Mage di roster.",       4));
        pool.add(new RewardCard(Type.DOUBLE_CANNON_DMG,
                "[LANGKA] Kapal Berkibar","Cannon damage x2 di battle berikutnya.",       2));

        return pool;
    }

    /**
     * Pilih 3 kartu unik dari pool menggunakan weighted random sampling.
     * Kartu yang syaratnya tidak terpenuhi difilter agar tidak muncul.
     */
    public static List<RewardCard> drawThree(Random rng, PlayerShip player) {
        List<RewardCard> pool = buildPool();

        pool.removeIf(card ->
            (card.type == Type.RECRUIT_MAGE && player.getMageCount() >= 5) ||
            (card.type == Type.ADD_POTION   && player.getPotions()   >= 3)
        );

        List<RewardCard>  drawn      = new ArrayList<>();
        Set<RewardCard.Type> selected = new HashSet<>();
        int maxAttempts = pool.size() * 4;
        int attempt     = 0;

        while (drawn.size() < 3 && !pool.isEmpty() && attempt < maxAttempts) {
            attempt++;
            int totalWeight = 0;
            for (RewardCard c : pool) totalWeight += c.weight;

            int roll       = rng.nextInt(totalWeight);
            int cumulative = 0;

            for (RewardCard card : pool) {
                cumulative += card.weight;
                if (roll < cumulative && !selected.contains(card.type)) {
                    drawn.add(card);
                    selected.add(card.type);
                    break;
                }
            }
        }

        // Fallback: pastikan selalu ada 3 pilihan
        for (RewardCard card : pool) {
            if (drawn.size() >= 3) break;
            if (!selected.contains(card.type)) {
                drawn.add(card);
                selected.add(card.type);
            }
        }

        return drawn;
    }
}
