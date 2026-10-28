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
import me.combat.util.ServerScheduler;
import me.combat.version.DisplayManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final SimpleCombatPlugin plugin;
    private final DisplayManager displayManager;
    private final Map<UUID, CombatSession> sessions = new ConcurrentHashMap<>();

    public CombatManager(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
        this.displayManager = new DisplayManager(plugin);
    }

    public boolean tagCombat(Player player, Player opponent) {
        return tagCombat(player, opponent, plugin.getConfigManager().getCombatSeconds());
    }

    public boolean tagCombat(Player player, Player opponent, int combatSeconds) {
        if (player == null || !player.isOnline()) return false;

        // Excluded worlds check
        if (plugin.getConfigManager().isWorldExcluded(player.getWorld())) {
            return false;
        }

        // Bypass permission check (только если включено в config.yml)
        if (plugin.getConfigManager().isEnableBypass() &&
                (player.hasPermission("simplecombat.bypass") || player.hasPermission("combat.bypass"))) {
            return false;
        }

        UUID uuid = player.getUniqueId();

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

        // Отключение полета и планирования на элитрах
        if (plugin.getConfigManager().isDisableFlight()) {
            if (!player.hasPermission("simplecombat.bypass.flight") &&
                    !player.hasPermission("combat.bypass.flight")) {
                if (player.isFlying() || player.getAllowFlight()) {
                    if (player.isFlying()) {
                        player.setFlying(false);
                    }
                    player.setAllowFlight(false);
                }
                if (me.combat.version.ServerVersion.isAtLeast(9) && player.isGliding()) {
                    player.setGliding(false);
                }
            }
        }

        // Версионно-адаптивное отображение индикатора боя (BossBar на 1.9+, ActionBar на 1.8)
        String title = formatBossBarTitle(session);
        displayManager.show(player, session, title, 1.0);

        // Уведомление отправляется только при первичном входе в бой
        if (isNew) {
            String msg = plugin.getLanguageManager().getTaggedMessage(combatSeconds, session.getOpponentName());
            if (!msg.trim().isEmpty()) {
                player.sendMessage(msg);
            }
        }

        return true;
    }

    public void clearCombat(UUID uuid, boolean notify) {
        CombatSession session = sessions.remove(uuid);
        if (session != null) {
            Player player = Bukkit.getPlayer(uuid);
            displayManager.remove(player, session);

            if (notify) {
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

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            // Если игрок переместился в мир-исключение, мгновенно снимаем с него бой
            if (plugin.getConfigManager().isWorldExcluded(player.getWorld())) {
                it.remove();
                ServerScheduler.runOnPlayer(plugin, player, () -> {
                    displayManager.remove(player, session);
                });
                continue;
            }

            // Периодический контроль запрета полета и элитр во время боя
            if (plugin.getConfigManager().isDisableFlight()) {
                if (!player.hasPermission("simplecombat.bypass.flight") &&
                        !player.hasPermission("combat.bypass.flight")) {
                    if (player.isFlying() || (me.combat.version.ServerVersion.isAtLeast(9) && player.isGliding())) {
                        ServerScheduler.runOnPlayer(plugin, player, () -> {
                            if (player.isFlying()) {
                                player.setFlying(false);
                                player.setAllowFlight(false);
                            }
                            if (me.combat.version.ServerVersion.isAtLeast(9) && player.isGliding()) {
                                player.setGliding(false);
                            }
                        });
                    }
                }
            }

            int newTime = session.getTimeLeft() - 1;
            if (newTime <= 0) {
                it.remove();

                ServerScheduler.runOnPlayer(plugin, player, () -> {
                    displayManager.remove(player, session);
                    if (player.isOnline()) {
                        String msg = plugin.getLanguageManager().getMessage("combat_ended");
                        if (!msg.trim().isEmpty()) {
                            player.sendMessage(msg);
                        }
                    }
                });
            } else {
                session.setTimeLeft(newTime);
                String title = plugin.getLanguageManager().getBossBarTitle(newTime, session.getOpponentName());
                double progress = session.getProgress();
                ServerScheduler.runOnPlayer(plugin, player, () -> {
                    displayManager.update(player, session, title, progress);
                });
            }
        }
    }

    public void tick() {
        tickAsync();
    }

    public void removeAll() {
        for (CombatSession session : sessions.values()) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            displayManager.remove(player, session);
        }
        displayManager.removeAll();
        sessions.clear();
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    private String formatBossBarTitle(CombatSession session) {
        return plugin.getLanguageManager().getBossBarTitle(session.getTimeLeft(), session.getOpponentName());
    }
}
