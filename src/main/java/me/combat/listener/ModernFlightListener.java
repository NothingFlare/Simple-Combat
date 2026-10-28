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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ModernFlightListener implements Listener {

    private final SimpleCombatPlugin plugin;
    private final CombatManager combatManager;

    public ModernFlightListener(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
        this.combatManager = plugin.getCombatManager();
    }

    private boolean hasBypassFlight(Player player) {
        return player.hasPermission("simplecombat.bypass.flight") || player.hasPermission("combat.bypass.flight");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!plugin.getConfigManager().isDisableFlight()) return;

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!combatManager.isInCombat(player)) return;

        if (hasBypassFlight(player)) return;

        // Блокировка раскрытия крыльев элитр во время боя
        if (event.isGliding()) {
            event.setCancelled(true);
            player.setGliding(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isDisableFlight()) return;

        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player)) return;

        if (hasBypassFlight(player)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        String typeName = item.getType().name();

        // Блокировка фейерверков для ускорения на элитрах во время боя
        if (typeName.contains("FIREWORK") && player.isGliding()) {
            event.setCancelled(true);
            player.setGliding(false);
            return;
        }

        // Блокировка запуска трезубца с чарами Тягун (Riptide) во время боя
        if (typeName.contains("TRIDENT") && item.hasItemMeta()) {
            try {
                if (item.getItemMeta().hasEnchants()) {
                    for (org.bukkit.enchantments.Enchantment enc : item.getItemMeta().getEnchants().keySet()) {
                        if (enc.getName().equalsIgnoreCase("RIPTIDE")) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
}
