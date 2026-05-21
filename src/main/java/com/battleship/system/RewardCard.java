package com.battleship.system;

import java.util.*;
import com.battleship.model.Mage;
import com.battleship.model.PlayerShip;
import com.battleship.model.enums.CannonballType;
import com.battleship.model.enums.Element;

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
    // Effect Application
    // -------------------------------------------------------------------------

    private static final String[] MAGE_NAMES = {
        "Ignis", "Aquara", "Voltus", "Pyra", "Hydra", "Tempest",
        "Ember", "Cascade", "Thunder", "Inferno", "Torrent", "Zephyr",
        "Cinder", "Deluge", "Gale", "Blaze", "Surge", "Frost", "Ash", "Mist"
    };

    private static Mage randomMage(int basePower, Random rng) {
        return new Mage(
            MAGE_NAMES[rng.nextInt(MAGE_NAMES.length)],
            Element.values()[rng.nextInt(Element.values().length)],
            basePower, rng
        );
    }

    /**
     * Terapkan efek kartu reward ke PlayerShip.
     * Semua tipe kecuali UPGRADE_MAGE_POWER diproses di sini.
     * (UPGRADE_MAGE_POWER lewat sini hanya saat roster kosong → fallback heal.)
     */
    public List<String> apply(PlayerShip player) {
        List<String> lines = new ArrayList<>();
        Random rng = new Random();

        switch (type) {
            case RECRUIT_MAGE -> {
                if (player.getMageCount() >= 5) {
                    player.heal(45);
                    lines.add("Roster penuh. Reward diganti heal kecil.");
                    break;
                }
                Mage newMage = randomMage(18 + rng.nextInt(18), rng);
                player.recruitMage(newMage);
                lines.add("Mage baru bergabung: " + newMage.getInfo(true));
                lines.add("Jurus: " + newMage.getSpellType().getFullDescription());
                int count = player.countMageByElement(newMage.getElement());
                if (count >= 2) {
                    lines.add(String.format("Sinergy terbentuk: %dx %s -> +%d%% magic damage",
                            count, newMage.getElement().sym(), count >= 3 ? 40 : 20));
                }
            }
            case HEAL_SMALL -> {
                int before = player.getCurrentHp();
                player.heal(45);
                lines.add(String.format("Perbaikan cepat. HP: %d -> %d", before, player.getCurrentHp()));
            }
            case HEAL_LARGE -> {
                int before = player.getCurrentHp();
                player.heal(80);
                lines.add(String.format("Perbaikan total. HP: %d -> %d", before, player.getCurrentHp()));
            }
            case UPGRADE_CANNON -> {
                int before = player.getBaseDamage();
                player.upgradeBaseDamage(7);
                lines.add(String.format("Meriam di-upgrade. Damage: %d -> %d", before, player.getBaseDamage()));
            }
            case UPGRADE_MAGE_POWER -> {
                // Fallback ketika roster kosong (dipanggil dari GameManager)
                player.heal(45);
                lines.add("Tidak ada Mage. Reward diganti heal kecil.");
            }
            case ADD_POTION -> {
                if (!player.addPotion()) {
                    player.heal(45);
                    lines.add("Potion penuh. Reward diganti heal kecil.");
                } else {
                    lines.add(String.format("+1 Potion. Sisa sekarang: %d/3", player.getPotions()));
                }
            }
            case ADD_EXPLOSIVE -> {
                player.addAmmo(CannonballType.EXPLOSIVE, 3);
                lines.add("Amunisi Ledak +3. Stok: " + player.getAmmoCount(CannonballType.EXPLOSIVE));
            }
            case ADD_CHAIN -> {
                player.addAmmo(CannonballType.CHAIN, 3);
                lines.add("Amunisi Rantai +3. Stok: " + player.getAmmoCount(CannonballType.CHAIN));
            }
            case ADD_GRAPESHOT -> {
                player.addAmmo(CannonballType.GRAPESHOT, 3);
                lines.add("Amunisi Angin +3. Stok: " + player.getAmmoCount(CannonballType.GRAPESHOT));
            }
            case UPGRADE_ALL_MAGE_SMALL -> {
                for (Mage mage : player.getRoster()) {
                    mage.upgradePower(5);
                }
                lines.add("Ritual kolektif aktif. Semua Mage +5 Magic Power.");
            }
            case DOUBLE_CANNON_DMG -> {
                lines.add("Kapal Berkibar aktif. Cannon damage x2 di battle berikutnya.");
            }
        }
        return lines;
    }

    /**
     * Terapkan UPGRADE_MAGE_POWER ke Mage spesifik di index tertentu.
     * Hanya dipanggil ketika roster tidak kosong (GUI sudah memilih Mage).
     */
    public List<String> applyMageUpgrade(PlayerShip player, int mageIndex) {
        List<String> lines = new ArrayList<>();
        Mage mage = player.getRoster().get(mageIndex);
        int before = mage.getMagicPower();
        mage.upgradePower(18);
        lines.add(String.format("%s di-upgrade. Power: %d -> %d", mage.getName(), before, mage.getMagicPower()));
        return lines;
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
