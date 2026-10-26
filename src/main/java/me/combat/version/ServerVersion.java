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

package me.combat.version;

import org.bukkit.Bukkit;

public final class ServerVersion {

    private static final int MAJOR;
    private static final int MINOR;
    private static final int PATCH;
    private static final String VERSION_STRING;

    private static final boolean HAS_BOSSBAR;
    private static final boolean HAS_HEX;

    static {
        String rawVersion = Bukkit.getBukkitVersion();
        String parsed = rawVersion;

        // Strip prefixes if present, e.g. "git-Paper-400 (MC: 1.21.1)"
        if (parsed.contains("(MC:")) {
            int start = parsed.indexOf("(MC:") + 4;
            int end = parsed.indexOf(')', start);
            if (end != -1) {
                parsed = parsed.substring(start, end).trim();
            }
        }
        if (parsed.contains("-")) {
            parsed = parsed.substring(0, parsed.indexOf('-')).trim();
        }

        int maj = 1;
        int min = 8;
        int pat = 0;

        try {
            String[] tokens = parsed.split("\\.");
            if (tokens.length >= 1 && isNumeric(tokens[0])) {
                maj = Integer.parseInt(tokens[0]);
            }
            if (tokens.length >= 2 && isNumeric(tokens[1])) {
                min = Integer.parseInt(tokens[1]);
            }
            if (tokens.length >= 3 && isNumeric(tokens[2])) {
                pat = Integer.parseInt(tokens[2]);
            }
        } catch (Throwable ignored) {}

        MAJOR = maj;
        MINOR = min;
        PATCH = pat;
        VERSION_STRING = maj + "." + min + (pat > 0 ? "." + pat : "");

        boolean bossbar = false;
        try {
            Class.forName("org.bukkit.boss.BossBar");
            bossbar = true;
        } catch (Throwable ignored) {
            bossbar = false;
        }
        HAS_BOSSBAR = bossbar;

        boolean hex = false;
        if (maj > 1 || (maj == 1 && min >= 16)) {
            try {
                Class<?> chatColorClass = Class.forName("net.md_5.bungee.api.ChatColor");
                chatColorClass.getMethod("of", String.class);
                hex = true;
            } catch (Throwable ignored) {
                hex = false;
            }
        }
        HAS_HEX = hex;
    }

    private ServerVersion() {}

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    public static int getMajor() {
        return MAJOR;
    }

    public static int getMinor() {
        return MINOR;
    }

    public static int getPatch() {
        return PATCH;
    }

    public static String getVersionString() {
        return VERSION_STRING;
    }

    public static boolean isAtLeast(int minorVersion) {
        if (MAJOR > 1) return true;
        return MINOR >= minorVersion;
    }

    public static boolean isLegacy18() {
        return MAJOR == 1 && MINOR <= 8;
    }

    public static boolean supportsBossBar() {
        return HAS_BOSSBAR;
    }

    public static boolean supportsHex() {
        return HAS_HEX;
    }
}
