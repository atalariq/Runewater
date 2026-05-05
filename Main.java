import java.util.*;
import java.util.Scanner;

// =============================================================================
// FILE   : Main.java  (Single-file edition)
// GAME   : Roguelike Pirate Mage Battle  (v4 -- Strategic Edition)
// COMPILE: javac Main.java
// RUN    : java Main
//
// STRUKTUR (urutan class dalam file ini):
//  Interfaces  : Describable, MagicCastable
//  Enums       : Element, StatusEffect, EnemyTrait, SpellType, CannonballType
//  Classes     : Mage, Ship (abstract), PlayerShip, EnemyShip, BossShip
//  Support     : RewardCard, BalanceConfig, InputHelper
//  Engine      : GameManager
//  Entry Point : Main
// =============================================================================

// =============================================================================
// FILE: Describable.java
// IMPLEMENTASI: Interface
// Kontrak untuk semua entitas yang bisa mendeskripsikan dirinya sendiri.
// Diimplementasikan oleh: CannonballType, SpellType, PlayerShip, EnemyShip
// =============================================================================
interface Describable {
    String getFullDescription();
}

// =============================================================================
// FILE: MagicCastable.java
// IMPLEMENTASI: Interface
// Kontrak untuk kapal yang bisa melakukan sihir dan jurus.
// Diimplementasikan oleh: PlayerShip
// =============================================================================
interface MagicCastable {
    int     castMagic(Ship target, Mage mage);
    String  castSpell(Ship target, Mage mage);
    boolean hasCounterMage(Element enemyElement);
    void    displayMageRoster(boolean showSpell);
}

// =============================================================================
// FILE: Element.java
// Tiga elemen sihir dengan logika segitiga: FIRE > STORM > WATER > FIRE
// Setiap elemen tahu multiplier-nya terhadap elemen lain.
// =============================================================================
enum Element {
    FIRE, WATER, STORM;

    /** Multiplier damage: 2.0 super effective, 0.5 not effective, 1.0 netral. */
    public double getMultiplier(Element defender) {
        if (this == FIRE  && defender == STORM) return 2.0;
        if (this == STORM && defender == WATER) return 2.0;
        if (this == WATER && defender == FIRE)  return 2.0;
        if (this == FIRE  && defender == WATER) return 0.5;
        if (this == STORM && defender == FIRE)  return 0.5;
        if (this == WATER && defender == STORM) return 0.5;
        return 1.0;
    }

    /** Elemen yang mengalahkan elemen ini. */
    public Element weakness() {
        return this == FIRE ? WATER : this == WATER ? STORM : FIRE;
    }

    /** Simbol pendek 5 karakter untuk UI. */
    public String sym() {
        return this == FIRE ? "FIRE " : this == WATER ? "WATER" : "STORM";
    }

    /** Teks efektivitas serangan untuk ditampilkan ke pemain. */
    public String effectText(Element defender) {
        double m = getMultiplier(defender);
        return m > 1 ? "SUPER EFFECTIVE! (x2.0)" : m < 1 ? "Not Effective (x0.5)" : "Netral (x1.0)";
    }
}

// =============================================================================
// FILE: StatusEffect.java
// Efek status yang bisa menempel ke kapal selama pertempuran.
//
//  BURNED   — Kehilangan 8 HP per giliran selama beberapa turn (DoT).
//  WEAKENED — Damage keluar berkurang 30% selama beberapa turn.
//  FROZEN   — Skip giliran berikutnya DAN kebal terhadap status lain.
// =============================================================================
enum StatusEffect {
    NONE, BURNED, WEAKENED, FROZEN;

    /** Tag singkat untuk ditampilkan di HP bar. */
    public String getTag() {
        switch (this) {
            case BURNED:   return "[BURNED]";
            case WEAKENED: return "[WEAKENED]";
            case FROZEN:   return "[FROZEN]";
            default:       return "";
        }
    }
}

// =============================================================================
// FILE: EnemyTrait.java
// Sifat pasif musuh. Muncul acak setiap stage, memaksa pemain
// membaca situasi dan menyesuaikan strategi.
//
//  ARMORED   — Semua damage fisik (cannon) berkurang 35%.
//              Counter: utamakan sihir Mage.
//  REGENERATE— Pulihkan 10 HP di awal gilirannya.
//              Counter: burst damage tinggi dalam sedikit turn.
//  BERSERKER — Damage +50% saat HP < 50% (fase amuk).
//              Counter: bunuh sebelum HP-nya drop ke 50%.
//  THORNS    — Memantulkan 15% damage sihir kembali ke pemain.
//              Counter: cannon atau jurus non-sihir (Inferno, Explosive).
// =============================================================================
enum EnemyTrait {
    NONE      ("Biasa",       "Tidak ada sifat khusus."),
    ARMORED   ("Berzirah",    "Semua damage fisik (cannon) berkurang 35%."),
    REGENERATE("Regenerasi",  "Pulihkan 10 HP di awal gilirannya."),
    BERSERKER ("Berserker",   "Damage +50% saat HP < 50% (fase amuk)."),
    THORNS    ("Berduri",     "Memantulkan 15% damage sihir kembali ke pemain.");

    final String displayName, description;

    EnemyTrait(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}

// =============================================================================
// FILE: SpellType.java
// IMPLEMENTASI: Enum dengan data dan behavior, implements Describable
//
// 6 jurus total, 2 per elemen. Setiap Mage mendapat satu jurus saat dibuat.
// Jurus hanya bisa dipakai SEKALI per battle — keputusan kapan memakainya
// adalah salah satu keputusan strategis utama dalam game.
//
//  INFERNO    [FIRE]  — Damage x3.0. Mage kelelahan (jurus hangus).
//  IGNITE     [FIRE]  — Damage normal + BURNED 3 turn (DoT 8 HP/turn).
//  TIDAL_WAVE [WATER] — Damage x2.0 + heal diri sendiri 35 HP.
//  FREEZE     [WATER] — Damage normal + FROZEN 1 turn (skip + kebal status).
//  CHAIN_BOLT [STORM] — Damage x2.5 + WEAKENED musuh 3 turn (-30% dmg).
//  OVERCHARGE [STORM] — Setiap Mage di roster menyerang x1.2 masing-masing.
// =============================================================================
enum SpellType implements Describable {
    // FIRE spells
    INFERNO   ("Inferno",    "[FIRE]  Damage x3.0. Mage kelelahan (jurus hangus)."),
    IGNITE    ("Ignite",     "[FIRE]  Damage normal + BURNED 3 turn (DoT)."),
    // WATER spells
    TIDAL_WAVE("Tidal Wave", "[WATER] Damage x2.0 + heal diri 35 HP."),
    FREEZE    ("Freeze",     "[WATER] Damage normal + FROZEN 1 turn (skip + kebal status)."),
    // STORM spells
    CHAIN_BOLT("Chain Bolt", "[STORM] Damage x2.5 + WEAKENED musuh 3 turn (-30% dmg)."),
    OVERCHARGE("Overcharge", "[STORM] Tiap Mage di roster menyerang x1.2 masing-masing.");

    private final String displayName;
    private final String description;

    SpellType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }

    // IMPLEMENTASI: Method Overriding dari interface Describable
    @Override
    public String getFullDescription() {
        return String.format("%-12s — %s", displayName, description);
    }
}

// =============================================================================
// FILE: CannonballType.java
// IMPLEMENTASI: Enum dengan data dan behavior, implements Describable
//
// Inti dari perbaikan sistem Cannon di v4. Setiap tipe peluru punya
// TUJUAN STRATEGIS berbeda — bukan sekadar angka damage yang berbeda.
// Ini yang membuat aksi "Tembak Meriam" selalu relevan.
//
//  IRON      — Stok tak terbatas. Fallback aman kapan saja.
//              Pilihan terbaik: musuh hampir mati, atau hemat ammo langka.
//
//  EXPLOSIVE — Damage x2.5. Stok terbatas, harus diisi dari reward.
//              Pilihan terbaik: burst kill boss atau musuh high-HP.
//
//  CHAIN     — Damage rendah (x0.6), tapi WEAKENED 3 turn (-30% dmg musuh).
//              Pilihan terbaik: boss high-damage, beli waktu untuk strategi.
//
//  GRAPESHOT — Damage menengah (x0.85) + BURNED DoT 8 HP/turn selama 3 turn.
//              Pilihan terbaik: tidak punya Mage FIRE, atau musuh ARMORED.
// =============================================================================
enum CannonballType implements Describable {
    IRON     ("Peluru Besi",   "Damage solid. Stok tak terbatas.",                  1.0,  false, false),
    EXPLOSIVE("Peluru Ledak",  "Damage x2.5. Stok terbatas — pakai bijak!",         2.5,  false, false),
    CHAIN    ("Peluru Rantai", "Damage rendah. WEAKENED musuh 3 turn (-30% dmg).",  0.6,  false, true ),
    GRAPESHOT("Peluru Angin",  "Damage menengah + BURNED musuh 3 turn (DoT 8/t).",  0.85, true,  false);

    // IMPLEMENTASI: Encapsulation — field enum private/package-private
    private final String displayName;
    private final String description;
    final double  dmgMult;      // perkalian damage vs base cannon
    final boolean appliesBurn;  // apakah menerapkan status BURNED ke target
    final boolean appliesWeak;  // apakah menerapkan status WEAKENED ke target

    CannonballType(String displayName, String description,
                   double dmgMult, boolean appliesBurn, boolean appliesWeak) {
        this.displayName  = displayName;
        this.description  = description;
        this.dmgMult      = dmgMult;
        this.appliesBurn  = appliesBurn;
        this.appliesWeak  = appliesWeak;
    }

    public String getDisplayName() { return displayName; }

    // IMPLEMENTASI: Method Overriding dari interface Describable
    @Override
    public String getFullDescription() {
        return String.format("%-14s — %s", displayName, description);
    }
}

// =============================================================================
// FILE: Mage.java
// IMPLEMENTASI: Encapsulation (semua field private + getter/setter)
// IMPLEMENTASI: Composition Part — satu Mage dimiliki oleh PlayerShip
//
// Merepresentasikan satu penyihir kru kapal. Setiap Mage punya:
//  - Elemen (FIRE/WATER/STORM) yang menentukan keunggulan vs musuh
//  - SpellType unik yang bisa dipakai sekali per battle
//  - Level & XP yang naik setelah tiap battle dimenangkan
// =============================================================================

class Mage {

    // IMPLEMENTASI: Encapsulation — semua field private
    private final String    name;
    private final Element   element;
    private final SpellType spellType;
    private       int       magicPower;
    private       int       level;
    private       int       xp;
    private       boolean   spellUsed;  // true = jurus sudah dipakai battle ini

    private static final int XP_PER_LEVEL = 30;

    // Tabel jurus yang tersedia per elemen
    private static final SpellType[] FIRE_SPELLS  = { SpellType.INFERNO,    SpellType.IGNITE      };
    private static final SpellType[] WATER_SPELLS = { SpellType.TIDAL_WAVE, SpellType.FREEZE      };
    private static final SpellType[] STORM_SPELLS = { SpellType.CHAIN_BOLT, SpellType.OVERCHARGE  };

    /**
     * Constructor untuk Mage dengan jurus acak sesuai elemennya.
     * Dipakai saat rekrut Mage dari reward.
     */
    public Mage(String name, Element element, int magicPower, Random rng) {
        this.name       = name;
        this.element    = element;
        this.magicPower = magicPower;
        this.level      = 1;
        this.xp         = 0;
        this.spellUsed  = false;

        SpellType[] pool = element == Element.FIRE  ? FIRE_SPELLS
                         : element == Element.WATER ? WATER_SPELLS
                         : STORM_SPELLS;
        this.spellType = pool[rng.nextInt(pool.length)];
    }

    /**
     * Constructor untuk Mage dengan jurus spesifik.
     * Dipakai saat pemain memilih Mage pertama di awal game.
     */
    public Mage(String name, Element element, int magicPower, SpellType spellType) {
        this.name       = name;
        this.element    = element;
        this.magicPower = magicPower;
        this.level      = 1;
        this.xp         = 0;
        this.spellUsed  = false;
        this.spellType  = spellType;
    }

    // =========================================================================
    // IMPLEMENTASI: Encapsulation — Getter
    // =========================================================================

    public String    getName()        { return name; }
    public Element   getElement()     { return element; }
    public int       getMagicPower()  { return magicPower; }
    public int       getLevel()       { return level; }
    public int       getXp()          { return xp; }
    public SpellType getSpellType()   { return spellType; }
    public boolean   isSpellUsed()    { return spellUsed; }

    // =========================================================================
    // IMPLEMENTASI: Encapsulation — Setter & mutators dengan validasi
    // =========================================================================

    /** Tandai jurus sudah dipakai. Tidak bisa dibatalkan sampai battle selesai. */
    public void markSpellUsed() { this.spellUsed = true; }

    /** Reset status jurus — dipanggil di awal setiap battle baru. */
    public void resetSpell()    { this.spellUsed = false; }

    /** Tambah magic power langsung (dari reward Upgrade Mage). */
    public void upgradePower(int amount) {
        this.magicPower = Math.max(1, magicPower + amount);
    }

    /**
     * Berikan XP ke Mage ini. Jika cukup, otomatis naik level.
     * Setiap level up: magicPower +7.
     * @return true jika naik level, false jika belum.
     */
    public boolean gainXp(int amount) {
        xp += amount;
        if (xp >= XP_PER_LEVEL) {
            xp         -= XP_PER_LEVEL;
            level++;
            magicPower += 7;
            return true;
        }
        return false;
    }

    // =========================================================================
    // Display
    // =========================================================================

    /**
     * Info lengkap satu baris untuk ditampilkan di roster.
     * @param withSpell true = tampilkan nama jurus + status READY/USED.
     */
    public String getInfo(boolean withSpell) {
        String spellPart = withSpell
            ? String.format(" | %s %s",
                spellType.getDisplayName(), spellUsed ? "[USED]" : "[READY]")
            : "";
        return String.format("Lv.%d %s %-8s Pwr:%-3d XP:%d/%d%s",
                level, element.sym(), name, magicPower, xp, XP_PER_LEVEL, spellPart);
    }
}

// =============================================================================
// FILE: Ship.java
// IMPLEMENTASI: Abstract Class (OOP Concept)
// IMPLEMENTASI: Encapsulation (field protected + getter/setter)
// IMPLEMENTASI: Method Overloading — attack(Ship) vs attack(Ship, Mage)
//
// Base class untuk semua kapal dalam game. Mendefinisikan atribut dasar,
// sistem HP, sistem status efek, dan dua variasi serangan (overloading).
// Subclass WAJIB mengimplementasikan takeTurn() dan getStatusDisplay().
// =============================================================================

abstract class Ship {

    // IMPLEMENTASI: Encapsulation — protected agar subclass bisa akses langsung
    protected String       name;
    protected int          maxHp;
    protected int          currentHp;
    protected int          baseDamage;
    protected Element      element;
    protected StatusEffect status;
    protected int          statusDuration; // sisa turn efek BURNED/WEAKENED/FROZEN

    // Satu RNG dibagi semua kapal — tidak perlu banyak instance Random
    protected static final Random RNG = new Random();

    public Ship(String name, int maxHp, int baseDamage, Element element) {
        this.name           = name;
        this.maxHp          = maxHp;
        this.currentHp      = maxHp;
        this.baseDamage     = baseDamage;
        this.element        = element;
        this.status         = StatusEffect.NONE;
        this.statusDuration = 0;
    }

    // =========================================================================
    // IMPLEMENTASI: Encapsulation — Getter
    // =========================================================================

    public String       getName()        { return name; }
    public int          getMaxHp()       { return maxHp; }
    public int          getCurrentHp()   { return currentHp; }
    public int          getBaseDamage()  { return baseDamage; }
    public Element      getElement()     { return element; }
    public StatusEffect getStatus()      { return status; }
    public boolean      isAlive()        { return currentHp > 0; }
    public boolean      isFrozen()       { return status == StatusEffect.FROZEN; }
    public boolean      isWeakened()     { return status == StatusEffect.WEAKENED; }

    // =========================================================================
    // IMPLEMENTASI: Encapsulation — Setter dengan validasi
    // =========================================================================

    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(hp, maxHp));
    }

    public void setBaseDamage(int damage) {
        this.baseDamage = Math.max(1, damage);
    }

    public void takeDamage(int damage) {
        this.currentHp = Math.max(0, currentHp - Math.max(0, damage));
    }

    /**
     * Terapkan status efek ke kapal ini.
     * FROZEN kebal terhadap status lain — tidak bisa di-overwrite.
     */
    public void applyStatus(StatusEffect newStatus, int duration) {
        if (this.status == StatusEffect.FROZEN && newStatus != StatusEffect.NONE) return;
        this.status         = newStatus;
        this.statusDuration = duration;
    }

    // =========================================================================
    // Status Effect Processing
    // =========================================================================

    /**
     * Proses semua status efek aktif di awal giliran kapal ini.
     * Harus dipanggil oleh GameManager sebelum takeTurn().
     * @return String log semua efek yang terjadi (untuk ditampilkan ke pemain).
     */
    public String processStatus() {
        if (status == StatusEffect.NONE) return "";
        StringBuilder log = new StringBuilder();

        switch (status) {
            case BURNED:
                int burnDmg = 8;
                takeDamage(burnDmg);
                statusDuration--;
                log.append(String.format("  [BURNED]   %s terbakar! -%d HP (sisa %d turn)%n",
                        name, burnDmg, statusDuration));
                break;

            case WEAKENED:
                statusDuration--;
                log.append(String.format("  [WEAKENED] %s masih lemah (%d turn lagi).%n",
                        name, statusDuration));
                break;

            case FROZEN:
                statusDuration--;
                log.append(String.format("  [FROZEN]   %s membeku! Giliran dilewati.%n", name));
                break;

            default:
                break;
        }

        // Hapus status jika durasinya habis
        if (statusDuration <= 0) {
            log.append(String.format("  [STATUS]   Efek %s pada %s habis.%n",
                    status.name(), name));
            status = StatusEffect.NONE;
        }

        return log.toString();
    }

    // =========================================================================
    // Damage Calculation
    // =========================================================================

    /**
     * Hitung damage keluar setelah memperhitungkan efek WEAKENED.
     * WEAKENED mengurangi damage 30%.
     */
    protected int effectiveDamage(int rawDamage) {
        return isWeakened() ? (int)(rawDamage * 0.70) : rawDamage;
    }

    // =========================================================================
    // IMPLEMENTASI: Method Overloading
    // Dua variasi serangan dengan parameter berbeda.
    // =========================================================================

    /**
     * Overload 1 — Serangan fisik/meriam. Netral, tidak ada elemen.
     * Dipakai oleh aksi "Tembak Meriam" dan serangan dasar musuh.
     */
    public int attack(Ship target) {
        int damage = effectiveDamage(baseDamage);
        target.takeDamage(damage);
        return damage;
    }

    /**
     * Overload 2 — Serangan sihir via Mage. Damage dipengaruhi elemen.
     * Dipakai sebagai kalkulasi dasar sebelum sinergy bonus diterapkan.
     */
    public int attack(Ship target, Mage mage) {
        double multiplier = mage.getElement().getMultiplier(target.getElement());
        int    rawDamage  = (int)((baseDamage + mage.getMagicPower()) * multiplier);
        int    damage     = effectiveDamage(rawDamage);
        target.takeDamage(damage);
        return damage;
    }

    // =========================================================================
    // UI
    // =========================================================================

    /** HP bar visual berbasis teks dengan tag status efek jika aktif. */
    public String getHpBar() {
        int           barLength = 20;
        int           filled    = (int)((double) currentHp / maxHp * barLength);
        StringBuilder bar       = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) bar.append(i < filled ? "#" : ".");
        bar.append("]");
        String statusTag = (status != StatusEffect.NONE) ? " " + status.getTag() : "";
        return String.format("%s %d/%d%s", bar, currentHp, maxHp, statusTag);
    }

    // =========================================================================
    // IMPLEMENTASI: Abstract Method — wajib diimplementasikan subclass
    // =========================================================================

    /**
     * Eksekusi giliran aksi kapal ini.
     * PlayerShip  : logika input dari pemain (dikelola GameManager).
     * EnemyShip   : AI sederhana — selalu serang.
     * BossShip    : AI boss — dual attack saat RAGE.
     */
    public abstract void takeTurn(Ship opponent, Scanner scanner);

    /**
     * Tampilkan status kapal dalam satu blok teks.
     * Format berbeda antara PlayerShip dan EnemyShip/BossShip.
     */
    public abstract String getStatusDisplay();
}

// =============================================================================
// FILE: PlayerShip.java
// IMPLEMENTASI: Inheritance — extends Ship
// IMPLEMENTASI: Interface — implements MagicCastable, Describable
// IMPLEMENTASI: Composition — HAS-A List<Mage> (roster), HAS-A Map<CannonballType,Integer> (ammo)
// IMPLEMENTASI: Encapsulation — semua field private, akses via getter/setter
// IMPLEMENTASI: Method Overriding — takeTurn(), castMagic(), castSpell(), dll
//
// Kapal yang dikendalikan pemain. Menyimpan roster Mage (maks 5),
// inventori cannonball, potion, dan mengelola sistem sinergy elemen.
// =============================================================================

class PlayerShip extends Ship implements MagicCastable, Describable {

    // IMPLEMENTASI: Composition — PlayerShip HAS-A list of Mage
    private final List<Mage>                  roster;
    private static final int                  MAX_MAGE    = 5;

    // IMPLEMENTASI: Composition — PlayerShip HAS-A inventori cannonball
    private final Map<CannonballType, Integer> ammo;
    private static final int                  MAX_AMMO    = 8;  // batas stok per tipe

    private       int     bounty;
    private       int     potions;
    private       boolean shieldActive;
    private static final int MAX_POTIONS = 3;
    private static final int POTION_HEAL = 50;

    public PlayerShip(String name, int maxHp, int baseDamage, Element element) {
        // IMPLEMENTASI: Inheritance — panggil constructor parent Ship
        super(name, maxHp, baseDamage, element);

        roster       = new ArrayList<>();
        ammo         = new EnumMap<>(CannonballType.class);
        bounty       = 0;
        potions      = 2;
        shieldActive = false;

        // Stok peluru awal: Iron unlimited (nilai -1), sisanya 3
        ammo.put(CannonballType.IRON,      -1); // -1 = tak terbatas
        ammo.put(CannonballType.EXPLOSIVE,  3);
        ammo.put(CannonballType.CHAIN,      3);
        ammo.put(CannonballType.GRAPESHOT,  3);
    }

    // =========================================================================
    // IMPLEMENTASI: Encapsulation — Getter
    // =========================================================================

    public List<Mage> getRoster()   { return Collections.unmodifiableList(roster); }
    public int  getMageCount()      { return roster.size(); }
    public int  getBounty()         { return bounty; }
    public int  getPotions()        { return potions; }
    public boolean isShielded()     { return shieldActive; }

    public int getAmmoCount(CannonballType type) {
        Integer val = ammo.get(type);
        return val == null ? 0 : val;
    }

    // =========================================================================
    // IMPLEMENTASI: Encapsulation — Mutators dengan validasi
    // =========================================================================

    public void addBounty(int amount)         { bounty += amount; }
    public void heal(int amount)              { setCurrentHp(currentHp + amount); }
    public void upgradeBaseDamage(int amount) { setBaseDamage(baseDamage + amount); }
    public void activateShield()              { shieldActive = true; }
    public void resetShield()                 { shieldActive = false; }

    /**
     * Override takeDamage agar shield memotong damage 50% saat aktif.
     * IMPLEMENTASI: Method Overriding dari Ship.takeDamage()
     */
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

    /** Rekrut Mage baru ke roster. Return false jika sudah penuh. */
    public boolean recruitMage(Mage mage) {
        if (roster.size() >= MAX_MAGE) return false;
        roster.add(mage);
        return true;
    }

    /** Tambah stok peluru. Iron tidak bisa ditambah (sudah unlimited). */
    public void addAmmo(CannonballType type, int amount) {
        if (type == CannonballType.IRON) return;
        int current = getAmmoCount(type);
        ammo.put(type, Math.min(MAX_AMMO, current + amount));
    }

    /** Reset jurus semua Mage — dipanggil di awal setiap battle baru. */
    public void resetAllSpells() {
        for (Mage m : roster) m.resetSpell();
    }

    /** Berikan XP ke semua Mage. Return list pesan level-up yang terjadi. */
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

    // =========================================================================
    // Sinergy System
    // =========================================================================

    /**
     * Hitung sinergy bonus berdasarkan jumlah Mage elemen sama di roster.
     *   2+ Mage elemen sama -> +20% magic damage (x1.20)
     *   3+ Mage elemen sama -> +40% magic damage (x1.40)
     * Mendorong pemain membangun "deck" Mage yang sinergis.
     */
    public double getSynergyMult(Element element) {
        int count = countMageByElement(element);
        return count >= 3 ? 1.40 : count >= 2 ? 1.20 : 1.00;
    }

    /** Hitung berapa Mage di roster yang punya elemen tertentu. */
    public int countMageByElement(Element element) {
        int count = 0;
        for (Mage m : roster) {
            if (m.getElement() == element) count++;
        }
        return count;
    }

    // =========================================================================
    // Cannonball System
    // =========================================================================

    /**
     * Tembakkan cannonball tipe tertentu ke target.
     * Kurangi stok (kecuali Iron), terapkan efek status sesuai tipe,
     * lalu kirim damage ke target.
     * @return Damage yang dilakukan, atau -1 jika stok habis.
     */
    public int fireCannonball(CannonballType type, Ship target) {
        int stock = getAmmoCount(type);
        if (type != CannonballType.IRON && stock <= 0) return -1;

        // Kurangi stok (Iron tidak dikurangi)
        if (type != CannonballType.IRON) ammo.put(type, stock - 1);

        int damage = effectiveDamage((int)(baseDamage * type.dmgMult));

        // Terapkan efek status dari tipe peluru ke target
        if (type.appliesBurn) target.applyStatus(StatusEffect.BURNED,   3);
        if (type.appliesWeak) target.applyStatus(StatusEffect.WEAKENED, 3);

        target.takeDamage(damage);
        return damage;
    }

    // =========================================================================
    // IMPLEMENTASI: Method Overriding dari interface MagicCastable
    // =========================================================================

    /**
     * Serangan sihir biasa dengan sinergy bonus.
     * Damage = (baseDamage + magicPower) * elemMultiplier * synergyMultiplier
     */
    @Override
    public int castMagic(Ship target, Mage mage) {
        double elemMult    = mage.getElement().getMultiplier(target.getElement());
        double synergyMult = getSynergyMult(mage.getElement());
        int    rawDamage   = (int)((baseDamage + mage.getMagicPower()) * elemMult * synergyMult);
        int    damage      = effectiveDamage(rawDamage);
        target.takeDamage(damage);
        return damage;
    }

    /**
     * Gunakan Jurus unik Mage. Efek berbeda per SpellType.
     * Jurus hanya bisa dipakai SEKALI per battle.
     * @return String deskripsi hasil jurus untuk ditampilkan ke pemain.
     */
    @Override
    public String castSpell(Ship target, Mage mage) {
        if (mage.isSpellUsed()) {
            return "  [!] Jurus " + mage.getSpellType().getDisplayName() + " sudah dipakai battle ini!";
        }

        mage.markSpellUsed();
        double synergyMult = getSynergyMult(mage.getElement());
        int    basePower   = baseDamage + mage.getMagicPower();
        StringBuilder result = new StringBuilder();

        switch (mage.getSpellType()) {

            case INFERNO: {
                // Damage x3.0, tidak ada elemen multiplier (pure burst)
                int damage = (int)(basePower * 3.0 * synergyMult);
                target.takeDamage(damage);
                result.append(String.format(
                        "  *** INFERNO! %s membakar segalanya! Damage: %d ***",
                        mage.getName(), damage));
                break;
            }

            case IGNITE: {
                // Damage normal + BURNED 3 turn
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
                // Damage x2.0 + heal player 35 HP
                double elemMult = mage.getElement().getMultiplier(target.getElement());
                int    damage   = (int)(basePower * 2.0 * elemMult * synergyMult);
                target.takeDamage(damage);
                int hpBefore = currentHp;
                heal(35);
                result.append(String.format(
                        "  *** TIDAL WAVE! Damage: %d + Healed +%d HP ***",
                        damage, currentHp - hpBefore));
                break;
            }

            case FREEZE: {
                // Damage normal + FROZEN 1 turn (skip giliran + kebal status lain)
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
                // Damage x2.5 + WEAKENED 3 turn (-30% damage musuh)
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
                // Setiap Mage di roster menyerang masing-masing x1.2
                result.append("  *** OVERCHARGE! Semua Mage menyerang serentak!\n");
                int totalDamage = 0;
                for (Mage m : roster) {
                    double elemMult = m.getElement().getMultiplier(target.getElement());
                    int    hit      = (int)((baseDamage + m.getMagicPower()) * 1.2 * elemMult);
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
        if (roster.isEmpty()) {
            System.out.println("    (Tidak ada Mage di roster)");
            return;
        }
        for (int i = 0; i < roster.size(); i++) {
            System.out.printf("    [%d] %s%n", i + 1, roster.get(i).getInfo(showSpell));
        }
    }

    // =========================================================================
    // IMPLEMENTASI: Method Overriding dari interface Describable
    // =========================================================================

    @Override
    public String getFullDescription() {
        return String.format(
            "Kapal: %s | HP: %d/%d | Cannon: %d | Mage: %d/%d | Potion: %d | Bounty: $%d",
            name, currentHp, maxHp, baseDamage, roster.size(), MAX_MAGE, potions, bounty);
    }

    // =========================================================================
    // IMPLEMENTASI: Method Overriding dari abstract class Ship
    // =========================================================================

    /**
     * takeTurn() untuk PlayerShip — logika input sepenuhnya dikelola GameManager.
     * Method ini ada untuk memenuhi kontrak abstract Ship.
     */
    @Override
    public void takeTurn(Ship opponent, Scanner scanner) {
        // Input dikelola di GameManager.doPlayerTurn()
    }

    @Override
    public String getStatusDisplay() {
        String shieldTag = shieldActive ? " [SHIELD]" : "";
        return String.format(
            "  [PLAYER] %s%s | HP: %s | Cannon: %d | Mage: %d/%d | Potion: %d | $%d",
            name, shieldTag, getHpBar(), baseDamage,
            roster.size(), MAX_MAGE, potions, bounty);
    }
}

// =============================================================================
// FILE: EnemyShip.java
// IMPLEMENTASI: Inheritance — extends Ship
// IMPLEMENTASI: Interface — implements Describable
// IMPLEMENTASI: Method Overriding — takeTurn() dengan AI sederhana
//
// Kapal musuh yang di-generate setiap stage. Punya EnemyTrait (sifat pasif)
// yang muncul acak dan memaksa pemain menyesuaikan strategi.
// AI: selalu menyerang dengan serangan fisik dasar per giliran.
// =============================================================================

class EnemyShip extends Ship implements Describable {

    // IMPLEMENTASI: Encapsulation — field protected agar BossShip bisa akses
    protected int        bountyReward;
    protected int        xpReward;
    protected EnemyTrait trait;

    public EnemyShip(String name, int maxHp, int baseDamage, Element element,
                     int bountyReward, int xpReward, EnemyTrait trait) {
        // IMPLEMENTASI: Inheritance — panggil constructor parent Ship
        super(name, maxHp, baseDamage, element);
        this.bountyReward = bountyReward;
        this.xpReward     = xpReward;
        this.trait        = trait;
    }

    // =========================================================================
    // IMPLEMENTASI: Encapsulation — Getter
    // =========================================================================

    public int        getBountyReward() { return bountyReward; }
    public int        getXpReward()     { return xpReward; }
    public EnemyTrait getTrait()        { return trait; }

    // =========================================================================
    // Trait Processing
    // =========================================================================

    /**
     * Proses sifat pasif musuh di awal gilirannya.
     * Hanya REGENERATE yang aktif di sini (pulihkan HP).
     * BERSERKER dan ARMORED dihitung saat damage, THORNS saat menerima sihir.
     * @return Log string efek yang terjadi.
     */
    public String processTrait() {
        if (trait != EnemyTrait.REGENERATE) return "";
        int healAmount = 10;
        setCurrentHp(currentHp + healAmount);
        return String.format("  [REGEN] %s memulihkan %d HP!%n", name, healAmount);
    }

    /**
     * Hitung damage keluar dengan bonus BERSERKER jika aktif.
     * BERSERKER: damage +50% saat HP < 50%.
     */
    public int getBerserkerDamage(int rawDamage) {
        if (trait == EnemyTrait.BERSERKER && (double) currentHp / maxHp < 0.5) {
            return (int)(rawDamage * 1.5);
        }
        return rawDamage;
    }

    /**
     * Kurangi damage fisik yang masuk jika musuh ARMORED.
     * ARMORED: semua damage cannon berkurang 35%.
     * @param damage     Damage sebelum reduksi.
     * @param isPhysical true jika damage dari cannon (bukan sihir).
     * @return Damage setelah reduksi armor (jika berlaku).
     */
    public int applyArmorReduction(int damage, boolean isPhysical) {
        if (trait == EnemyTrait.ARMORED && isPhysical) {
            return (int)(damage * 0.65);
        }
        return damage;
    }

    // =========================================================================
    // IMPLEMENTASI: Method Overriding dari abstract Ship
    // AI: proses trait, lalu selalu serang dengan damage dasar.
    // =========================================================================

    @Override
    public void takeTurn(Ship opponent, Scanner scanner) {
        // Proses trait pasif terlebih dahulu
        String traitLog = processTrait();
        if (!traitLog.isEmpty()) System.out.print(traitLog);

        // Hitung damage dengan mempertimbangkan WEAKENED dan BERSERKER
        int rawDamage        = effectiveDamage(baseDamage);
        int finalDamage      = getBerserkerDamage(rawDamage);
        opponent.takeDamage(finalDamage);

        String berserkerTag = (trait == EnemyTrait.BERSERKER && finalDamage > rawDamage)
                ? " [BERSERKER!]" : "";
        System.out.printf("  [MUSUH] %s menyerang! Damage: %d%s%n",
                name, finalDamage, berserkerTag);
    }

    // =========================================================================
    // IMPLEMENTASI: Method Overriding dari interface Describable
    // =========================================================================

    @Override
    public String getFullDescription() {
        String statusTag = (status != StatusEffect.NONE) ? status.getTag() : "";
        return String.format(
            "  %-24s | %s%s | Elemen: %-5s | Dmg: ~%d | Sifat: %-12s | Bounty: $%d",
            name, getHpBar(), statusTag,
            element.sym(), baseDamage, trait.displayName, bountyReward);
    }

    @Override
    public String getStatusDisplay() {
        return getFullDescription();
    }
}

// =============================================================================
// FILE: BossShip.java
// IMPLEMENTASI: Inheritance multi-level — BossShip -> EnemyShip -> Ship
// IMPLEMENTASI: Method Overriding — takeTurn() dengan perilaku boss unik
//
// Muncul setiap kelipatan 5 stage. Berbeda dari EnemyShip biasa:
//  - Punya judul (bossTitle) untuk narasi
//  - Fase RAGE (HP < 50%): menyerang DUA kali per giliran
//  - Serangan kedua (Kutukan Laut) lebih lemah tapi tetap signifikan
//
// Keputusan desain: dual attack HANYA saat RAGE agar boss tidak unfair
// di awal battle, tapi tetap berbahaya saat HP-nya kritis.
// =============================================================================

class BossShip extends EnemyShip {

    private final String bossTitle;
    private final int    bonusDamage; // damage serangan kedua saat RAGE

    public BossShip(String name, String bossTitle, int maxHp, int baseDamage,
                    Element element, int bountyReward, int xpReward, EnemyTrait trait) {
        // IMPLEMENTASI: Inheritance — panggil constructor EnemyShip
        super(name, maxHp, baseDamage, element, bountyReward, xpReward, trait);
        this.bossTitle   = bossTitle;
        this.bonusDamage = baseDamage / 3; // serangan kedua = 33% dari base damage
    }

    // =========================================================================
    // IMPLEMENTASI: Encapsulation — Getter
    // =========================================================================

    public String  getBossTitle() { return bossTitle; }

    /** true jika boss dalam fase RAGE (HP di bawah 50%). */
    public boolean isEnraged()    { return (double) currentHp / maxHp < 0.5; }

    // =========================================================================
    // IMPLEMENTASI: Method Overriding dari EnemyShip (dan Ship)
    // Boss punya perilaku takeTurn() yang berbeda: bisa menyerang 2x saat RAGE.
    // =========================================================================

    @Override
    public void takeTurn(Ship opponent, Scanner scanner) {
        // Proses trait pasif (REGENERATE jika berlaku)
        String traitLog = processTrait();
        if (!traitLog.isEmpty()) System.out.print(traitLog);

        System.out.printf("%n  [BOSS] %s -- %s bergerak!%n", bossTitle, name);

        // Serangan pertama selalu terjadi
        int rawDamage   = effectiveDamage(baseDamage);
        int finalDamage = getBerserkerDamage(rawDamage);
        opponent.takeDamage(finalDamage);

        String berserkerTag = (trait == EnemyTrait.BERSERKER && finalDamage > rawDamage)
                ? " [BERSERKER!]" : "";
        System.out.printf("  >> Serangan Utama: %d damage%s%n", finalDamage, berserkerTag);

        // Serangan kedua HANYA saat fase RAGE (HP < 50%) dan lawan masih hidup
        if (isEnraged() && opponent.isAlive()) {
            int rageDamage = Math.max(1,
                    (int)(bonusDamage * (0.85 + RNG.nextDouble() * 0.30)));
            opponent.takeDamage(rageDamage);
            System.out.printf("  >> [RAGE] Kutukan Laut: %d damage tambahan!%n", rageDamage);
        }
    }

    // =========================================================================
    // IMPLEMENTASI: Method Overriding dari Describable (via EnemyShip)
    // =========================================================================

    @Override
    public String getFullDescription() {
        String rageTag = isEnraged() ? " *** ENRAGED! ***" : "";
        return String.format(
            "  [BOSS] %s -- %-18s%s%n  HP: %s | Elemen: %-5s | Dmg: ~%d | Sifat: %s",
            bossTitle, name, rageTag,
            getHpBar(), element.sym(), baseDamage, trait.displayName);
    }

    @Override
    public String getStatusDisplay() {
        return getFullDescription();
    }
}

// =============================================================================
// FILE: RewardCard.java
// Sistem reward terinspirasi Balatro — pool besar kartu, 3 dipilih acak
// dengan weighted probability setiap kali pemain menang.
//
// Desain: reward bukan menu tetap, melainkan "kartu" yang muncul acak.
// Kartu langka (weight rendah) bisa muncul kapan saja, menciptakan momen
// kejutan dan mendorong pemain untuk beradaptasi dengan yang ditawarkan.
//
// ENUM Type berada di dalam kelas ini karena tightly coupled —
// tidak ada kelas lain yang perlu tahu Type tanpa konteks RewardCard.
// =============================================================================

class RewardCard {

    // Semua jenis reward yang mungkin muncul
    enum Type {
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

    // IMPLEMENTASI: Encapsulation — field final, hanya bisa dibaca
    final Type   type;
    final String title;
    final String description;
    final int    weight; // semakin tinggi = semakin sering muncul

    RewardCard(Type type, String title, String description, int weight) {
        this.type        = type;
        this.title       = title;
        this.description = description;
        this.weight      = weight;
    }

    /** Satu baris display untuk ditampilkan ke pemain. */
    public String getDisplay() {
        return String.format("%-28s -- %s", title, description);
    }

    // =========================================================================
    // Pool & Drawing
    // =========================================================================

    /**
     * Bangun pool lengkap semua kartu yang mungkin muncul.
     * Weight menentukan seberapa sering kartu muncul relatif terhadap yang lain.
     */
    static List<RewardCard> buildPool() {
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
     * Kartu yang syaratnya tidak terpenuhi (roster penuh, potion penuh)
     * otomatis difilter agar tidak muncul.
     *
     * @param rng    Random number generator.
     * @param player PlayerShip untuk cek kondisi (roster, potion).
     * @return List berisi tepat 3 RewardCard yang berbeda.
     */
    static List<RewardCard> drawThree(Random rng, PlayerShip player) {
        List<RewardCard> pool = buildPool();

        // Filter kartu yang tidak relevan untuk kondisi player saat ini
        pool.removeIf(card ->
            (card.type == Type.RECRUIT_MAGE && player.getMageCount() >= 5) ||
            (card.type == Type.ADD_POTION   && player.getPotions()   >= 3)
        );

        List<RewardCard>  drawn      = new ArrayList<>();
        Set<RewardCard.Type> selected = new HashSet<>();
        int maxAttempts = pool.size() * 4;
        int attempt     = 0;

        // Weighted random sampling tanpa pengulangan tipe
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

        // Fallback: pastikan selalu ada 3 pilihan meski sampling gagal
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

// =============================================================================
// FILE: BalanceConfig.java
// Semua konstanta balance game di satu tempat.
//
// Keuntungan memisahkan ini: untuk menyesuaikan difficulty, dosen/penguji
// cukup membuka file ini — tidak perlu mencari angka di seluruh codebase.
//
// Formula scaling musuh:
//   HP musuh    = BASE_ENEMY_HP  + (stage * HP_SCALE)
//   Damage musuh = BASE_ENEMY_DMG + (stage * DMG_SCALE)
//   Boss HP      = HP musuh       * BOSS_HP_MULT
//   Boss Damage  = Damage musuh   * BOSS_DMG_MULT
//
// Contoh Stage 5 Boss:
//   HP    = (55 + 5*10) * 2.2 = 231
//   Dmg   = (10 + 5*2)  * 1.5 = 30
//   Player bisa menang dalam ~2.1 turn pakai super-effective magic
//   Boss butuh ~5.8 turn untuk kill player dengan 120 HP
// =============================================================================
class BalanceConfig {

    // ── Player awal ──────────────────────────────────────────────────────────
    static final int    PLAYER_HP       = 120;
    static final int    PLAYER_DAMAGE   = 18;
    static final int    START_MAGE_PWR  = 22;

    // ── Scaling musuh per stage ───────────────────────────────────────────────
    static final int    BASE_ENEMY_HP   = 55;
    static final int    HP_SCALE        = 10;
    static final int    BASE_ENEMY_DMG  = 10;
    static final int    DMG_SCALE       = 2;

    // ── Boss multiplier (diterapkan ke stat enemy biasa) ──────────────────────
    static final double BOSS_HP_MULT    = 2.2;
    static final double BOSS_DMG_MULT   = 1.5;

    // ── XP & Bounty reward ────────────────────────────────────────────────────
    static final int    BASE_XP         = 20;
    static final int    XP_SCALE        = 3;
    static final int    BASE_BOUNTY     = 120;
    static final int    BOUNTY_SCALE    = 45;
    static final int    BOSS_BOUNTY_BON = 400;
}

// =============================================================================
// FILE: InputHelper.java
// Utility class untuk sanitasi semua input dari Scanner.
//
// Semua interaksi keyboard pemain melewati class ini.
// Mencegah crash dari: input kosong, non-numerik, angka di luar range,
// atau Scanner yang bermasalah karena EOF.
// =============================================================================

class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Baca satu baris teks dari input.
     * Return string kosong "" jika terjadi error (tidak crash).
     */
    public String readLine() {
        try {
            String line = scanner.nextLine();
            return (line == null) ? "" : line.trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Baca integer dalam rentang [min, max].
     * Jika input tidak valid atau di luar rentang, kembalikan defaultValue.
     * Tidak pernah throw exception.
     *
     * @param min          Nilai minimum yang diterima (inklusif).
     * @param max          Nilai maksimum yang diterima (inklusif).
     * @param defaultValue Nilai fallback jika input tidak valid.
     */
    public int readInt(int min, int max, int defaultValue) {
        try {
            int value = Integer.parseInt(readLine());
            return (value >= min && value <= max) ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Tampilkan prompt lalu tunggu pemain menekan Enter.
     * Dipakai di antara tampilan yang perlu jeda.
     */
    public void pressEnter() {
        System.out.print("  [Enter untuk lanjut...] ");
        readLine();
    }
}

// =============================================================================
// FILE: GameManager.java
// IMPLEMENTASI: Polymorphism — enemy disimpan sebagai referensi Ship,
//               takeTurn() dipanggil polimorfis (EnemyShip vs BossShip)
//
// Mengelola seluruh siklus game:
//   start() -> showHelp() -> initPlayer() -> gameLoop()
//     -> offerEnemyChoice() -> runBattle() -> handleReward() -> [ulang]
//
// Semua logika UI (print) dan semua logika input pemain ada di sini,
// sehingga class-class lain (Ship, Mage, dll) tetap bersih dari I/O.
// =============================================================================

class GameManager {

    private PlayerShip  player;
    private InputHelper input;
    private final Random rng   = new Random();
    private int         stage  = 1;
    private boolean     cannonDoubled = false; // efek kartu langka DOUBLE_CANNON

    // ── Data teks untuk generasi nama acak ───────────────────────────────────
    private static final String[] ENEMY_NAMES = {
        "Galleon Merah", "Fregat Vortex", "Brigantine Badai", "Junk Hantu",
        "Karakoa Iblis", "Schooner Kutukan", "Barque Neraka", "Dhow Kegelapan",
        "Frigate Bayangan", "Brig Kutukan"
    };
    private static final String[] BOSS_TITLES = {
        "Raja Tua Laut", "Sang Leviathan", "Dewa Badai", "Si Mata Satu",
        "Laksamana Kegelapan", "Naga Samudra", "Hantu Laut Dalam"
    };
    private static final String[] MAGE_NAMES = {
        "Ignis", "Aquara", "Voltus", "Pyra", "Hydra", "Tempest",
        "Ember", "Cascade", "Thunder", "Inferno", "Torrent", "Zephyr",
        "Cinder", "Deluge", "Gale", "Blaze", "Surge", "Frost", "Ash", "Mist"
    };

    public GameManager() {
        this.input = new InputHelper(new Scanner(System.in));
    }

    // =========================================================================
    // START & INITIALIZATION
    // =========================================================================

    public void start() {
        printBanner();
        showHelp();
        initPlayer();
        gameLoop();
    }

    private void printBanner() {
        System.out.println();
        System.out.println("+============================================================+");
        System.out.println("|    (*)   ROGUELIKE PIRATE MAGE BATTLE   (*)               |");
        System.out.println("|          v4 Strategic Edition -- OOP Final Project        |");
        System.out.println("+============================================================+");
        System.out.println();
    }

    private void showHelp() {
        System.out.println("  +------------------------------------------------------+");
        System.out.println("  |  ELEMEN  : FIRE > STORM > WATER > FIRE (x2.0 SE)    |");
        System.out.println("  +------------------------------------------------------+");
        System.out.println("  |  AKSI BATTLE:                                        |");
        System.out.println("  |   [1] Tembak Meriam -- pilih tipe peluru             |");
        System.out.println("  |       Iron(safe) | Explosive(x2.5) |                 |");
        System.out.println("  |       Chain(WEAKENED) | Grapeshot(BURNED)            |");
        System.out.println("  |   [2] Sihir Mage    -- elemen + sinergy bonus        |");
        System.out.println("  |   [3] Gunakan Jurus -- efek spesial, 1x per battle   |");
        System.out.println("  |   [4] Bertahan      -- -50% damage masuk turn ini    |");
        System.out.println("  |   [5] Minum Potion  -- heal 50 HP                    |");
        System.out.println("  +------------------------------------------------------+");
        System.out.println("  |  SINERGY : 2+ Mage elemen sama -> +20% magic dmg    |");
        System.out.println("  |            3+ Mage elemen sama -> +40% magic dmg    |");
        System.out.println("  +------------------------------------------------------+");
        System.out.println("  |  MUSUH   : Pilih 1 dari 3 opsi setiap stage         |");
        System.out.println("  |  REWARD  : 3 kartu acak dari pool besar (weighted)  |");
        System.out.println("  +------------------------------------------------------+");
        System.out.println();
        input.pressEnter();
    }

    private void initPlayer() {
        System.out.println("  +--------------------------------------------------+");
        System.out.println("  |           PENDAFTARAN KAPTEN                     |");
        System.out.println("  +--------------------------------------------------+");
        System.out.print("  Nama Kapten: ");
        String captainName = input.readLine();
        if (captainName.isEmpty()) captainName = "Nusantara";

        // IMPLEMENTASI: Polymorphism — player bertipe PlayerShip (konkret)
        player = new PlayerShip(
            "Kapal " + captainName,
            BalanceConfig.PLAYER_HP,
            BalanceConfig.PLAYER_DAMAGE,
            Element.WATER
        );

        System.out.println();
        System.out.println("  Pilih Mage pertama + Jurus:");
        System.out.println("  [1] FIRE  + INFERNO    (Damage x3.0, sekali pakai)");
        System.out.println("  [2] FIRE  + IGNITE     (Damage + BURNED 3 turn)");
        System.out.println("  [3] WATER + TIDAL WAVE (Damage x2.0 + Heal 35 HP)");
        System.out.println("  [4] WATER + FREEZE     (Damage + FROZEN skip+kebal)");
        System.out.println("  [5] STORM + CHAIN BOLT (Damage x2.5 + WEAKENED 3t)");
        System.out.println("  [6] STORM + OVERCHARGE (Semua Mage x1.2 sekaligus)");
        System.out.print("  Pilihan (1-6): ");

        int choice = input.readInt(1, 6, 1);

        SpellType[] spells   = {
            SpellType.INFERNO,    SpellType.IGNITE,
            SpellType.TIDAL_WAVE, SpellType.FREEZE,
            SpellType.CHAIN_BOLT, SpellType.OVERCHARGE
        };
        Element[] elements = {
            Element.FIRE,  Element.FIRE,
            Element.WATER, Element.WATER,
            Element.STORM, Element.STORM
        };

        Mage startingMage = new Mage(
            randomMageName(),
            elements[choice - 1],
            BalanceConfig.START_MAGE_PWR,
            spells[choice - 1]
        );
        player.recruitMage(startingMage);

        System.out.println();
        System.out.printf("  Mage  : %s%n", startingMage.getInfo(true));
        System.out.printf("  Jurus : %s%n", startingMage.getSpellType().getFullDescription());
        System.out.println("  Peluru: Iron(tak terbatas), Explosive x3, Chain x3, Grapeshot x3");
        System.out.println("  Potion: 2");
        System.out.println();
        System.out.println("  Berlayar! Pilih musuh dengan bijak setiap stage.");
        System.out.println();
        input.pressEnter();
    }

    // =========================================================================
    // GAME LOOP
    // =========================================================================

    private void gameLoop() {
        // IMPLEMENTASI: Polymorphism — currentEnemy bertipe Ship (bisa Enemy atau Boss)
        Ship currentEnemy;

        while (player.isAlive()) {
            cannonDoubled = false; // reset efek kartu langka tiap stage baru
            printStageHeader();

            boolean isBoss = (stage % 5 == 0);

            if (isBoss) {
                currentEnemy = generateBoss(stage);
                System.out.println("  *** STAGE BOSS! Tidak ada pilihan musuh. ***");
                System.out.println(currentEnemy.getStatusDisplay());
            } else {
                // Pemain memilih 1 dari 3 opsi musuh
                currentEnemy = offerEnemyChoice(stage);
            }

            printPreBattleInfo(currentEnemy);
            input.pressEnter();

            // Reset semua jurus Mage untuk battle baru ini
            player.resetAllSpells();

            boolean playerWon = runBattle(currentEnemy);

            if (playerWon) {
                EnemyShip defeatedEnemy = (EnemyShip) currentEnemy;
                player.addBounty(defeatedEnemy.getBountyReward());
                List<String> levelUps = player.grantXpToAll(defeatedEnemy.getXpReward());
                printVictory(defeatedEnemy, levelUps);
                input.pressEnter();
                handleReward();
                stage++;
            } else {
                printGameOver();
                break;
            }
        }
    }

    // =========================================================================
    // ENEMY CHOICE — Pilih 1 dari 3 musuh
    // =========================================================================

    /**
     * Tampilkan 3 opsi musuh dan minta pemain memilih.
     *   Opsi 1: Musuh standar, elemen acak.
     *   Opsi 2: Musuh elemen berbeda dari opsi 1, HP +15%, bounty +30%.
     *   Opsi 3: Musuh Elite, HP +40%, DMG +20%, bounty +80% -- high risk high reward.
     */
    private Ship offerEnemyChoice(int stage) {
        EnemyShip[] options = new EnemyShip[3];
        options[0] = generateEnemy(stage);
        options[1] = generateEnemyDifferentElement(stage, options[0].getElement());
        options[2] = generateEliteEnemy(stage);

        System.out.println("  Pilih musuh yang akan dihadapi:");
        System.out.println();

        for (int i = 0; i < 3; i++) {
            System.out.printf("  [%d] %s%n", i + 1, options[i].getFullDescription());
            printCounterHint(options[i]);
            System.out.println();
        }

        System.out.print("  Pilihan (1-3): ");
        int choice = input.readInt(1, 3, 1);
        System.out.println();
        return options[choice - 1];
    }

    /** Tampilkan hint counter elemen + info sinergy yang relevan. */
    private void printCounterHint(EnemyShip enemy) {
        Element counter       = enemy.getElement().weakness();
        boolean hasCounter    = player.hasCounterMage(enemy.getElement());
        int     synergyCount  = player.countMageByElement(counter);

        System.out.printf("     Counter: %s", counter.sym());

        if (hasCounter) {
            System.out.print(" | Anda punya Mage counter!");
            if (synergyCount >= 2) {
                System.out.printf(" + SINERGY +%d%%", synergyCount >= 3 ? 40 : 20);
            }
        } else {
            System.out.print(" | (Tidak punya Mage counter)");
        }

        if (enemy.getTrait() != EnemyTrait.NONE) {
            System.out.printf(" | Sifat: %s", enemy.getTrait().description);
        }

        System.out.println();
    }

    // =========================================================================
    // ENEMY GENERATION
    // =========================================================================

    private EnemyShip generateEnemy(int stage) {
        int hp     = BalanceConfig.BASE_ENEMY_HP  + stage * BalanceConfig.HP_SCALE;
        int damage = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
        int bounty = BalanceConfig.BASE_BOUNTY    + stage * BalanceConfig.BOUNTY_SCALE;
        int xp     = BalanceConfig.BASE_XP        + stage * BalanceConfig.XP_SCALE;
        return new EnemyShip(randomEnemyName(), hp, damage,
                randomElement(), bounty, xp, randomTrait());
    }

    private EnemyShip generateEnemyDifferentElement(int stage, Element exclude) {
        int hp     = (int)((BalanceConfig.BASE_ENEMY_HP  + stage * BalanceConfig.HP_SCALE)     * 1.15);
        int damage =        BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
        int bounty = (int)((BalanceConfig.BASE_BOUNTY    + stage * BalanceConfig.BOUNTY_SCALE)  * 1.30);
        int xp     = (int)((BalanceConfig.BASE_XP        + stage * BalanceConfig.XP_SCALE)      * 1.15);

        // Pastikan elemen berbeda dari opsi pertama
        Element element;
        do { element = randomElement(); } while (element == exclude);

        return new EnemyShip(randomEnemyName(), hp, damage,
                element, bounty, xp, randomTrait());
    }

    private EnemyShip generateEliteEnemy(int stage) {
        int hp     = (int)((BalanceConfig.BASE_ENEMY_HP  + stage * BalanceConfig.HP_SCALE)     * 1.40);
        int damage = (int)((BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE)    * 1.20);
        int bounty = (int)((BalanceConfig.BASE_BOUNTY    + stage * BalanceConfig.BOUNTY_SCALE)  * 1.80);
        int xp     = (int)((BalanceConfig.BASE_XP        + stage * BalanceConfig.XP_SCALE)      * 1.50);
        return new EnemyShip(randomEnemyName() + " [ELITE]", hp, damage,
                randomElement(), bounty, xp, randomTrait());
    }

    private BossShip generateBoss(int stage) {
        int baseHp  = BalanceConfig.BASE_ENEMY_HP  + stage * BalanceConfig.HP_SCALE;
        int baseDmg = BalanceConfig.BASE_ENEMY_DMG + stage * BalanceConfig.DMG_SCALE;
        int hp      = (int)(baseHp  * BalanceConfig.BOSS_HP_MULT);
        int damage  = (int)(baseDmg * BalanceConfig.BOSS_DMG_MULT);
        int bounty  = BalanceConfig.BASE_BOUNTY + stage * BalanceConfig.BOUNTY_SCALE
                    + BalanceConfig.BOSS_BOUNTY_BON;
        int xp      = (BalanceConfig.BASE_XP + stage * BalanceConfig.XP_SCALE) * 2;

        return new BossShip(
            randomEnemyName() + " [BOSS]",
            BOSS_TITLES[rng.nextInt(BOSS_TITLES.length)],
            hp, damage, randomElement(), bounty, xp, randomTrait()
        );
    }

    private EnemyTrait randomTrait() {
        EnemyTrait[] traits = EnemyTrait.values();
        // NONE punya bobot 2x agar musuh "biasa" lebih sering muncul
        int roll = rng.nextInt(traits.length + traits.length - 1);
        return roll < traits.length ? traits[roll] : EnemyTrait.NONE;
    }

    // =========================================================================
    // BATTLE SYSTEM
    // =========================================================================

    /**
     * Jalankan satu pertempuran penuh antara player vs enemy.
     *
     * IMPLEMENTASI: Polymorphism — enemy.takeTurn() dipanggil via referensi Ship.
     * JVM otomatis memilih implementasi yang tepat:
     *   - EnemyShip.takeTurn() jika enemy adalah EnemyShip biasa
     *   - BossShip.takeTurn()  jika enemy adalah BossShip
     *
     * @return true jika pemain menang, false jika kalah.
     */
    private boolean runBattle(Ship enemy) {
        boolean playerTurn = true;

        while (player.isAlive() && enemy.isAlive()) {

            if (playerTurn) {
                // ── Awal giliran player: proses status player ──
                String statusLog = player.processStatus();
                if (!statusLog.isEmpty()) System.out.print(statusLog);

                // Jika player terkena FROZEN, skip giliran ini
                if (player.isFrozen()) {
                    System.out.println("  [FROZEN] Anda membeku! Giliran dilewati.");
                    player.applyStatus(StatusEffect.NONE, 0);
                    playerTurn = !playerTurn;
                    input.pressEnter();
                    continue;
                }
                player.resetShield();

            } else {
                // ── Awal giliran musuh: proses status musuh ──
                String statusLog = enemy.processStatus();
                if (!statusLog.isEmpty()) System.out.print(statusLog);

                // Jika musuh terkena FROZEN, skip gilirannya
                if (enemy.isFrozen()) {
                    System.out.printf("  [FROZEN] %s membeku! Giliran dilewati.%n", enemy.getName());
                    enemy.applyStatus(StatusEffect.NONE, 0);
                    playerTurn = !playerTurn;
                    if (!enemy.isAlive()) break;
                    input.pressEnter();
                    continue;
                }
            }

            if (!enemy.isAlive() || !player.isAlive()) break;

            printBattleStatus(enemy);

            if (playerTurn) {
                doPlayerTurn(enemy);
            } else {
                printDivider();
                System.out.printf("  GILIRAN MUSUH: %s%n", enemy.getName());
                printDivider();
                // IMPLEMENTASI: Polymorphism — takeTurn() polimorfis
                enemy.takeTurn(player, null);
                if (player.isShielded()) {
                    System.out.println("  [SHIELD aktif -- damage dipotong 50%!]");
                }
            }

            playerTurn = !playerTurn;
            if (!enemy.isAlive() || !player.isAlive()) break;
            System.out.println();
            input.pressEnter();
        }

        return player.isAlive();
    }

    /** Kelola semua input dan aksi pada giliran player. */
    private void doPlayerTurn(Ship enemy) {
        printDivider();
        System.out.println("  GILIRAN ANDA");
        printDivider();
        System.out.println("  [1] Tembak Meriam   (pilih tipe peluru)");
        System.out.println("  [2] Sihir Mage      (elemen + sinergy)");
        System.out.println("  [3] Gunakan Jurus   (1x per battle!)");
        System.out.printf( "  [4] Bertahan        (-50%% damage masuk turn ini)%n");
        System.out.printf( "  [5] Minum Potion    (heal 50 HP | Sisa: %d)%n", player.getPotions());
        System.out.print("  Pilihan (1-5): ");

        int choice = input.readInt(1, 5, 1);
        System.out.println();

        switch (choice) {
            case 1: doCannonball(enemy);   break;
            case 2: doMagicAttack(enemy);  break;
            case 3: doSpellAttack(enemy);  break;
            case 4:
                player.activateShield();
                System.out.println("  Kapal bersiap bertahan! Damage masuk -50% turn ini.");
                break;
            case 5:
                if (!player.usePotion()) {
                    System.out.println("  Potion habis! Aksi dilewati.");
                } else {
                    System.out.printf("  Minum potion! HP: %d/%d | Sisa: %d%n",
                            player.getCurrentHp(), player.getMaxHp(), player.getPotions());
                }
                break;
        }
    }

    /**
     * Aksi [1]: Tembak Meriam — pemain memilih tipe peluru.
     * Setiap tipe punya trade-off strategis berbeda (lihat CannonballType.java).
     */
    private void doCannonball(Ship enemy) {
        System.out.println("  Pilih tipe peluru:");
        CannonballType[] types = CannonballType.values();

        for (int i = 0; i < types.length; i++) {
            CannonballType type = types[i];
            String stock = (type == CannonballType.IRON)
                    ? "tak terbatas"
                    : "stok: " + player.getAmmoCount(type);
            System.out.printf("  [%d] %s | %s%n",
                    i + 1, type.getFullDescription(), stock);
        }

        // Tip situasional berdasarkan trait musuh
        if (enemy instanceof EnemyShip) {
            EnemyTrait trait = ((EnemyShip) enemy).getTrait();
            if (trait == EnemyTrait.ARMORED)
                System.out.println("  [TIP] Musuh BERZIRAH -- cannon berkurang 35%, pertimbangkan sihir.");
            if (trait == EnemyTrait.REGENERATE)
                System.out.println("  [TIP] Musuh REGENERASI -- burst damage lebih efektif dari DoT.");
        }

        if (cannonDoubled) System.out.println("  [KAPAL BERKIBAR] Cannon damage x2 aktif!");

        System.out.print("  Pilih (1-4): ");
        int choice = input.readInt(1, 4, 1);
        CannonballType chosen = types[choice - 1];

        int damage = player.fireCannonball(chosen, enemy);

        // Fallback ke Iron jika stok peluru pilihan habis
        if (damage < 0) {
            System.out.printf("  Stok %s habis! Otomatis pakai Peluru Besi.%n",
                    chosen.getDisplayName());
            damage = player.fireCannonball(CannonballType.IRON, enemy);
            chosen = CannonballType.IRON;
        }

        // Kurangi damage jika musuh ARMORED
        if (enemy instanceof EnemyShip) {
            damage = ((EnemyShip) enemy).applyArmorReduction(damage, true);
        }

        // Terapkan efek kartu langka Double Cannon
        if (cannonDoubled) { damage *= 2; cannonDoubled = false; }

        System.out.printf("  Menembakkan %s! Damage: %d%n", chosen.getDisplayName(), damage);
        if (chosen.appliesBurn)
            System.out.printf("  %s terbakar! BURNED 3 turn aktif.%n", enemy.getName());
        if (chosen.appliesWeak)
            System.out.printf("  %s terjerat! WEAKENED 3 turn aktif.%n", enemy.getName());
    }

    /**
     * Aksi [2]: Sihir Mage — pilih Mage, hitung damage elemen + sinergy.
     */
    private void doMagicAttack(Ship enemy) {
        if (player.getRoster().isEmpty()) {
            System.out.println("  Tidak ada Mage! Tembak meriam Iron otomatis.");
            int dmg = player.fireCannonball(CannonballType.IRON, enemy);
            System.out.printf("  Damage: %d%n", dmg);
            return;
        }

        System.out.println("  Pilih Mage:");
        player.displayMageRoster(false);
        System.out.printf("  Elemen musuh: %-5s | Counter: %s%n",
                enemy.getElement().sym(), enemy.getElement().weakness().sym());
        printSynergyStatus();
        System.out.print("  Nomor Mage: ");

        int  mageIndex = input.readInt(1, player.getMageCount(), 1) - 1;
        Mage mage      = player.getRoster().get(mageIndex);

        double elemMult    = mage.getElement().getMultiplier(enemy.getElement());
        double synergyMult = player.getSynergyMult(mage.getElement());

        // IMPLEMENTASI: Interface — castMagic() dari MagicCastable
        int damage = player.castMagic(enemy, mage);

        // Trait THORNS: pantulkan 15% damage sihir ke pemain
        if (enemy instanceof EnemyShip && ((EnemyShip) enemy).getTrait() == EnemyTrait.THORNS) {
            int reflected = (int)(damage * 0.15);
            player.takeDamage(reflected);
            System.out.printf("  [BERDURI] %d damage dipantulkan ke Anda!%n", reflected);
        }

        System.out.printf("  %s melepas sihir %s!%n", mage.getName(), mage.getElement().sym());
        System.out.printf("  %s%n", mage.getElement().effectText(enemy.getElement()));
        if (synergyMult > 1.0)
            System.out.printf("  [SINERGY +%d%%!]%n", (int)((synergyMult - 1.0) * 100));
        System.out.printf("  Damage: %d  (x%.1f elem, x%.2f sinergy)%n",
                damage, elemMult, synergyMult);
    }

    /**
     * Aksi [3]: Gunakan Jurus — efek spesial, 1x per battle per Mage.
     */
    private void doSpellAttack(Ship enemy) {
        if (player.getRoster().isEmpty()) {
            System.out.println("  Tidak ada Mage! Tembak meriam Iron otomatis.");
            int dmg = player.fireCannonball(CannonballType.IRON, enemy);
            System.out.printf("  Damage: %d%n", dmg);
            return;
        }

        System.out.println("  Pilih Mage untuk Jurus (READY = bisa dipakai):");
        player.displayMageRoster(true); // showSpell = true
        System.out.print("  Nomor Mage: ");

        int  mageIndex = input.readInt(1, player.getMageCount(), 1) - 1;
        Mage mage      = player.getRoster().get(mageIndex);

        if (mage.isSpellUsed()) {
            System.out.printf("  [!] Jurus %s sudah dipakai! Pilih aksi lain.%n",
                    mage.getSpellType().getDisplayName());
            return;
        }

        System.out.println();
        // IMPLEMENTASI: Interface — castSpell() dari MagicCastable
        String spellResult = player.castSpell(enemy, mage);
        System.out.println(spellResult);

        // THORNS juga berlaku untuk jurus
        if (enemy instanceof EnemyShip && ((EnemyShip) enemy).getTrait() == EnemyTrait.THORNS) {
            System.out.println("  [BERDURI] Jurus dipantulkan 15% ke Anda -- cek HP!");
        }
    }

    /** Tampilkan sinergy bonus yang sedang aktif di roster. */
    private void printSynergyStatus() {
        for (Element el : Element.values()) {
            int count = player.countMageByElement(el);
            if (count >= 2) {
                System.out.printf("  [SINERGY] %dx %s -> +%d%% magic damage!%n",
                        count, el.sym(), count >= 3 ? 40 : 20);
            }
        }
    }

    // =========================================================================
    // REWARD SYSTEM — 3 kartu acak dari pool besar
    // =========================================================================

    private void handleReward() {
        List<RewardCard> cards = RewardCard.drawThree(rng, player);

        System.out.println();
        System.out.println("  +--------------------------------------------------+");
        System.out.println("  |              PILIH REWARD                        |");
        System.out.println("  +--------------------------------------------------+");
        for (int i = 0; i < cards.size(); i++) {
            System.out.printf("  [%d] %s%n", i + 1, cards.get(i).getDisplay());
        }
        System.out.print("  Pilihan (1-3): ");

        int        choice     = input.readInt(1, 3, 1);
        RewardCard chosenCard = cards.get(choice - 1);
        System.out.println();
        applyReward(chosenCard);
        System.out.println();
        input.pressEnter();
    }

    private void applyReward(RewardCard card) {
        switch (card.type) {

            case RECRUIT_MAGE: {
                if (player.getMageCount() >= 5) {
                    System.out.println("  Roster penuh! Diganti Heal Kecil.");
                    player.heal(45); break;
                }
                Mage newMage = new Mage(randomMageName(), randomElement(),
                        18 + rng.nextInt(18), rng);
                player.recruitMage(newMage);
                System.out.printf("  Mage baru bergabung: %s%n", newMage.getInfo(true));
                System.out.printf("  Jurus: %s%n", newMage.getSpellType().getFullDescription());
                int count = player.countMageByElement(newMage.getElement());
                if (count >= 2) System.out.printf(
                        "  [SINERGY TERBENTUK!] %dx %s -> +%d%% magic!%n",
                        count, newMage.getElement().sym(), count >= 3 ? 40 : 20);
                break;
            }

            case HEAL_SMALL: {
                int before = player.getCurrentHp();
                player.heal(45);
                System.out.printf("  Perbaikan cepat! HP: %d -> %d (+%d)%n",
                        before, player.getCurrentHp(), player.getCurrentHp() - before);
                break;
            }

            case HEAL_LARGE: {
                int before = player.getCurrentHp();
                player.heal(80);
                System.out.printf("  Perbaikan total! HP: %d -> %d (+%d)%n",
                        before, player.getCurrentHp(), player.getCurrentHp() - before);
                break;
            }

            case UPGRADE_CANNON: {
                int before = player.getBaseDamage();
                player.upgradeBaseDamage(7);
                System.out.printf("  Meriam di-upgrade! Damage: %d -> %d%n",
                        before, player.getBaseDamage());
                break;
            }

            case UPGRADE_MAGE_POWER: {
                if (player.getRoster().isEmpty()) {
                    System.out.println("  Tidak ada Mage! Diganti Heal Kecil.");
                    player.heal(45); break;
                }
                System.out.println("  Pilih Mage yang di-upgrade:");
                player.displayMageRoster(false);
                System.out.print("  Nomor: ");
                int  idx  = input.readInt(1, player.getMageCount(), 1) - 1;
                Mage mage = player.getRoster().get(idx);
                int before = mage.getMagicPower();
                mage.upgradePower(18);
                System.out.printf("  %s di-upgrade! Power: %d -> %d%n",
                        mage.getName(), before, mage.getMagicPower());
                break;
            }

            case ADD_POTION: {
                if (!player.addPotion()) {
                    System.out.println("  Potion penuh! Diganti Heal Kecil.");
                    player.heal(45);
                } else {
                    System.out.printf("  +1 Potion! Sisa: %d/3%n", player.getPotions());
                }
                break;
            }

            case ADD_EXPLOSIVE:
                player.addAmmo(CannonballType.EXPLOSIVE, 3);
                System.out.printf("  +3 Peluru Ledak! Stok: %d%n",
                        player.getAmmoCount(CannonballType.EXPLOSIVE));
                break;

            case ADD_CHAIN:
                player.addAmmo(CannonballType.CHAIN, 3);
                System.out.printf("  +3 Peluru Rantai! Stok: %d%n",
                        player.getAmmoCount(CannonballType.CHAIN));
                break;

            case ADD_GRAPESHOT:
                player.addAmmo(CannonballType.GRAPESHOT, 3);
                System.out.printf("  +3 Peluru Angin! Stok: %d%n",
                        player.getAmmoCount(CannonballType.GRAPESHOT));
                break;

            case UPGRADE_ALL_MAGE_SMALL:
                for (Mage m : player.getRoster()) m.upgradePower(5);
                System.out.println("  Ritual Kolektif! Semua Mage +5 Magic Power.");
                break;

            case DOUBLE_CANNON_DMG:
                cannonDoubled = true;
                System.out.println("  [LANGKA] Kapal Berkibar! Cannon damage x2 di battle berikutnya.");
                break;
        }
    }

    // =========================================================================
    // UI / DISPLAY
    // =========================================================================

    private void printStageHeader() {
        System.out.println();
        System.out.println("============================================================");
        String bossTag = (stage % 5 == 0) ? "  *** BOSS BATTLE! ***" : "";
        System.out.printf("  STAGE %d%s%n", stage, bossTag);
        System.out.println("============================================================");
        System.out.println(player.getStatusDisplay());

        // Inventori peluru
        System.out.printf("  Amunisi: Iron(inf) | Explosive: %d | Chain: %d | Grapeshot: %d%n",
                player.getAmmoCount(CannonballType.EXPLOSIVE),
                player.getAmmoCount(CannonballType.CHAIN),
                player.getAmmoCount(CannonballType.GRAPESHOT));

        // Sinergy aktif
        for (Element el : Element.values()) {
            int count = player.countMageByElement(el);
            if (count >= 2) {
                System.out.printf("  [SINERGY] %dx %s Mage -> +%d%% magic damage%n",
                        count, el.sym(), count >= 3 ? 40 : 20);
            }
        }

        System.out.println("  Kru Mage:");
        player.displayMageRoster(false);
        System.out.println();
    }

    private void printPreBattleInfo(Ship enemy) {
        System.out.println("  -- SIAP TEMPUR --");
        System.out.println(enemy.getStatusDisplay());
        System.out.println();

        // Tips berdasarkan sifat pasif musuh
        if (enemy instanceof EnemyShip) {
            EnemyTrait trait = ((EnemyShip) enemy).getTrait();
            if (trait == EnemyTrait.ARMORED)
                System.out.println("  [TIP] BERZIRAH: Utamakan sihir Mage daripada cannon.");
            if (trait == EnemyTrait.REGENERATE)
                System.out.println("  [TIP] REGENERASI: Burst damage dalam sedikit turn lebih efektif.");
            if (trait == EnemyTrait.BERSERKER)
                System.out.println("  [TIP] BERSERKER: Bunuh sebelum HP-nya turun ke 50%!");
            if (trait == EnemyTrait.THORNS)
                System.out.println("  [TIP] BERDURI: 15% sihir dipantulkan -- INFERNO atau cannon lebih aman.");
        }

        Element counter = enemy.getElement().weakness();
        System.out.printf("  Counter elemen: %s%n", counter.sym());
        if (player.hasCounterMage(enemy.getElement()))
            System.out.println("  [GOOD] Anda punya Mage counter!");
    }

    private void printBattleStatus(Ship enemy) {
        System.out.println();
        System.out.println("  ------- STATUS PERTEMPURAN ---------------------------");
        System.out.printf( "  PLAYER : %s%s%n", player.getHpBar(),
                player.isShielded() ? " [SHIELD]" : "");
        System.out.printf( "  MUSUH  : %s%n", enemy.getHpBar());
        System.out.println("  -------------------------------------------------------");
    }

    private void printVictory(EnemyShip enemy, List<String> levelUps) {
        System.out.println();
        System.out.println("  +=======================================================+");
        System.out.printf( "  | MENANG! %s tenggelam!%n", enemy.getName());
        System.out.printf( "  | Bounty: +$%d | XP Mage: +%d | Total: $%d%n",
                enemy.getBountyReward(), enemy.getXpReward(), player.getBounty());
        System.out.println("  +=======================================================+");
        for (String msg : levelUps) System.out.println(msg);
    }

    private void printGameOver() {
        System.out.println();
        System.out.println("  +=======================================================+");
        System.out.println("  |                    GAME OVER                          |");
        System.out.println("  +=======================================================+");
        System.out.printf( "  Kapal tenggelam di Stage %d.%n", stage);
        System.out.printf( "  Total Bounty: $%d%n", player.getBounty());
        System.out.println();
        System.out.println("  Kru terakhir:");
        player.displayMageRoster(false);
        System.out.println();
        System.out.println("  Terima kasih sudah bermain! Arr, sampai jumpa di lautan!");
        System.out.println();
    }

    private void printDivider() {
        System.out.println("  -------------------------------------------------------");
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Element randomElement()   { return Element.values()[rng.nextInt(3)]; }
    private String  randomEnemyName() { return ENEMY_NAMES[rng.nextInt(ENEMY_NAMES.length)]; }
    private String  randomMageName()  { return MAGE_NAMES[rng.nextInt(MAGE_NAMES.length)]; }
}

// =============================================================================
// FILE: Main.java
// Entry point program. Hanya bertugas membuat GameManager dan memulai game.
//
// CARA COMPILE (dari folder yang berisi semua file .java):
//   javac *.java
//
// CARA JALANKAN:
//   java Main
// =============================================================================
public class Main {
    public static void main(String[] args) {
        new GameManager().start();
    }
}