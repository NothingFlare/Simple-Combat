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

        // Загрузка миров-исключений (поддержка boolean и строк 'true', 'yes', 'on')
        Object enabledObj = plugin.getConfig().get("excluded-worlds.enabled");
        if (enabledObj == null) {
            enabledObj = plugin.getConfig().get("disabled-worlds.enabled");
        }
        if (enabledObj instanceof Boolean) {
            this.excludedWorldsEnabled = (Boolean) enabledObj;
        } else if (enabledObj != null) {
            String s = enabledObj.toString().trim().toLowerCase();
            this.excludedWorldsEnabled = s.equals("true") || s.equals("yes") || s.equals("on") || s.equals("1");
        } else {
            this.excludedWorldsEnabled = false;
        }

        // Если указан прямой список миров без ключа enabled, считаем включенным
        if (!excludedWorldsEnabled && (plugin.getConfig().isList("excluded-worlds") || plugin.getConfig().isList("disabled-worlds"))) {
            this.excludedWorldsEnabled = true;
        }

        this.excludedWorlds.clear();
        List<String> worldsList = new java.util.ArrayList<String>();
        if (plugin.getConfig().isList("excluded-worlds.worlds")) {
            worldsList.addAll(plugin.getConfig().getStringList("excluded-worlds.worlds"));
        }
        if (plugin.getConfig().isList("disabled-worlds.worlds")) {
            worldsList.addAll(plugin.getConfig().getStringList("disabled-worlds.worlds"));
        }
        if (plugin.getConfig().isList("excluded-worlds")) {
            worldsList.addAll(plugin.getConfig().getStringList("excluded-worlds"));
        }
        if (plugin.getConfig().isList("disabled-worlds")) {
            worldsList.addAll(plugin.getConfig().getStringList("disabled-worlds"));
        }

        for (String w : worldsList) {
            if (w != null) {
                String clean = w.trim().toLowerCase();
                if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() > 1) {
                    clean = clean.substring(1, clean.length() - 1).trim();
                }
                if (clean.startsWith("'") && clean.endsWith("'") && clean.length() > 1) {
                    clean = clean.substring(1, clean.length() - 1).trim();
                }
                clean = org.bukkit.ChatColor.stripColor(clean);
                if (clean != null && !clean.isEmpty()) {
                    this.excludedWorlds.add(clean.trim().toLowerCase());
                }
            }
        }

        if (excludedWorldsEnabled) {
            plugin.getLogger().info("Миры-исключения активны: " + this.excludedWorlds);
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

        String clean = worldName.trim().toLowerCase();
        if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() > 1) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        if (clean.startsWith("'") && clean.endsWith("'") && clean.length() > 1) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }

        if (excludedWorlds.contains(clean)) return true;

        String stripped = org.bukkit.ChatColor.stripColor(clean);
        if (stripped != null && excludedWorlds.contains(stripped.trim().toLowerCase())) return true;

        return false;
    }

    public boolean isWorldExcluded(org.bukkit.World world) {
        if (!excludedWorldsEnabled || world == null) return false;

        String rawName = world.getName();
        if (isWorldExcluded(rawName)) return true;

        String stripped = org.bukkit.ChatColor.stripColor(rawName);
        if (stripped != null && isWorldExcluded(stripped)) return true;

        // Multiverse-Core интеграция (проверка alias и внутреннего имени мира в Multiverse)
        try {
            org.bukkit.plugin.Plugin mvPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (mvPlugin != null && mvPlugin.isEnabled()) {
                java.lang.reflect.Method getWorldManager = mvPlugin.getClass().getMethod("getMVWorldManager");
                Object worldManager = getWorldManager.invoke(mvPlugin);
                if (worldManager != null) {
                    java.lang.reflect.Method getMVWorld = worldManager.getClass().getMethod("getMVWorld", org.bukkit.World.class);
                    Object mvWorld = getMVWorld.invoke(worldManager, world);
                    if (mvWorld != null) {
                        java.lang.reflect.Method getAlias = mvWorld.getClass().getMethod("getAlias");
                        Object aliasObj = getAlias.invoke(mvWorld);
                        if (aliasObj != null) {
                            String alias = aliasObj.toString();
                            if (isWorldExcluded(alias)) return true;
                            String strippedAlias = org.bukkit.ChatColor.stripColor(alias);
                            if (strippedAlias != null && isWorldExcluded(strippedAlias)) return true;
                        }
                        java.lang.reflect.Method getName = mvWorld.getClass().getMethod("getName");
                        Object nameObj = getName.invoke(mvWorld);
                        if (nameObj != null && isWorldExcluded(nameObj.toString())) return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
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
