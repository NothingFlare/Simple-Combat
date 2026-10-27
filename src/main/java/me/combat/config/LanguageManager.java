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
import me.combat.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    public static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "en", "ru", "zh", "de", "fr", "es", "pt", "ja", "uk", "it", "pl", "tr", "ko"
    );

    // Предварительно кэшированные строковые представления чисел от 0 до 120 (устраняет аллокации строк каждую секунду боя)
    private static final String[] FAST_TIME = new String[121];

    static {
        for (int i = 0; i <= 120; i++) {
            FAST_TIME[i] = Integer.toString(i);
        }
    }

    public static String fastTime(int seconds) {
        if (seconds >= 0 && seconds <= 120) {
            return FAST_TIME[seconds];
        }
        return Integer.toString(seconds);
    }

    private final SimpleCombatPlugin plugin;
    private FileConfiguration langConfig;
    private String currentLanguage = "ru";

    // Кэш предварительно раскрашенных сообщений с подставленным prefix
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveDefaultLanguages();

        String lang = plugin.getConfig().getString("language", "ru").toLowerCase();
        if (!SUPPORTED_LANGUAGES.contains(lang)) {
            plugin.getLogger().warning("Unsupported language '" + lang + "' specified in config.yml! Defaulting to 'en'.");
            lang = "en";
        }
        this.currentLanguage = lang;

        File langFile = new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + langFile.getName() + " not found, defaulting to messages_en.yml.");
            langFile = new File(plugin.getDataFolder(), "lang/messages_en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Fallback to internal default resource if some keys are missing
        InputStream defaultStream = plugin.getResource("lang/messages_" + lang + ".yml");
        if (defaultStream == null) {
            defaultStream = plugin.getResource("lang/messages_en.yml");
        }
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultConfig);
        }

        // Прогрев кэша сообщений: подставляем prefix и предвычисляем цвета один раз при загрузке
        messageCache.clear();
        String rawPrefix = langConfig.getString("prefix", "");

        if (langConfig.getDefaults() != null) {
            for (String key : langConfig.getDefaults().getKeys(true)) {
                if (langConfig.getDefaults().isString(key)) {
                    String val = langConfig.getDefaults().getString(key, "");
                    if (val != null) {
                        val = val.replace("{prefix}", rawPrefix);
                        messageCache.put(key, ColorUtil.color(val));
                    }
                }
            }
        }

        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                String val = langConfig.getString(key, "");
                if (val != null) {
                    val = val.replace("{prefix}", rawPrefix);
                    messageCache.put(key, ColorUtil.color(val));
                }
            }
        }
    }

    private void saveDefaultLanguages() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        for (String lang : SUPPORTED_LANGUAGES) {
            String fileName = "lang/messages_" + lang + ".yml";
            File targetFile = new File(plugin.getDataFolder(), fileName);
            if (!targetFile.exists()) {
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                }
            }
        }
    }

    public String getRawMessage(String key) {
        if (langConfig == null) {
            return key;
        }
        return langConfig.getString(key, key);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messageCache.get(key);
        if (message == null) {
            message = getRawMessage(key);
            String prefix = getRawMessage("prefix");
            message = message.replace("{prefix}", prefix);
            message = ColorUtil.color(message);
        }

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return message;
    }

    public String getMessage(String key) {
        String msg = messageCache.get(key);
        if (msg != null) {
            return msg;
        }
        return getMessage(key, null);
    }

    // Быстрые методы форматирования частых событий без аллокации промежуточных HashMap
    public String getBossBarTitle(int timeLeft, String opponent) {
        String template = messageCache.get("bossbar_title");
        if (template == null) {
            return "§c⚔ Combat: §f" + fastTime(timeLeft) + "s";
        }
        String res = template.replace("{time}", fastTime(timeLeft));
        if (opponent != null && res.indexOf("{opponent}") != -1) {
            res = res.replace("{opponent}", opponent);
        }
        return res;
    }

    public String getTaggedMessage(int timeLeft, String opponent) {
        String template = messageCache.get("combat_tagged");
        if (template == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", fastTime(timeLeft));
            placeholders.put("opponent", opponent != null ? opponent : "");
            return getMessage("combat_tagged", placeholders);
        }
        String res = template.replace("{time}", fastTime(timeLeft));
        if (opponent != null && res.indexOf("{opponent}") != -1) {
            res = res.replace("{opponent}", opponent);
        }
        return res;
    }

    public String getCommandBlockedMessage(String command) {
        String template = messageCache.get("command_blocked");
        if (template == null) return "";
        return template.replace("{command}", command != null ? command : "");
    }

    public String getQuitPunishedMessage(String player) {
        String template = messageCache.get("quit_punished");
        if (template == null) return "";
        return template.replace("{player}", player != null ? player : "");
    }

    public String getStatusInCombatMessage(int timeLeft, String opponent) {
        String template = messageCache.get("combat_status_in_combat");
        if (template == null) return "";
        String res = template.replace("{time}", fastTime(timeLeft));
        if (opponent != null && res.indexOf("{opponent}") != -1) {
            res = res.replace("{opponent}", opponent);
        }
        return res;
    }

    public String getAdminTagMessage(String player, int seconds) {
        String template = messageCache.get("admin_tag");
        if (template == null) return "";
        return template.replace("{player}", player != null ? player : "")
                       .replace("{time}", fastTime(seconds));
    }

    public String getAdminUntagMessage(String player) {
        String template = messageCache.get("admin_untag");
        if (template == null) return "";
        return template.replace("{player}", player != null ? player : "");
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
