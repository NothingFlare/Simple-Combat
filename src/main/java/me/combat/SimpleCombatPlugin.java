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

package me.combat;

import me.combat.command.CombatCommand;
import me.combat.config.ConfigManager;
import me.combat.config.LanguageManager;
import me.combat.listener.CombatListener;
import me.combat.manager.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SimpleCombatPlugin extends JavaPlugin {

    private static SimpleCombatPlugin instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private CombatManager combatManager;
    private BukkitTask tickerTask;

    @Override
    public void onEnable() {
        instance = this;

        // Load configs and language
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.languageManager = new LanguageManager(this);
        this.languageManager.load();

        // Initialize Combat Manager
        this.combatManager = new CombatManager(this);

        // Register event listener
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);

        // Register command
        CombatCommand combatCommand = new CombatCommand(this);
        PluginCommand cmd = getCommand("simplecombat");
        if (cmd != null) {
            cmd.setExecutor(combatCommand);
            cmd.setTabCompleter(combatCommand);
        }

        // Start asynchronous 1-second ticker task
        this.tickerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            combatManager.tickAsync();
        }, 20L, 20L);

        // Print startup banner
        printBanner(false);
    }

    @Override
    public void onDisable() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }

        if (combatManager != null) {
            combatManager.removeAll();
        }

        getLogger().info("Simple Combat has been disabled!");
    }

    public void reloadPluginAsync(Runnable onComplete) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            configManager.load();
            languageManager.load();
            printBanner(true);
            if (onComplete != null) {
                Bukkit.getScheduler().runTask(this, onComplete);
            }
        });
    }

    public void printBanner(boolean isReload) {
        getLogger().info("");
        getLogger().info("   ____ _                 _         ____                 _           _   ");
        getLogger().info("  / ___(_)_ __ ___  _ __ | | ___   / ___|___  _ __ ___  | |__   __ _| |_ ");
        getLogger().info("  \\___ \\| | '_ ` _ \\| '_ \\| |/ _ \\ | |   / _ \\| '_ ` _ \\ | '_ \\ / _` | __|");
        getLogger().info("   ___) | | | | | | | |_) | |  __/ | |__| (_) | | | | | || |_) | (_| | |_ ");
        getLogger().info("  |____/|_|_| |_| |_| .__/|_|\\___|  \\____\\___/|_| |_| |_||_.__/ \\__,_|\\__|");
        getLogger().info("                    |_|                                                   ");
        getLogger().info("");
        getLogger().info("Привет, мир!");
        getLogger().info("Наш Telegram канал!");
        getLogger().info("t.me/lighthouse_top");
        getLogger().info("Версия Simple Combat: " + getDescription().getVersion());
        getLogger().info("Язык: " + (languageManager != null ? languageManager.getCurrentLanguage().toUpperCase() : "RU"));
        getLogger().info("Simple Combat запущен на " + Bukkit.getName());
        if (isReload) {
            getLogger().info("Simple Combat успешно перезагружен!");
        } else {
            getLogger().info("Simple Combat успешно включен!");
        }
        getLogger().info("");
    }

    public static SimpleCombatPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }
}
