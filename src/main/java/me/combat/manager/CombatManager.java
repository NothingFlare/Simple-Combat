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

package me.combat.manager;

import me.combat.SimpleCombatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final SimpleCombatPlugin plugin;
    private final Map<UUID, CombatSession> sessions = new ConcurrentHashMap<>();

    public CombatManager(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public void tagCombat(Player player, Player opponent) {
        if (player == null || !player.isOnline()) return;

        // Bypass permission check
        if (plugin.getConfigManager().isEnableBypass() &&
                (player.hasPermission("simplecombat.bypass") || player.hasPermission("combat.bypass"))) {
            return;
        }

        UUID uuid = player.getUniqueId();
        int combatSeconds = plugin.getConfigManager().getCombatSeconds();

        // Одиночный поиск вместо containsKey + computeIfAbsent (избегает лишнего хэширования и аллокации лямбды)
        CombatSession session = sessions.get(uuid);
        boolean isNew = (session == null);

        if (isNew) {
            session = new CombatSession(uuid, opponent != null ? opponent.getUniqueId() : null,
                    opponent != null ? opponent.getName() : null, combatSeconds);
            sessions.put(uuid, session);
        } else {
            session.resetTime(combatSeconds);
            if (opponent != null) {
                session.setOpponent(opponent.getUniqueId(), opponent.getName());
            }
        }

        // Disable flight if enabled in config
        if (plugin.getConfigManager().isDisableFlight() &&
                !player.hasPermission("simplecombat.bypass.flight") &&
                !player.hasPermission("combat.bypass.flight")) {
            if (player.isFlying()) {
                player.setFlying(false);
            }
            player.setAllowFlight(false);
        }

        // Управление BossBar (без двойного вызова formatBossBarTitle)
        if (plugin.getConfigManager().isBossBarEnabled()) {
            BossBar bar = session.getBossBar();
            String title = formatBossBarTitle(session);
            if (bar == null) {
                bar = Bukkit.createBossBar(
                        title,
                        plugin.getConfigManager().getBossBarColor(),
                        plugin.getConfigManager().getBossBarStyle()
                );
                bar.addPlayer(player);
                session.setBossBar(bar);
            } else {
                bar.setTitle(title);
            }
            bar.setProgress(1.0);
            bar.setVisible(true);
        }

        // Send tag notification if new
        if (isNew) {
            String msg = plugin.getLanguageManager().getTaggedMessage(combatSeconds, session.getOpponentName());
            if (!msg.trim().isEmpty()) {
                player.sendMessage(msg);
            }
        }
    }

    public void clearCombat(UUID uuid, boolean notify) {
        CombatSession session = sessions.remove(uuid);
        if (session != null) {
            BossBar bar = session.getBossBar();
            if (bar != null) {
                bar.removeAll();
                session.setBossBar(null);
            }

            if (notify) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    String msg = plugin.getLanguageManager().getMessage("combat_ended");
                    if (!msg.trim().isEmpty()) {
                        player.sendMessage(msg);
                    }
                }
            }
        }
    }

    public void clearCombat(Player player, boolean notify) {
        if (player != null) {
            clearCombat(player.getUniqueId(), notify);
        }
    }

    public boolean isInCombat(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public boolean isInCombat(Player player) {
        return player != null && isInCombat(player.getUniqueId());
    }

    public CombatSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public CombatSession getSession(Player player) {
        return player != null ? getSession(player.getUniqueId()) : null;
    }

    public void tickAsync() {
        if (sessions.isEmpty()) return;

        Iterator<Map.Entry<UUID, CombatSession>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CombatSession> entry = it.next();
            UUID uuid = entry.getKey();
            CombatSession session = entry.getValue();

            int newTime = session.getTimeLeft() - 1;
            if (newTime <= 0) {
                it.remove();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    BossBar bar = session.getBossBar();
                    if (bar != null) {
                        bar.removeAll();
                        session.setBossBar(null);
                    }
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        String msg = plugin.getLanguageManager().getMessage("combat_ended");
                        if (!msg.trim().isEmpty()) {
                            player.sendMessage(msg);
                        }
                    }
                });
            } else {
                session.setTimeLeft(newTime);
                BossBar bar = session.getBossBar();
                if (bar != null) {
                    String title = plugin.getLanguageManager().getBossBarTitle(newTime, session.getOpponentName());
                    double progress = session.getProgress();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        bar.setTitle(title);
                        bar.setProgress(progress);
                    });
                }
            }
        }
    }

    public void tick() {
        tickAsync();
    }

    public void removeAll() {
        for (CombatSession session : sessions.values()) {
            BossBar bar = session.getBossBar();
            if (bar != null) {
                bar.removeAll();
            }
        }
        sessions.clear();
    }

    private String formatBossBarTitle(CombatSession session) {
        return plugin.getLanguageManager().getBossBarTitle(session.getTimeLeft(), session.getOpponentName());
    }
}
