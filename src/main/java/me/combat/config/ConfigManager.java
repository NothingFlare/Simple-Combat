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
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final SimpleCombatPlugin plugin;

    private int combatSeconds;
    private boolean bossBarEnabled;
    private BarColor bossBarColor;
    private BarStyle bossBarStyle;
    private boolean punishOnQuit;
    private boolean broadcastOnQuit;
    private boolean removeCombatOnKill;
    private boolean removeCombatOnDeath;
    private boolean blockTeleport;
    private boolean disableFlight;
    private boolean enableBypass;
    private boolean tagCreative;
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
        String colorStr = plugin.getConfig().getString("bossbar.color", "RED").toUpperCase();
        try {
            this.bossBarColor = BarColor.valueOf(colorStr);
        } catch (IllegalArgumentException e) {
            this.bossBarColor = BarColor.RED;
        }

        String styleStr = plugin.getConfig().getString("bossbar.style", "SOLID").toUpperCase();
        try {
            this.bossBarStyle = BarStyle.valueOf(styleStr);
        } catch (IllegalArgumentException e) {
            this.bossBarStyle = BarStyle.SOLID;
        }

        this.punishOnQuit = plugin.getConfig().getBoolean("punish-on-quit", true);
        this.broadcastOnQuit = plugin.getConfig().getBoolean("broadcast-on-quit", true);
        this.removeCombatOnKill = plugin.getConfig().getBoolean("remove-combat-on-kill", true);
        this.removeCombatOnDeath = plugin.getConfig().getBoolean("remove-combat-on-death", true);
        this.blockTeleport = plugin.getConfig().getBoolean("block-teleport", true);
        this.disableFlight = plugin.getConfig().getBoolean("disable-flight", true);
        this.enableBypass = plugin.getConfig().getBoolean("enable-bypass", false);
        this.tagCreative = plugin.getConfig().getBoolean("tag-creative", false);
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

    public BarColor getBossBarColor() {
        return bossBarColor;
    }

    public BarStyle getBossBarStyle() {
        return bossBarStyle;
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
