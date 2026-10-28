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
import me.combat.util.ServerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleCombatPlugin extends JavaPlugin {

    private static SimpleCombatPlugin instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private CombatManager combatManager;
    private ServerScheduler.TaskHandle tickerTask;

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

        // Register 1.9+ Elytra, fireworks and modern flight restrictions
        if (me.combat.version.ServerVersion.isAtLeast(9)) {
            Bukkit.getPluginManager().registerEvents(new me.combat.listener.ModernFlightListener(this), this);
        }

        // Register command
        CombatCommand combatCommand = new CombatCommand(this);
        PluginCommand cmd = getCommand("simplecombat");
        if (cmd != null) {
            cmd.setExecutor(combatCommand);
            cmd.setTabCompleter(combatCommand);
        }

        // Start asynchronous 1-second ticker task (compatible with Folia & Paper/Spigot)
        this.tickerTask = ServerScheduler.runTimerAsync(this, () -> {
            combatManager.tickAsync();
        }, 1L, 1L);

        // Прогрев (Warmup) классов и пакетов BossBar для исключения микрофриза при первом ударе
        warmup();

        // Print startup banner
        printBanner(false);
    }

    private void warmup() {
        try {
            // Принудительная предварительная загрузка классов в память JVM
            Class.forName(me.combat.manager.CombatSession.class.getName());
            Class.forName(me.combat.version.ServerVersion.class.getName());
            Class.forName(me.combat.version.DisplayManager.class.getName());
            Class.forName(me.combat.version.BossBarDisplayAdapter.class.getName());
            Class.forName(me.combat.version.ActionBarDisplayAdapter.class.getName());
            Class.forName(me.combat.util.ColorUtil.class.getName());

            // Прогрев шаблонов форматирования строк
            languageManager.getBossBarTitle(20, "Player");
            languageManager.getTaggedMessage(20, "Player");

            // Прогрев структур пакетов BossBar на этапе загрузки сервера
            if (me.combat.version.ServerVersion.supportsBossBar()) {
                org.bukkit.boss.BossBar warmupBar = Bukkit.createBossBar(
                        "",
                        configManager.getParsedBarColor(),
                        configManager.getParsedBarStyle()
                );
                warmupBar.removeAll();
            }
        } catch (Throwable ignored) {}
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
        ServerScheduler.runAsync(this, () -> {
            configManager.load();
            languageManager.load();
            printBanner(true);
            if (onComplete != null) {
                ServerScheduler.runGlobal(this, onComplete);
            }
        });
    }

    public void printBanner(boolean isReload) {
        getLogger().info("");
        getLogger().info("   _____ _                 _        _____                 _           _   ");
        getLogger().info("  / ____(_)               | |      / ____|               | |         | |  ");
        getLogger().info(" | (___  _ _ __ ___  _ __ | | ___ | |     ___  _ __ ___  | |__   __ _| |_ ");
        getLogger().info("  \\___ \\| | '_ ` _ \\| '_ \\| |/ _ \\| |    / _ \\| '_ ` _ \\ | '_ \\ / _` | __|");
        getLogger().info("  ____) | | | | | | | |_) | |  __/| |___| (_) | | | | | || |_) | (_| | |_ ");
        getLogger().info(" |_____/|_|_| |_| |_| .__/|_|\\___| \\_____\\___/|_| |_| |_||_.__/ \\__,_|\\__|");
        getLogger().info("                    | |                                                   ");
        getLogger().info("                    |_|                                                   ");
        getLogger().info("");
        getLogger().info(" Привет, мир!");
        getLogger().info(" Наш Telegram канал!");
        getLogger().info(" t.me/lighthouse_top");
        getLogger().info(" Версия Simple Combat: " + getDescription().getVersion());
        getLogger().info(" Язык: " + (languageManager != null ? languageManager.getCurrentLanguage().toUpperCase() : "RU"));
        String platform = ServerScheduler.isFolia() ? "Folia" : Bukkit.getName();
        getLogger().info(" Simple Combat запущен на " + platform);
        if (configManager != null && configManager.isExcludedWorldsEnabled()) {
            getLogger().info(" Исключенные миры: " + configManager.getExcludedWorlds());
        }
        if (isReload) {
            getLogger().info(" Simple Combat успешно перезагружен!");
        } else {
            getLogger().info(" Simple Combat успешно включен!");
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
