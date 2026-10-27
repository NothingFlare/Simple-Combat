<!--
  MIT License

  Copyright (c) 2026 NothingFlare

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
-->

# Simple Combat

Combat tagging and anti-relog plugin for Spigot, Paper, and Purpur servers.

---

## Overview

Simple Combat tags players into a combat state upon dealing or receiving damage from other players. While tagged, players are prevented from disconnecting without penalty, running specified commands, flying, and teleporting. The plugin features an asynchronous ticker for session management, pre-cached localized message formatting, and a configurable BossBar countdown.

---

## Functional Features

- **Asynchronous Execution:** Timer checks, time decrements, and string formatting are processed asynchronously. Visual BossBar updates are synchronized with the server main thread.
- **Asynchronous Configuration Reload:** File reading and message cache rebuilding during reload run off the main thread.
- **BossBar Countdown:** Real-time remaining time display with progress tracking. Configurable color and style.
- **Anti-Relog Punishment:** Configurable player kill upon disconnecting while in combat, with optional server broadcast.
- **Command Restrictions:** Blacklist or whitelist mode for command execution during combat.
- **Teleportation Restrictions:** Blocks ender pearl, chorus fruit, command, and plugin-initiated teleports during combat.
- **Flight Control:** Automatically revokes flight and disables flight permissions for tagged players.
- **Combat Source Detection:** Supports direct melee hits, projectiles (arrows, tridents), lingering potion clouds (AreaEffectCloud), primed TNT, and tamed wolves.
- **Multi-Language Support:** 13 built-in translations (en, ru, zh, de, fr, es, pt, ja, uk, it, pl, tr, ko).
- **Target Compatibility:** Compatible with Bukkit, CraftBukkit, Spigot, Paper, Purpur, and Folia from version 1.8 through 1.21.x+ and beyond.
- **Version-Adaptive Display:** Automatically uses BossBar countdown on modern versions (1.9–1.21+) and ActionBar countdown on legacy versions (1.8.x).

---

## Commands

| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/simplecombat status` | `/combat status`, `/sc status`, `/ct` | Displays current combat status and remaining time | None |
| `/simplecombat reload` | `/combat reload`, `/sc reload` | Asynchronously reloads configuration and language files | `simplecombat.admin` |
| `/simplecombat tag <player> [seconds]` | `/combat tag`, `/sc tag` | Tags specified player into combat for given duration | `simplecombat.admin` |
| `/simplecombat untag <player>` | `/combat untag`, `/sc untag` | Removes combat status from specified player | `simplecombat.admin` |

---

## Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `simplecombat.admin` | Grants permission to use reload, tag, and untag commands | `op` |
| `simplecombat.bypass` | Bypasses combat tagging entirely (requires `enable-bypass: true`) | `false` |
| `simplecombat.bypass.commands` | Allows executing restricted commands while in combat | `false` |
| `simplecombat.bypass.teleport` | Allows teleportation while in combat | `false` |
| `simplecombat.bypass.flight` | Retains flight when entering combat | `false` |
| `simplecombat.bypass.quit` | Prevents death when disconnecting in combat | `false` |

*Note: Legacy permissions prefixed with `combat.` (`combat.admin`, `combat.bypass.*`) are also accepted.*

---

## Configuration

Default `config.yml`:

```yaml
# Language: en, ru, zh, de, fr, es, pt, ja, uk, it, pl, tr, ko
language: "en"

# Combat duration in seconds
combat-seconds: 20

# BossBar settings
bossbar:
  enabled: true
  color: RED
  style: SOLID

# Kill player on disconnect during combat
punish-on-quit: true

# Broadcast message when player disconnects during combat
broadcast-on-quit: true

# Remove combat tag when opponent is killed
remove-combat-on-kill: true

# Remove combat tag on death
remove-combat-on-death: true

# Block ender pearl and chorus fruit teleports during combat
block-teleport: true

# Disable flight when entering combat
disable-flight: true

# Enable bypass permission check (simplecombat.bypass)
enable-bypass: false

# Allow combat tagging in creative mode
tag-creative: false

# Commands configuration
commands:
  whitelist-mode: false
  list:
    - "tp"
    - "tpa"
    - "spawn"
    - "home"
    - "warp"
    - "fly"
    - "god"
```

---

## Specifications

- **Server Software:** Bukkit, CraftBukkit, Spigot, Paper, Purpur, Folia (1.8 – 1.21.x+)
- **Java Compatibility:** Java 8 and higher
- **License:** MIT License
- **Contact:** [t.me/lighthouse_top](https://t.me/lighthouse_top)
