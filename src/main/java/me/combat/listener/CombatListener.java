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

package me.combat.listener;

import me.combat.SimpleCombatPlugin;
import me.combat.manager.CombatManager;
import me.combat.manager.CombatSession;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class CombatListener implements Listener {

    private final SimpleCombatPlugin plugin;
    private final CombatManager combatManager;

    public CombatListener(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
        this.combatManager = plugin.getCombatManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = resolveAttacker(event.getDamager());

        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        // Do not tag players in spectator
        if (victim.getGameMode() == GameMode.SPECTATOR || attacker.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Do not tag in creative mode unless tag-creative is enabled in config.yml
        if (!plugin.getConfigManager().isTagCreative()) {
            if (victim.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.CREATIVE) {
                return;
            }
        }

        combatManager.tagCombat(victim, attacker);
        combatManager.tagCombat(attacker, victim);
    }

    private Player resolveAttacker(Object damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        if (damager instanceof org.bukkit.entity.AreaEffectCloud) {
            ProjectileSource source = ((org.bukkit.entity.AreaEffectCloud) damager).getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        if (damager instanceof org.bukkit.entity.TNTPrimed) {
            org.bukkit.entity.Entity source = ((org.bukkit.entity.TNTPrimed) damager).getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        if (damager instanceof org.bukkit.entity.Tameable) {
            org.bukkit.entity.AnimalTamer owner = ((org.bukkit.entity.Tameable) damager).getOwner();
            if (owner instanceof Player) {
                return (Player) owner;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        CombatSession session = combatManager.getSession(victim);
        if (session == null) return;

        UUID opponentId = session.getOpponentId();

        if (plugin.getConfigManager().isRemoveCombatOnDeath()) {
            combatManager.clearCombat(victim, false);
        }

        if (plugin.getConfigManager().isRemoveCombatOnKill() && opponentId != null) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null && opponent.isOnline()) {
                CombatSession oppSession = combatManager.getSession(opponent);
                if (oppSession != null && victim.getUniqueId().equals(oppSession.getOpponentId())) {
                    combatManager.clearCombat(opponent, true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatManager.isInCombat(player)) {
            if (plugin.getConfigManager().isPunishOnQuit() &&
                    !player.hasPermission("simplecombat.bypass.quit") &&
                    !player.hasPermission("combat.bypass.quit")) {
                player.setHealth(0.0);

                if (plugin.getConfigManager().isBroadcastOnQuit()) {
                    String msg = plugin.getLanguageManager().getQuitPunishedMessage(player.getName());
                    if (!msg.trim().isEmpty()) {
                        Bukkit.broadcastMessage(msg);
                    }
                }
            }
            combatManager.clearCombat(player, false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player)) {
            return;
        }

        if (player.hasPermission("simplecombat.bypass.commands") || player.hasPermission("combat.bypass.commands")) {
            return;
        }

        String message = event.getMessage();
        if (message.length() <= 1) return;

        // Быстрое извлечение имени команды без split и regex
        int spaceIndex = message.indexOf(' ');
        String cmd = (spaceIndex == -1) ? message.substring(1) : message.substring(1, spaceIndex);

        if (plugin.getConfigManager().isCommandBlocked(cmd)) {
            event.setCancelled(true);
            String msg = plugin.getLanguageManager().getCommandBlockedMessage(cmd);
            if (!msg.trim().isEmpty()) {
                player.sendMessage(msg);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.getConfigManager().isBlockTeleport()) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL &&
            cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT &&
            cause != PlayerTeleportEvent.TeleportCause.COMMAND &&
            cause != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player)) return;

        if (player.hasPermission("simplecombat.bypass.teleport") || player.hasPermission("combat.bypass.teleport")) return;

        event.setCancelled(true);
        String msg = plugin.getLanguageManager().getMessage("teleport_blocked");
        if (!msg.trim().isEmpty()) {
            player.sendMessage(msg);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!plugin.getConfigManager().isDisableFlight()) return;

        if (!event.isFlying()) return;

        Player player = event.getPlayer();
        if (combatManager.isInCombat(player)) {
            if (!player.hasPermission("simplecombat.bypass.flight") && !player.hasPermission("combat.bypass.flight")) {
                event.setCancelled(true);
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }
    }
}
