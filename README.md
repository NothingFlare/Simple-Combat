# Simple Combat

Combat tagging and anti-relog plugin for Spigot, Paper, and Purpur servers.

---

## Overview

Simple Combat tags players into a combat state upon dealing or receiving damage from other players. While tagged, players are prevented from disconnecting without penalty, running restricted commands, flying, and teleporting. The plugin utilizes an asynchronous ticker for session management, pre-cached localized message formatting, and a configurable BossBar countdown.

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
- **Target Compatibility:** Built with Bukkit api-version 1.13; compatible with Spigot, Paper, and Purpur from 1.13 to 1.21+.

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

Legacy permissions prefixed with `combat.` (`combat.admin`, `combat.bypass.*`) are also accepted.

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
