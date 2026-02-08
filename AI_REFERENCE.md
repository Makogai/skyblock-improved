# Skyblock Improved – AI Agent Reference

> **Purpose:** This document provides context for AI agents working on the Skyblock Improved mod. Update it after significant changes.

## Project Overview

**Skyblock Improved** is a Minecraft Fabric 1.21.11 mod for Hypixel SkyBlock. It offers a modular base with an Odin-style in-game overlay config GUI.

### Key References
- **Odin**: https://github.com/odtheking/OdinFabric – Overlay config style, modular structure
- **SkyHanni**: https://github.com/hannibal002/SkyHanni – SkyBlock tab list/scoreboard parsing, Hypixel-specific detections

---

## Architecture

### Package Structure

```
me.sbi/
├── config/          # ModuleConfig – JSON save/load for modules & settings
├── core/            # Category, Module, ModuleManager – module system
├── gui/             # ClickGui, Panel, ModuleButton – overlay config UI
├── modules/         # Feature modules by category
│   ├── general/     # CoordsDisplay, NameChanger
│   ├── mining/      # ExampleMiningModule
│   ├── render/      # ClickGuiModule
│   └── skyblock/    # SkyBlock-specific (data, detection)
├── settings/        # Setting types (Boolean, Number, String, Keybind, etc.)
└── SkyblockImproved # Client singleton (mc, configDir)
```

### Module System

- **Module**: Abstract base. `enabled`, `settings`, `onTick()`, `onEnable()`, `onDisable()`, `onKeybind()`.
- **Category**: Groups modules (GENERAL, MINING, COMBAT, RENDER, SKYBLOCK, MISC).
- **ModuleManager**: Registers modules, loads config, fires tick events, handles keybinds.
- **ModuleConfig**: Gson-based JSON persistence for module state and `Saving` settings.

### Config GUI

- **ClickGui**: Screen overlay, no pause. Uses raw screen coords (no scaling).
- **Panel**: Draggable category panel. Left-click header = drag, right-click = expand/collapse.
- **Edit GUI**: "Edit HUD" button in Click GUI (Render) opens EditHudScreen to drag HUD elements.
- **HudLayout**: Positions (x,y) for COORDS, NICK, AREA, PARTY. Saved to `config/hud.json`.
- **ModuleButton**: Left-click = toggle module, right-click = expand settings.
- **Text colors**: Must use **ARGB** (`0xAARRGGBB`). Use `0xFF` prefix for opaque text (e.g. `0xFFFFFFFF.toInt()`).

### Logging & Messaging

- **SbiLog**: `info()`, `warn()`, `error()`, `debug()` (debug only when Debug mode enabled in Click GUI).
- **SbiMessage**: `send(moduleName, message)` – format `[SBI] |module|: message`. Also `info()`, `warn()`, `error()`, `debug()`.
- **Debug mode**: Click GUI → Render → Click GUI → Debug mode. When on, SbiLog.debug and SbiMessage.debug log/send; ChatParser logs received party messages.

### Setting Types

| Type | Use Case |
|------|----------|
| `BooleanSetting` | Toggle |
| `StringSetting` | Text input (click to focus, type, Enter/Esc to confirm) |
| `NumberSetting` | Slider (min, max, step, decimals) |
| `KeybindSetting` | Key/mouse bind |
| `DropdownSetting` | Choose from options |
| `ActionSetting` | Button (runs action, not persisted) |

Settings extending `RenderableSetting<T>` appear in the GUI. Implement `Saving` for persistence.

---

## SkyBlock Data Layer

### Tab List & Scoreboard

- **Tab list**: Main area (e.g. "Deep Caverns") from Info column: header/footer, DisplaySlot.LIST scoreboard, or fake player display names ("Area: X").
- **Scoreboard (sidebar)**: Sub-area (e.g. "⏣ Gunpowder Mines") from right-side scoreboard.
- **Availability**: User must enable "Player List Info" in SkyBlock Menu → Settings → Personal → User Interface. Configurable via `/tablist` and `/widgets`.

### SkyBlockData (singleton)

- Parses tab list and scoreboard each tick when on Hypixel SkyBlock.
- Exposes: `area`, `subArea`, `tabListAvailable`, `scoreboardAvailable`, etc.
- Other modules read from `SkyBlockData`; they do not parse directly.

### Availability Detection

- If tab list lacks SkyBlock-style content → `tabListAvailable = false`.
- Can show in-game hint: "Enable Player List Info: SkyBlock Menu → Settings → User Interface → Player List Info" (similar to SkyHanni/NEU).

---

## Build & Run

- **Gradle**: `./gradlew build` (PowerShell: `.\gradlew build`)
- **Minecraft**: 1.21.11, Fabric loader
- **Mappings**: Mojang (official)
- **Keybind**: Right Shift opens ClickGui

---

## Changelog (Summary for AI)

1. **Base**: Module system, config, overlay ClickGui.
2. **GUI fixes**: Removed scaling, fixed hit testing, ARGB text colors.
3. **Settings**: Boolean, String, Number, Keybind, Dropdown, Action.
4. **SkyBlock**: Tab list + scoreboard parsing, `SkyBlockData` singleton, area/sub-area extraction.
5. **SkyBlock modules**: `SkyBlockDataModule` (internal), `AreaDisplayModule`, `ChatParserModule` (internal).
6. **Module.internal**: Set `internal = true` to hide from ClickGui.
7. **Party**: `SkyBlockParty` – parsed from chat (ChatComponentMixin). Supports "Party Leader:", "Party Members:", "Party Moderators:", "party was transferred to". Stores `PartyMember(rank, name)`. Hypixel rank colors: MVP++=gold, MVP+/MVP=aqua, VIP+/VIP=green. Auto-runs `/party list` 3s after joining Hypixel.
8. **PartyDisplayModule**: Renders party members in HUD; leader with ♔ crown, bold color.
9. **EspModule**: Stub for future party-member ESP.
