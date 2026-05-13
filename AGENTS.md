# AGENTS.md

This file contains context and conventions for AI coding assistants working on this repository.

## Project Overview

**Roguelike Pirate Mage Battle** — A console-based roguelike game written in Java as a final OOP project. Modularized into a Maven multi-package project from a single-file application (~2100 lines).

## Agent skills

### Issue tracker

GitHub Issues (`atalariq/kapalan`). See `docs/agents/issue-tracker.md`.

### Triage labels

Default canonical labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context repo. See `docs/agents/domain.md`.

## Build & Run

- **Build tool**: Maven (transitioning from `javac` manual compile)
- **Java version**: 17
- **Compile**: `mvn compile`
- **Run**: `mvn exec:java`
- **Test**: `mvn test`

## Code Style

- Package root: `com.battleship`
- Prefer Indonesian for user-facing strings, English for code/comments
- Encapsulation: `private` fields with getters/setters
- Interfaces: `Describable`, `MagicCastable`
- Abstract class: `Ship` with `takeTurn()` and `getStatusDisplay()`
