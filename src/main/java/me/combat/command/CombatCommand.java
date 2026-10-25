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

package me.combat.command;

import me.combat.SimpleCombatPlugin;
import me.combat.manager.CombatSession;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CombatCommand implements CommandExecutor, TabCompleter {

    private static final List<String> USER_SUBS = Collections.singletonList("status");
    private static final List<String> ADMIN_SUBS = Collections.unmodifiableList(
            Arrays.asList("status", "reload", "tag", "untag")
    );

    private final SimpleCombatPlugin plugin;

    public CombatCommand(SimpleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("simplecombat.admin") || sender.hasPermission("combat.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("time")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("only_players"));
                return true;
            }
            Player player = (Player) sender;
            CombatSession session = plugin.getCombatManager().getSession(player);
            if (session != null) {
                player.sendMessage(plugin.getLanguageManager().getStatusInCombatMessage(
                        session.getTimeLeft(), session.getOpponentName()
                ));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("combat_status_not_in_combat"));
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }
            sender.sendMessage("§e[Simple Combat] Перезагрузка конфигурации...");
            plugin.reloadPluginAsync(() -> {
                sender.sendMessage(plugin.getLanguageManager().getMessage("admin_reload"));
            });
            return true;
        }

        if (sub.equals("untag")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /" + label + " untag <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found"));
                return true;
            }
            plugin.getCombatManager().clearCombat(target, true);
            sender.sendMessage(plugin.getLanguageManager().getAdminUntagMessage(target.getName()));
            return true;
        }

        if (sub.equals("tag")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /" + label + " tag <player> [seconds]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found"));
                return true;
            }
            int seconds = plugin.getConfigManager().getCombatSeconds();
            if (args.length >= 3) {
                try {
                    seconds = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {}
            }
            plugin.getCombatManager().tagCombat(target, sender instanceof Player ? (Player) sender : null, seconds);
            sender.sendMessage(plugin.getLanguageManager().getAdminTagMessage(target.getName(), seconds));
            return true;
        }

        sender.sendMessage("§cUsage: /" + label + " [status|reload|tag|untag]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            List<String> subs = hasAdminPermission(sender) ? ADMIN_SUBS : USER_SUBS;
            String query = args[0].toLowerCase();
            for (String s : subs) {
                if (s.startsWith(query)) {
                    list.add(s);
                }
            }
            return list;
        }

        if (args.length == 2 && hasAdminPermission(sender)) {
            if (args[0].equalsIgnoreCase("tag") || args[0].equalsIgnoreCase("untag")) {
                List<String> list = new ArrayList<>();
                String query = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(query)) {
                        list.add(p.getName());
                    }
                }
                return list;
            }
        }

        return Collections.emptyList();
    }
}
