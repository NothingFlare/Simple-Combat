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

package me.combat.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.combat.SimpleCombatPlugin;
import me.combat.config.LanguageManager;
import me.combat.util.ServerScheduler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class UpdateChecker {

    public static final String GITHUB_RELEASES_URL = "https://github.com/NothingFlare/Simple-Combat/releases";
    private static final String GITHUB_API = "https://api.github.com/repos/NothingFlare/Simple-Combat/releases/latest";

    private final SimpleCombatPlugin plugin;
    private final String currentVersion;
    private final Logger logger;
    private final LanguageManager languageManager;

    public UpdateChecker(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.logger = plugin.getLogger();
        this.languageManager = plugin.getLanguageManager();
    }

    public UpdateChecker(String currentVersion, Logger logger, LanguageManager languageManager) {
        this.plugin = SimpleCombatPlugin.getInstance();
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.languageManager = languageManager;
    }

    /**
     * Запускает асинхронную проверку обновлений через GitHub API.
     */
    public void checkAsync() {
        if (plugin != null) {
            ServerScheduler.runAsync(plugin, this::checkAndLog);
        } else {
            new Thread(this::checkAndLog, "SimpleCombat-UpdateChecker").start();
        }
    }

    /**
     * Проверка версии с выводом результата в консоль.
     */
    public void checkAndLog() {
        try {
            String checkMsg = getMessage("updates_checking", "[SimpleCombat] Checking for updates...");
            logger.info(checkMsg);

            String remoteVersion = fetchLatestVersion();

            if (remoteVersion == null || remoteVersion.trim().isEmpty()) {
                String failMsg = getMessage("updates_fail", "[SimpleCombat] Could not retrieve the latest version from GitHub API.");
                logger.warning(failMsg);
                return;
            }

            int comparison = compareVersions(remoteVersion, currentVersion);
            if (comparison > 0) {
                logger.warning("****************************************************************");
                String availMsg = getUpdateAvailableMessage(remoteVersion, currentVersion);
                logger.warning(availMsg);
                String dlMsg = getMessage("updates_download", "[SimpleCombat] Download the update at:");
                logger.warning(dlMsg);
                logger.warning("[SimpleCombat]   GitHub: " + GITHUB_RELEASES_URL);
                logger.warning("****************************************************************");
            } else {
                String latestMsg = getUpdateLatestMessage(currentVersion);
                logger.info(latestMsg);
            }
        } catch (Exception e) {
            String errMsg = getUpdateErrorMessage(e.getMessage());
            logger.warning(errMsg);
        }
    }

    public String fetchLatestVersion() {
        return fetchFromGitHub();
    }

    private String fetchFromGitHub() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(GITHUB_API);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "NothingFlare/Simple-Combat/" + currentVersion);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    JsonElement root = new JsonParser().parse(reader);
                    if (root != null && root.isJsonObject()) {
                        JsonObject obj = root.getAsJsonObject();
                        if (obj.has("tag_name")) {
                            return obj.get("tag_name").getAsString().trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    public static int compareVersions(String v1, String v2) {
        if (v1 == null) v1 = "0";
        if (v2 == null) v2 = "0";

        String clean1 = v1.trim().replaceAll("^[vV]", "");
        String clean2 = v2.trim().replaceAll("^[vV]", "");

        String[] parts1 = clean1.split("[.\\-_]");
        String[] parts2 = clean2.split("[.\\-_]");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = 0;
            int num2 = 0;

            if (i < parts1.length) {
                String p1 = parts1[i].replaceAll("\\D+", "");
                if (!p1.isEmpty()) {
                    try {
                        num1 = Integer.parseInt(p1);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (i < parts2.length) {
                String p2 = parts2[i].replaceAll("\\D+", "");
                if (!p2.isEmpty()) {
                    try {
                        num2 = Integer.parseInt(p2);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    private String getMessage(String key, String def) {
        if (languageManager != null) {
            String msg = languageManager.getMessage(key);
            if (msg != null && !msg.equals(key) && !msg.trim().isEmpty()) {
                return msg;
            }
        }
        return def;
    }

    private String getUpdateAvailableMessage(String remote, String current) {
        String tpl = getMessage("updates_available", "[SimpleCombat] A new version is available: v{remote} (Current: v{current})");
        return tpl.replace("{remote}", remote).replace("{current}", current);
    }

    private String getUpdateLatestMessage(String current) {
        String tpl = getMessage("updates_latest", "[SimpleCombat] You running on the Latest version (v{version})");
        return tpl.replace("{version}", current);
    }

    private String getUpdateErrorMessage(String error) {
        String tpl = getMessage("updates_error", "[SimpleCombat] Could not check for updates: {error}");
        return tpl.replace("{error}", error != null ? error : "Unknown");
    }
}
