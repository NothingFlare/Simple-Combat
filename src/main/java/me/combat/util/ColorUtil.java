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

package me.combat.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PATTERN_3 = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
    private static final boolean SUPPORTS_HEX;

    static {
        boolean hex = false;
        try {
            // Проверяем, поддерживает ли сервер Bungee ChatColor HEX или версию 1.16+
            Class<?> chatColorClass = Class.forName("net.md_5.bungee.api.ChatColor");
            chatColorClass.getMethod("of", String.class);
            hex = true;
        } catch (Throwable ignored) {
            hex = false;
        }
        SUPPORTS_HEX = hex;
    }

    private ColorUtil() {}

    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Fast path: если в строке нет '#', пропускаем регулярные выражения HEX
        if (message.indexOf('#') != -1) {
            if (SUPPORTS_HEX) {
                if (message.contains("&#")) {
                    message = applyHex(message, HEX_PATTERN_1);
                }
                if (message.contains("<#")) {
                    message = applyHex(message, HEX_PATTERN_2);
                }
                if (message.contains("{#")) {
                    message = applyHex(message, HEX_PATTERN_3);
                }
            } else {
                // На версиях до 1.16 заменяем HEX на пустую строку, чтобы не было кракозябр
                if (message.contains("&#")) {
                    message = HEX_PATTERN_1.matcher(message).replaceAll("");
                }
                if (message.contains("<#")) {
                    message = HEX_PATTERN_2.matcher(message).replaceAll("");
                }
                if (message.contains("{#")) {
                    message = HEX_PATTERN_3.matcher(message).replaceAll("");
                }
            }
        }

        // Fast path: если в строке нет '&', пропускаем трансляцию Bukkit цветов
        if (message.indexOf('&') != -1) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        return message;
    }

    public static List<String> color(List<String> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<String> colored = new ArrayList<>(list.size());
        for (String s : list) {
            colored.add(color(s));
        }
        return colored;
    }

    private static String applyHex(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return message;
        }
        StringBuffer buffer = new StringBuffer(message.length() + 32);
        do {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (int i = 0; i < 6; i++) {
                replacement.append('§').append(hexCode.charAt(i));
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        } while (matcher.find());
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
