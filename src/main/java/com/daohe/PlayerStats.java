package com.daohe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerStats {
    public int stars;
    public int kills;
    public int deaths;
    public double kdr;
    public int wins;
    public int score;
    public String equippedKit;
    public boolean isNick;
    public boolean isQuerying;
    public boolean hasApiError;
    public List<String> artifacts;
    public boolean isNearbyCached = false;
    public int currentGameKills;
    public long queryStartTime = 0;
    public String nickObfuscatedStars = "";
    public String nickObfuscatedKdr = "";
    public String nickObfuscatedWins = "";
    private static final Random random = new Random();

    public PlayerStats(int stars, int kills, int deaths, double kdr, int wins, int score, String equippedKit, List<String> artifacts) {
        this.stars = stars;
        this.kills = kills;
        this.deaths = deaths;
        this.kdr = kdr;
        this.wins = wins;
        this.score = score;
        this.equippedKit = equippedKit;
        this.isNick = false;
        this.isQuerying = false;
        this.hasApiError = false;
        this.artifacts = artifacts;
        this.currentGameKills = 0;
        this.queryStartTime = System.currentTimeMillis();
    }

    public PlayerStats(boolean isNick) {
        this.isNick = isNick;
        this.isQuerying = false;
        this.stars = 0;
        this.kills = 0;
        this.deaths = 0;
        this.kdr = 0.0;
        this.wins = 0;
        this.score = 0;
        this.equippedKit = "None";
        this.hasApiError = false;
        this.artifacts = new ArrayList<>();
        this.currentGameKills = 0;
        this.queryStartTime = System.currentTimeMillis();
        if (isNick) {
            generateObfuscatedData();
        }
    }

    public PlayerStats(boolean isNick, boolean isQuerying) {
        this.isNick = isNick;
        this.isQuerying = isQuerying;
        this.stars = 0;
        this.kills = 0;
        this.deaths = 0;
        this.kdr = 0.0;
        this.wins = 0;
        this.score = 0;
        this.equippedKit = "None";
        this.hasApiError = false;
        this.artifacts = new ArrayList<>();
        this.currentGameKills = 0;
        this.queryStartTime = System.currentTimeMillis();
        if (isNick && !isQuerying) {
            generateObfuscatedData();
        }
    }

    public void generateObfuscatedData() {
        this.nickObfuscatedStars = "§e§k" + random.nextInt(10) + "§r";
        int kdrDigits = random.nextBoolean() ? 2 : 3;
        this.nickObfuscatedKdr = "§c§k" + (kdrDigits == 2 ? random.nextInt(90) + 10 : random.nextInt(900) + 100) + "§r";
        int winsDigits = random.nextBoolean() ? 1 : 2;
        this.nickObfuscatedWins = "§6§k" + (winsDigits == 1 ? random.nextInt(10) : random.nextInt(11) + 10) + "§r";
    }

    public double getKdr() {
        return kdr;
    }
}