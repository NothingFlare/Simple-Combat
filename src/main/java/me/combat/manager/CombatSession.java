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

import org.bukkit.boss.BossBar;

import java.util.UUID;

public class CombatSession {

    private final UUID playerId;
    private UUID opponentId;
    private String opponentName;
    private int timeLeft;
    private int maxTime;
    private BossBar bossBar;

    public CombatSession(UUID playerId, UUID opponentId, String opponentName, int maxTime) {
        this.playerId = playerId;
        this.opponentId = opponentId;
        this.opponentName = opponentName;
        this.timeLeft = maxTime;
        this.maxTime = maxTime;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getOpponentId() {
        return opponentId;
    }

    public void setOpponent(UUID opponentId, String opponentName) {
        this.opponentId = opponentId;
        this.opponentName = opponentName;
    }

    public String getOpponentName() {
        return opponentName != null ? opponentName : "";
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public void resetTime(int seconds) {
        this.timeLeft = seconds;
        this.maxTime = seconds;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public double getProgress() {
        if (maxTime <= 0) return 0.0;
        double progress = (double) timeLeft / maxTime;
        return progress > 1.0 ? 1.0 : (progress < 0.0 ? 0.0 : progress);
    }
}
