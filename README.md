# Runewater

> **Final Project Pemrograman Berorientasi Objek (OOP)**  
> Terminal UI roguelike game dengan sistem battle strategis, sihir elemen, dan deck-building Mage.

---

## Overview

**Runewater** adalah game berbasis teks dengan tampilan terminal UI (TUI) di mana pemain mengendalikan sebuah kapal bajak laut yang dilengkapi dengan penyihir (Mage). Setiap stage, pemain menghadapi musuh dengan elemen berbeda dan harus memilih strategi terbaik:

- **Tembak Meriam** — 4 tipe peluru dengan efek strategis berbeda
- **Sihir Mage** — 3 elemen (FIRE, WATER, STORM) dengan segitiga keunggulan
- **Jurus Spesial** — efek unik yang hanya bisa dipakai sekali per battle
- **Sinergy System** — 2+ Mage elemen sama = bonus damage +20% (3+ = +40%)

Game ini dirancang dengan prinsip OOP: *Inheritance*, *Polymorphism*, *Encapsulation*, *Composition*, dan *Interface*.

---

## Struktur Project

```
kapalan/
├── pom.xml                          # Maven build configuration
├── AGENTS.md                        # Konvensi untuk AI coding assistants
├── docs/
│   ├── MIGRATION.md                 # Dokumentasi perubahan dari Main.java
│   └── agents/                      # Setup konfigurasi agent skills
└── src/
    ├── main/java/com/battleship/
    │   ├── Main.java                # Entry point
    │   ├── interfaces/
    │   │   ├── Describable.java     # Kontrak deskripsi entitas
    │   │   └── MagicCastable.java   # Kontrak kapal yang bisa sihir
    │   ├── model/
    │   │   ├── Ship.java            # Abstract class — base untuk semua kapal
    │   │   ├── Mage.java            # Penyihir kru kapal
    │   │   ├── PlayerShip.java      # Kapal pemain (implements MagicCastable)
    │   │   ├── EnemyShip.java       # Musuh biasa dengan trait pasif
    │   │   ├── BossShip.java        # Boss setiap kelipatan 5 stage
    │   │   └── enums/
    │   │       ├── Element.java         # Elemen sihir (FIRE/WATER/STORM)
    │   │       ├── StatusEffect.java    # Efek status (BURNED, WEAKENED, FROZEN)
    │   │       ├── EnemyTrait.java      # Sifat pasif musuh
    │   │       ├── SpellType.java       # 6 jurus sihir
    │   │       └── CannonballType.java  # 4 tipe peluru meriam
    │   ├── engine/
    │   │   └── BattleEngine.java    # Logika pertempuran
    │   ├── system/
    │   │   ├── GameManager.java     # Orchestrator flow game
    │   │   └── RewardCard.java      # Sistem reward weighted random
    │   └── util/
    │       ├── BalanceConfig.java   # Konstanta balance game
    │       └── InputHelper.java     # Sanitasi input pemain
    └── test/java/com/battleship/
        ├── MageTest.java
        ├── PlayerShipTest.java
        ├── EnemyShipTest.java
        ├── BalanceConfigTest.java
        └── BattleEngineTest.java
```

---

## Cara Menjalankan

### Prasyarat
- Java 17 atau lebih baru
- Maven 3.6+

### Compile
```bash
mvn compile
```

### Run
```bash
mvn exec:java
```

### Test
```bash
mvn test
```

---

## Cara Bermain

### 1. Pendaftaran
- Masukkan **nama kapten**
- Pilih **Mage pertama** (FIRE/WATER/STORM + jurus spesial)

### 2. Setiap Stage
Pilih **1 dari 3 musuh** yang ditawarkan. Setiap musuh punya:
- Elemen berbeda
- Sifat pasif (ARMORED, REGENERATE, BERSERKER, THORNS)
- Stat HP dan damage yang naik tiap stage

### 3. Battle — Pilih Aksi
```
[1] Tembak Meriam    — pilih tipe peluru
[2] Sihir Mage       — serangan elemen + sinergy bonus
[3] Gunakan Jurus    — efek spesial (1x per battle)
[4] Bertahan         — -50% damage masuk turn ini
[5] Minum Potion     — heal 50 HP
```

### 4. Elemen & Sinergy
```
FIRE  > STORM > WATER > FIRE  (x2.0 super effective)
```
- **2+ Mage elemen sama** → magic damage +20%
- **3+ Mage elemen sama** → magic damage +40%

### 5. Reward
Setelah menang, pilih **1 dari 3 kartu reward**:
- Rekrut Mage baru
- Upgrade meriam / mage power
- Heal / tambah potion / amunisi
- **[LANGKA]** Cannon damage x2 di battle berikutnya

### 6. Boss
Setiap **kelipatan 5 stage** muncul boss dengan:
- HP dan damage lebih tinggi
- **Fase RAGE** (HP < 50%) — menyerang 2x per giliran!

---

## Kontribusi

Project ini adalah **tugas kelompok** untuk mata kuliah Pemrograman Berorientasi Objek. Kontribusi dilakukan melalui GitHub Issues dan Pull Request.

### Workflow
1. **Fork** repo ini
2. **Buat branch** dari `main`: `git checkout -b feat/nama-fitur`
3. **Commit** perubahan dengan [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat:` — fitur baru
   - `fix:` — perbaikan bug
   - `refactor:` — perubahan struktur tanpa mengubah behavior
   - `test:` — menambah/mengubah test
   - `docs:` — dokumentasi
4. **Push** ke fork: `git push origin feat/nama-fitur`
5. **Buat PR** ke branch `main` repo ini

### Konvensi Kode
- Package root: `com.battleship`
- Field: `private` dengan getters/setters (encapsulation)
- Kode & komentar: **Bahasa Inggris**
- String UI (yang dilihat pemain): **Bahasa Indonesia**

---

## Teknologi

- **Java 17** — LTS version
- **Maven** — build tool & dependency management
- **JUnit 5** — unit testing framework

---

## Lisensi

Project ini dibuat untuk tujuan akademik. Hak cipta milik kontributor.
