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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ServerScheduler {

    public interface TaskHandle {
        void cancel();
    }

    private static final boolean IS_FOLIA;

    private static Object asyncSchedulerObj;
    private static MethodHandle asyncRunNowHandle;
    private static MethodHandle asyncRunAtFixedRateHandle;

    private static Object globalRegionSchedulerObj;
    private static MethodHandle globalRunHandle;

    private static MethodHandle getEntitySchedulerHandle;
    private static MethodHandle entityRunHandle;

    private static MethodHandle scheduledTaskCancelHandle;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            folia = false;
        }
        IS_FOLIA = folia;

        if (IS_FOLIA) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();

                // Bukkit.getAsyncScheduler()
                Method getAsync = Bukkit.class.getMethod("getAsyncScheduler");
                asyncSchedulerObj = getAsync.invoke(null);
                Class<?> asyncClass = asyncSchedulerObj.getClass();

                // asyncScheduler.runNow(Plugin, Consumer<ScheduledTask>)
                Method runNowMethod = asyncClass.getMethod("runNow", Plugin.class, Consumer.class);
                asyncRunNowHandle = lookup.unreflect(runNowMethod);

                // asyncScheduler.runAtFixedRate(Plugin, Consumer<ScheduledTask>, long, long, TimeUnit)
                Method runAtFixedRateMethod = asyncClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                asyncRunAtFixedRateHandle = lookup.unreflect(runAtFixedRateMethod);

                // Bukkit.getGlobalRegionScheduler()
                Method getGlobal = Bukkit.class.getMethod("getGlobalRegionScheduler");
                globalRegionSchedulerObj = getGlobal.invoke(null);
                Class<?> globalClass = globalRegionSchedulerObj.getClass();

                // globalRegionScheduler.run(Plugin, Consumer<ScheduledTask>)
                Method globalRunMethod = globalClass.getMethod("run", Plugin.class, Consumer.class);
                globalRunHandle = lookup.unreflect(globalRunMethod);

                // Player.getScheduler() -> EntityScheduler
                Method getEntitySched = Player.class.getMethod("getScheduler");
                getEntitySchedulerHandle = lookup.unreflect(getEntitySched);

                Class<?> entitySchedClass = getEntitySched.getReturnType();
                // entityScheduler.run(Plugin, Consumer<ScheduledTask>, Runnable)
                Method entityRunMethod = entitySchedClass.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                entityRunHandle = lookup.unreflect(entityRunMethod);

                // ScheduledTask.cancel()
                Class<?> scheduledTaskClass = runAtFixedRateMethod.getReturnType();
                Method cancelMethod = scheduledTaskClass.getMethod("cancel");
                scheduledTaskCancelHandle = lookup.unreflect(cancelMethod);

            } catch (Throwable t) {
                Bukkit.getLogger().warning("[SimpleCombat] Failed to initialize Folia scheduler handles, falling back to Bukkit scheduler: " + t.getMessage());
            }
        }
    }

    private ServerScheduler() {}

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA && asyncRunNowHandle != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                asyncRunNowHandle.invoke(asyncSchedulerObj, plugin, consumer);
                return;
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[SimpleCombat] Error invoking Folia async runNow: " + t.getMessage());
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static TaskHandle runTimerAsync(Plugin plugin, Runnable runnable, long initialDelaySeconds, long periodSeconds) {
        if (IS_FOLIA && asyncRunAtFixedRateHandle != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = asyncRunAtFixedRateHandle.invoke(asyncSchedulerObj, plugin, consumer, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
                return () -> {
                    if (scheduledTask != null && scheduledTaskCancelHandle != null) {
                        try {
                            scheduledTaskCancelHandle.invoke(scheduledTask);
                        } catch (Throwable ignored) {}
                    }
                };
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[SimpleCombat] Error invoking Folia runAtFixedRate: " + t.getMessage());
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, initialDelaySeconds * 20L, periodSeconds * 20L);
        return task::cancel;
    }

    public static void runGlobal(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA && globalRunHandle != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                globalRunHandle.invoke(globalRegionSchedulerObj, plugin, consumer);
                return;
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[SimpleCombat] Error invoking Folia global run: " + t.getMessage());
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runOnPlayer(Plugin plugin, Player player, Runnable runnable) {
        if (player == null) return;
        if (IS_FOLIA && entityRunHandle != null && getEntitySchedulerHandle != null) {
            try {
                Object entityScheduler = getEntitySchedulerHandle.invoke(player);
                if (entityScheduler != null) {
                    Consumer<Object> consumer = task -> runnable.run();
                    entityRunHandle.invoke(entityScheduler, plugin, consumer, (Runnable) null);
                    return;
                }
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[SimpleCombat] Error invoking Folia player run: " + t.getMessage());
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
