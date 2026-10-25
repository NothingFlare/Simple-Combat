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
import org.bukkit.entity.Player;

public class DisplayManager {

    private final SimpleCombatPlugin plugin;
    private final DisplayAdapter adapter;

    public DisplayManager(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
        if (ServerVersion.supportsBossBar() && plugin.getConfigManager().isBossBarEnabled()) {
            this.adapter = new BossBarDisplayAdapter(plugin);
        } else {
            this.adapter = new ActionBarDisplayAdapter();
        }
    }

    public void show(Player player, CombatSession session, String title, double progress) {
        adapter.show(player, session, title, progress);
    }

    public void update(Player player, CombatSession session, String title, double progress) {
        adapter.update(player, session, title, progress);
    }

    public void remove(Player player, CombatSession session) {
        adapter.remove(player, session);
    }

    public void removeAll() {
        adapter.removeAll();
    }

    public String getAdapterName() {
        return adapter.getAdapterName();
    }
}
