# Changelog

## v0.2 (2026-05-21)

### Architecture: 9-slice modularization

Refactored the monolithic game architecture into focused, testable modules.

- **DamagePipeline** — Consolidated all damage calculations (element/synergy/status/trait multipliers) into a single pure-function pipeline with 27 test combinations
- **StageFactory** — Extracted enemy/boss/mage generation from GameManager into a dedicated factory with variant support (NORMAL/DIFFERENT_ELEMENT/ELITE)
- **RewardCard** — Co-located reward effect logic with card definitions; each `Type.apply()` self-contains its effect
- **UI Foundation** — ViewModel records per screen type (`BattleViewModel`, `StageViewModel`, `RewardViewModel`, `TitleViewModel`); `ShipRenderer` separates display formatting from domain models; resize handling and text overflow protection
- **BattleState** — Replaced imperative BattleEngine (~280 lines) with a pure state machine (`BattleState`) + thin coordinator (~130 lines). `Command` sealed interface with 5 record subtypes; 33 new tests
- **Battle screen** — Pokemon-inspired 4-panel layout with always-visible command panel, dialog overlays for sub-selection, Esc-as-undo, and per-command focus details
- **Stage select + Reward select** — Card-based selection with counter hints, rarity stars, and keyboard navigation
- **Title + Info screens** — Name input, mage selection, generic info/pause screen
- **GameManager cleanup** — 600→370 lines; dead methods removed; all orchestration flows through dedicated modules

**Stats:** 31 files changed, 3,252 insertions(+), 797 deletions(-)
**Tests:** 118 tests, 0 failures (`mvn test`)

---

## v0.1 (2026-05-21)

### Terminal UI with Lanterna

Migrated from raw console output to a rich terminal UI powered by Lanterna.

- TerminalUi.java — 683 lines of screen rendering with dark nautical color scheme
- Scrollable choice menus with arrow key navigation
- HP bar rendering with colored segments
- Centralized balance configuration (`BalanceConfig`)
- Input validation wrapper (`InputHelper`)
- Weighted random reward system (`RewardCard`) with 11 types
- Boss mechanics: RAGE mode, boss titles every 5 stages

**Files:** 9 modified, 1,265 insertions(+), 518 deletions(-)

---

## v0

### Initial modularization

Migrated from a single ~2100-line file to a Maven multi-package project under `com.battleship`.

- Package structure: `model/`, `engine/`, `system/`, `util/`, `interfaces/`
- Abstract `Ship` class with `PlayerShip`, `EnemyShip`, `BossShip` hierarchy
- Mage system with 5 elements, 6 spells, XP/leveling
- Status effects (BURNED, WEAKENED, FROZEN)
- Rock-paper-scissors element system (FIRE > STORM > WATER > FIRE)
- 5 unit tests via JUnit 5
- Maven build with `mvn compile` / `mvn test` / `mvn exec:java`
