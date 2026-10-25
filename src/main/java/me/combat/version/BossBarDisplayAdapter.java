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

import me.combat.SimpleCombatPlugin;
import me.combat.manager.CombatSession;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class BossBarDisplayAdapter implements DisplayAdapter {

    private final SimpleCombatPlugin plugin;

    public BossBarDisplayAdapter(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void show(Player player, CombatSession session, String title, double progress) {
        if (player == null || !player.isOnline()) return;

        BossBar bar = (BossBar) session.getBossBar();
        if (bar == null) {
            bar = Bukkit.createBossBar(
                    title,
                    plugin.getConfigManager().getParsedBarColor(),
                    plugin.getConfigManager().getParsedBarStyle()
            );
            bar.setProgress(progress > 1.0 ? 1.0 : (progress < 0.0 ? 0.0 : progress));
            bar.addPlayer(player);
            session.setBossBar(bar);
        } else {
            if (!title.equals(bar.getTitle())) {
                bar.setTitle(title);
            }
            bar.setProgress(progress > 1.0 ? 1.0 : (progress < 0.0 ? 0.0 : progress));
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }

    @Override
    public void update(Player player, CombatSession session, String title, double progress) {
        if (session == null) return;
        BossBar bar = (BossBar) session.getBossBar();
        if (bar != null) {
            if (!title.equals(bar.getTitle())) {
                bar.setTitle(title);
            }
            bar.setProgress(progress > 1.0 ? 1.0 : (progress < 0.0 ? 0.0 : progress));
        }
    }

    @Override
    public void remove(Player player, CombatSession session) {
        if (session != null) {
            BossBar bar = (BossBar) session.getBossBar();
            if (bar != null) {
                bar.removeAll();
                session.setBossBar(null);
            }
        }
    }

    @Override
    public void removeAll() {}

    @Override
    public String getAdapterName() {
        return "Modern BossBar (1.9+)";
    }
}
