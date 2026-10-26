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

import me.combat.manager.CombatSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActionBarDisplayAdapter implements DisplayAdapter {

    private static boolean nmsInitialized = false;
    private static Method getHandleMethod;
    private static Field playerConnectionField;
    private static Method sendPacketMethod;
    private static Constructor<?> chatPacketConstructor;
    private static Method chatSerializerMethod;

    static {
        try {
            String nmsPackage = Bukkit.getServer().getClass().getPackage().getName();
            String version = nmsPackage.substring(nmsPackage.lastIndexOf('.') + 1);

            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            getHandleMethod = craftPlayerClass.getMethod("getHandle");

            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            playerConnectionField = entityPlayerClass.getField("playerConnection");

            Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + version + ".PlayerConnection");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".Packet");
            sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);

            Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            chatSerializerMethod = chatSerializerClass.getMethod("a", String.class);

            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            chatPacketConstructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class);
            nmsInitialized = true;
        } catch (Throwable ignored) {
            nmsInitialized = false;
        }
    }

    public ActionBarDisplayAdapter() {}

    @Override
    public void show(Player player, CombatSession session, String title, double progress) {
        sendActionBar(player, title);
    }

    @Override
    public void update(Player player, CombatSession session, String title, double progress) {
        sendActionBar(player, title);
    }

    @Override
    public void remove(Player player, CombatSession session) {
        sendActionBar(player, "");
    }

    @Override
    public void removeAll() {}

    @Override
    public String getAdapterName() {
        return "Legacy ActionBar (1.8)";
    }

    private void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;

        if (nmsInitialized) {
            try {
                Object entityPlayer = getHandleMethod.invoke(player);
                Object playerConnection = playerConnectionField.get(entityPlayer);
                String json = "{\"text\": \"" + escapeJson(message) + "\"}";
                Object chatComponent = chatSerializerMethod.invoke(null, json);
                Object packet = chatPacketConstructor.newInstance(chatComponent, (byte) 2);
                sendPacketMethod.invoke(playerConnection, packet);
                return;
            } catch (Throwable ignored) {}
        }
    }

    private String escapeJson(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.indexOf('\\') == -1 && s.indexOf('"') == -1) {
            return s;
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
