/*
 * MIT License
 *
 * Copyright (c) 2026 NothingFlare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.combat.config;

import me.combat.SimpleCombatPlugin;
import me.combat.version.ServerVersion;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final SimpleCombatPlugin plugin;

    private int combatSeconds;
    private boolean bossBarEnabled;
    private String bossBarColor;
    private String bossBarStyle;
    private BarColor parsedBarColor = BarColor.RED;
    private BarStyle parsedBarStyle = BarStyle.SOLID;
    private boolean punishOnQuit;
    private boolean broadcastOnQuit;
    private boolean removeCombatOnKill;
    private boolean removeCombatOnDeath;
    private boolean blockTeleport;
    private boolean disableFlight;
    private boolean enableBypass;
    private boolean tagCreative;
    private boolean excludedWorldsEnabled;
    private final Set<String> excludedWorlds = new HashSet<>();
    private boolean whitelistMode;
    private final Set<String> blockedCommands = new HashSet<>();

    public ConfigManager(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        this.combatSeconds = plugin.getConfig().getInt("combat-seconds", 20);

        this.bossBarEnabled = plugin.getConfig().getBoolean("bossbar.enabled", true);
        this.bossBarColor = plugin.getConfig().getString("bossbar.color", "RED").toUpperCase();
        this.bossBarStyle = plugin.getConfig().getString("bossbar.style", "SOLID").toUpperCase();

        // Предварительный парсинг Enum при загрузке, чтобы не делать lookup на каждый удар
        if (ServerVersion.supportsBossBar()) {
            try {
                this.parsedBarColor = BarColor.valueOf(this.bossBarColor);
            } catch (Throwable ignored) {
                this.parsedBarColor = BarColor.RED;
            }
            try {
                this.parsedBarStyle = BarStyle.valueOf(this.bossBarStyle);
            } catch (Throwable ignored) {
                this.parsedBarStyle = BarStyle.SOLID;
            }
        }

        this.punishOnQuit = plugin.getConfig().getBoolean("punish-on-quit", true);
        this.broadcastOnQuit = plugin.getConfig().getBoolean("broadcast-on-quit", true);
        this.removeCombatOnKill = plugin.getConfig().getBoolean("remove-combat-on-kill", true);
        this.removeCombatOnDeath = plugin.getConfig().getBoolean("remove-combat-on-death", true);
        this.blockTeleport = plugin.getConfig().getBoolean("block-teleport", true);
        this.disableFlight = plugin.getConfig().getBoolean("disable-flight", true);
        this.enableBypass = plugin.getConfig().getBoolean("enable-bypass", false);
        this.tagCreative = plugin.getConfig().getBoolean("tag-creative", false);
        this.excludedWorldsEnabled = plugin.getConfig().getBoolean("excluded-worlds.enabled",
                plugin.getConfig().getBoolean("disabled-worlds.enabled", false));
        this.excludedWorlds.clear();
        List<String> worldsList = plugin.getConfig().getStringList("excluded-worlds.worlds");
        if (worldsList.isEmpty()) {
            worldsList = plugin.getConfig().getStringList("disabled-worlds.worlds");
        }
        if (worldsList.isEmpty()) {
            worldsList = plugin.getConfig().getStringList("excluded-worlds");
        }
        for (String w : worldsList) {
            if (w != null) {
                String clean = w.trim().toLowerCase();
                if (!clean.isEmpty()) {
                    this.excludedWorlds.add(clean);
                }
            }
        }
        this.whitelistMode = plugin.getConfig().getBoolean("commands.whitelist-mode", false);

        this.blockedCommands.clear();
        List<String> list = plugin.getConfig().getStringList("commands.list");
        for (String cmd : list) {
            String clean = cmd.trim().toLowerCase();
            if (clean.startsWith("/")) {
                clean = clean.substring(1);
            }
            if (!clean.isEmpty()) {
                this.blockedCommands.add(clean);
            }
        }
    }

    public int getCombatSeconds() {
        return combatSeconds;
    }

    public boolean isBossBarEnabled() {
        return bossBarEnabled;
    }

    public String getBossBarColor() {
        return bossBarColor;
    }

    public String getBossBarStyle() {
        return bossBarStyle;
    }

    public BarColor getParsedBarColor() {
        return parsedBarColor;
    }

    public BarStyle getParsedBarStyle() {
        return parsedBarStyle;
    }

    public boolean isPunishOnQuit() {
        return punishOnQuit;
    }

    public boolean isBroadcastOnQuit() {
        return broadcastOnQuit;
    }

    public boolean isRemoveCombatOnKill() {
        return removeCombatOnKill;
    }

    public boolean isRemoveCombatOnDeath() {
        return removeCombatOnDeath;
    }

    public boolean isBlockTeleport() {
        return blockTeleport;
    }

    public boolean isDisableFlight() {
        return disableFlight;
    }

    public boolean isEnableBypass() {
        return enableBypass;
    }

    public boolean isTagCreative() {
        return tagCreative;
    }

    public boolean isExcludedWorldsEnabled() {
        return excludedWorldsEnabled;
    }

    public Set<String> getExcludedWorlds() {
        return excludedWorlds;
    }

    public boolean isWorldExcluded(String worldName) {
        if (!excludedWorldsEnabled || worldName == null) return false;
        return excludedWorlds.contains(worldName.toLowerCase());
    }

    public boolean isWorldExcluded(org.bukkit.World world) {
        if (!excludedWorldsEnabled || world == null) return false;
        return isWorldExcluded(world.getName());
    }

    public boolean isCommandBlocked(String commandName) {
        if (commandName == null || commandName.isEmpty()) return false;

        String clean = commandName;
        if (clean.charAt(0) == '/') {
            clean = clean.substring(1);
        }
        int colon = clean.indexOf(':');
        if (colon != -1) {
            clean = clean.substring(colon + 1);
        }
        clean = clean.trim().toLowerCase();

        boolean listed = blockedCommands.contains(clean);
        return whitelistMode ? !listed : listed;
    }
}
