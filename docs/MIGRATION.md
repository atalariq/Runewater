# Migration Notes: Main.java → Modular Project

## Overview
Single-file application (~2100 lines) dengan 16 types (2 interfaces, 5 enums, 9 classes) telah dimodularisasi menjadi Maven project dengan separation of concerns yang jelas.

## Package Structure

```
src/main/java/com/battleship/
├── Main.java                          # Entry point (baru)
├── interfaces/
│   ├── Describable.java               # Interface untuk entitas yang bisa deskripsi
│   └── MagicCastable.java             # Interface untuk kapal yang bisa sihir
├── model/
│   ├── Ship.java                      # Abstract base class untuk semua kapal
│   ├── Mage.java                      # Penyihir kru kapal
│   ├── PlayerShip.java                # Kapal pemain (implements MagicCastable, Describable)
│   ├── EnemyShip.java                 # Kapal musuh (implements Describable)
│   ├── BossShip.java                  # Boss extends EnemyShip
│   └── enums/
│       ├── Element.java               # 3 elemen + logika segitiga
│       ├── StatusEffect.java          # BURNED, WEAKENED, FROZEN
│       ├── EnemyTrait.java            # ARMORED, REGENERATE, BERSERKER, THORNS
│       ├── SpellType.java             # 6 jurus sihir
│       └── CannonballType.java        # 4 tipe peluru meriam
├── engine/
│   └── BattleEngine.java              # Logika pertempuran + aksi pemain
└── system/
│   ├── GameManager.java               # Orchestrator & UI flow
│   └── RewardCard.java                # Sistem reward terinspirasi Balatro
└── util/
    ├── BalanceConfig.java             # Semua konstanta game
    └── InputHelper.java               # Sanitasi input
```

## Key Changes

### 1. Encapsulation Refactor
- **Ship fields**: `protected` → `private` (subclass akses via getters)
- **EnemyShip fields**: `protected` → `private` (bountyReward, xpReward, trait)
- Semua subclass (`PlayerShip`, `EnemyShip`, `BossShip`) akses parent fields via getters

### 2. Method Signature Changes
- `Ship.takeTurn(Ship opponent, Scanner scanner)` → `takeTurn(Ship opponent)`
  - Scanner parameter dihapus (tidak digunakan di model layer)
  - Input handling tetap di GameManager/BattleEngine

### 3. Class Decomposition
- **GameManager** (asli ~700 baris) → dipecah menjadi:
  - `GameManager`: Orchestrator & UI flow (~400 baris)
  - `BattleEngine`: Logika pertempuran & aksi pemain (~300 baris)
- **RewardCard**: Dari nested class di GameManager → class independent di `system/`

### 4. Visibility Changes
- Semua class/interface/enum menjadi `public` untuk cross-package access
- Memungkinkan `BattleEngine` (package `engine`) akses `Ship` (package `model`)

### 5. Build System
- **Before**: Manual `javac` compile
- **After**: Maven project (`pom.xml`)
  - Java 17
  - JUnit 5 untuk testing
  - Exec plugin untuk run

## What Stayed the Same
- Semua damage formulas identik
- Status effect logic identik
- Sinergy calculation identik
- Reward pool & weights identik
- Enemy scaling formulas identik
- UI output format identik (Bahasa Indonesia)
- Game flow & mechanics identik

## Testing
- JUnit 5 configured in `pom.xml`
- Sample tests di `src/test/java/com/battleship/`

## Files Removed
- `Main.java` (root) — replaced by modular files + new `com.battleship.Main`
