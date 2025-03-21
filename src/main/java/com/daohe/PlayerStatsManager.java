package com.daohe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlayerStatsManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final UHCCMod mod;
    public final ConcurrentHashMap<String, PlayerStats> playerStatsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<?>> playerQueryTasks = new ConcurrentHashMap<>();
    private ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Map<String, CachedPlayerStats> statsCache = new HashMap<>();
    private static final long CACHE_DURATION = 1800 * 1000;
    private static final List<String> ARTIFACTS_TO_CHECK = Arrays.asList(
            "artemis_bow", "flask_of_ichor", "exodus", "hide_of_leviathan", "tablets_of_destiny",
            "axe_of_perun", "excalibur", "anduril", "deaths_scythe", "chest_of_fate",
            "cornucopia", "essence_of_yggdrasil", "voidbox", "deus_ex_machina", "dice_of_god",
            "kings_rod", "daredevil", "flask_of_cleansing", "shoes_of_vidar", "potion_of_vitality",
            "miners_blessing", "ambrosia", "bloodlust", "modular_bow", "expert_seal",
            "hermes_boots", "barbarian_chestplate"
    );

    public PlayerStatsManager(UHCCMod mod) {
        this.mod = mod;
    }

    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
            if (UHCCMod.debugMode) {
                System.out.println("Executor shutdown");
            }
        }
    }

    public void submitPlayerQuery(String playerName) {
        submitPlayerQuery(playerName, false);
    }

    public void submitPlayerQuery(String playerName, boolean forceQuery) {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            restartExecutor();
        }
        try {
            PlayerStats stats = playerStatsMap.get(playerName);
            if (stats == null || stats.isQuerying || stats.hasApiError || forceQuery) {
                playerStatsMap.put(playerName, new PlayerStats(false, true));
            }
            Future<?> task = executor.submit(() -> getPlayerStats(playerName, forceQuery));
            playerQueryTasks.put(playerName, task);
            if (UHCCMod.debugMode) {
                System.out.println("Query started for " + playerName + (forceQuery ? " (forced)" : ""));
            }
        } catch (Exception e) {
            if (UHCCMod.debugMode) {
                System.out.println("Query submission failed for " + playerName + ": " + e.getMessage());
            }
        }
    }

    public void cancelPlayerQuery(String playerName) {
        Future<?> task = playerQueryTasks.remove(playerName);
        if (task != null && !task.isDone()) {
            task.cancel(true);
        }
    }

    public void cancelAllQueries() {
        for (Map.Entry<String, Future<?>> entry : playerQueryTasks.entrySet()) {
            Future<?> task = entry.getValue();
            if (!task.isDone()) {
                task.cancel(true);
            }
        }
        playerQueryTasks.clear();
        if (UHCCMod.debugMode) {
            System.out.println("All queries canceled");
        }
    }

    private void restartExecutor() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        executor = Executors.newFixedThreadPool(5);
        if (UHCCMod.debugMode) {
            System.out.println("Executor restarted");
        }
    }

    public PlayerStats getPlayerStats(String playerName) {
        PlayerStats stats = playerStatsMap.get(playerName);
        if (stats == null || stats.isQuerying || stats.hasApiError) {
            submitPlayerQuery(playerName, false);
            return playerStatsMap.getOrDefault(playerName, new PlayerStats(false, true));
        }
        if (statsCache.containsKey(playerName)) {
            CachedPlayerStats cached = statsCache.get(playerName);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION) {
                stats = new PlayerStats(
                        cached.stars, cached.kills, cached.deaths, cached.kdr, cached.wins,
                        cached.score, cached.equippedKit, cached.artifacts
                );
                stats.isNick = false;
                stats.isQuerying = true;
                stats.queryStartTime = System.currentTimeMillis();
                playerStatsMap.put(playerName, stats);
                if (UHCCMod.debugMode) {
                    System.out.println("Loaded cached stats for " + playerName + ", starting delay");
                }
                final PlayerStats finalStats = stats;
                final boolean isNick = cached.isNick;
                executor.submit(() -> {
                    try {
                        long minDelay = 1000;
                        double randomDelay = 500 + Math.random() * 1500;
                        Thread.sleep(minDelay + (long) randomDelay);
                        PlayerStats updatedStats = new PlayerStats(
                                finalStats.stars, finalStats.kills, finalStats.deaths, finalStats.kdr,
                                finalStats.wins, finalStats.score, finalStats.equippedKit, finalStats.artifacts
                        );
                        updatedStats.isNick = isNick;
                        updatedStats.isQuerying = false;
                        updatedStats.currentGameKills = finalStats.currentGameKills;
                        if (updatedStats.isNick) {
                            updatedStats.generateObfuscatedData();
                        }
                        playerStatsMap.put(playerName, updatedStats);
                        if (UHCCMod.debugMode) {
                            System.out.println("Finished delay for " + playerName);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                return stats;
            } else {
                statsCache.remove(playerName);
                submitPlayerQuery(playerName, false);
                return playerStatsMap.getOrDefault(playerName, new PlayerStats(false, true));
            }
        }
        return stats;
    }

    private void getPlayerStats(String playerName, boolean forceQuery) {
        if (UHCCMod.apiKey.isEmpty()) {
            if (UHCCMod.debugMode) {
                System.out.println("Query aborted for " + playerName + ": No API key");
            }
            return;
        }
        if (!playerName.matches("^[a-zA-Z0-9_]{3,16}$")) {
            mc.thePlayer.addChatMessage(new ChatComponentText("Invalid player name: " + playerName));
            return;
        }
        if (!forceQuery && statsCache.containsKey(playerName)) {
            CachedPlayerStats cached = statsCache.get(playerName);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION) {
                PlayerStats stats = new PlayerStats(
                        cached.stars, cached.kills, cached.deaths, cached.kdr, cached.wins,
                        cached.score, cached.equippedKit, cached.artifacts
                );
                stats.isNick = false;
                stats.isQuerying = true;
                stats.queryStartTime = System.currentTimeMillis();
                playerStatsMap.put(playerName, stats);
                playerQueryTasks.remove(playerName);
                if (UHCCMod.debugMode) {
                    System.out.println("Loaded cached stats for " + playerName + ", starting delay");
                }
                final boolean isNick = cached.isNick;
                try {
                    long minDelay = 1000;
                    double randomDelay = 500 + Math.random() * 1500;
                    Thread.sleep(minDelay + (long) randomDelay);
                    stats.isQuerying = false;
                    stats.isNick = isNick;
                    if (stats.isNick) {
                        stats.generateObfuscatedData();
                    }
                    playerStatsMap.put(playerName, stats);
                    if (UHCCMod.debugMode) {
                        System.out.println("Finished delay for " + playerName);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            } else {
                statsCache.remove(playerName);
            }
        }
        try {
            String uuid = getUUIDFromMojang(playerName);
            if (uuid == null) {
                PlayerStats existingStats = playerStatsMap.get(playerName);
                if (existingStats != null && existingStats.isNick) {
                    return;
                }
                PlayerStats nickStats = new PlayerStats(false, true);
                playerStatsMap.put(playerName, nickStats);
                playerQueryTasks.remove(playerName);
                executor.submit(() -> {
                    try {
                        long minDelay = 1000;
                        double randomDelay = 500 + Math.random() * 1500;
                        Thread.sleep(minDelay + (long) randomDelay);
                        nickStats.isQuerying = false;
                        nickStats.isNick = true;
                        nickStats.generateObfuscatedData();
                        playerStatsMap.put(playerName, nickStats);
                        statsCache.put(playerName, new CachedPlayerStats(nickStats, System.currentTimeMillis()));
                        if (UHCCMod.debugMode) {
                            System.out.println("Nick stats generated for " + playerName + " after delay");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                return;
            }
            JsonObject hypixelResponse = getHypixelStats(uuid, playerName);
            if (hypixelResponse == null) {
                return;
            }
            if (hypixelResponse.get("success").getAsBoolean() && hypixelResponse.get("player").isJsonNull()) {
                if (UHCCMod.debugMode) {
                    System.out.println(playerName + " marked as nick (Hypixel: success=true, player=null)");
                }
                PlayerStats nickStats = new PlayerStats(false, true);
                playerStatsMap.put(playerName, nickStats);
                playerQueryTasks.remove(playerName);
                executor.submit(() -> {
                    try {
                        long minDelay = 1000;
                        double randomDelay = 500 + Math.random() * 1500;
                        Thread.sleep(minDelay + (long) randomDelay);
                        nickStats.isQuerying = false;
                        nickStats.isNick = true;
                        nickStats.generateObfuscatedData();
                        playerStatsMap.put(playerName, nickStats);
                        statsCache.put(playerName, new CachedPlayerStats(nickStats, System.currentTimeMillis()));
                        if (UHCCMod.debugMode) {
                            System.out.println("Nick stats generated for " + playerName + " after delay");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                return;
            }
            JsonObject player = hypixelResponse.getAsJsonObject("player");
            if (player == null) {
                return;
            }
            JsonObject stats = player.getAsJsonObject("stats");
            if (stats == null || !stats.has("UHC")) {
                if (UHCCMod.debugMode) {
                    System.out.println(playerName + " no UHC stats");
                }
                PlayerStats statsData = new PlayerStats(0, 0, 0, 0.0, 0, 0, "None", new ArrayList<>());
                playerStatsMap.put(playerName, statsData);
                playerQueryTasks.remove(playerName);
                statsCache.put(playerName, new CachedPlayerStats(statsData, System.currentTimeMillis()));
                return;
            }
            JsonObject uhcStats = stats.getAsJsonObject("UHC");
            int kills = uhcStats.has("kills") && !uhcStats.get("kills").isJsonNull() ? uhcStats.get("kills").getAsInt() : 0;
            int killsSolo = uhcStats.has("kills_solo") && !uhcStats.get("kills_solo").isJsonNull() ? uhcStats.get("kills_solo").getAsInt() : 0;
            int deaths = uhcStats.has("deaths") && !uhcStats.get("deaths").isJsonNull() ? uhcStats.get("deaths").getAsInt() : 0;
            int deathsSolo = uhcStats.has("deaths_solo") && !uhcStats.get("deaths_solo").isJsonNull() ? uhcStats.get("deaths_solo").getAsInt() : 0;
            int wins = uhcStats.has("wins") && !uhcStats.get("wins").isJsonNull() ? uhcStats.get("wins").getAsInt() : 0;
            int winsSolo = uhcStats.has("wins_solo") && !uhcStats.get("wins_solo").isJsonNull() ? uhcStats.get("wins_solo").getAsInt() : 0;
            int score = uhcStats.has("score") && !uhcStats.get("score").isJsonNull() ? uhcStats.get("score").getAsInt() : 0;
            String equippedKit = uhcStats.has("equippedKit") && !uhcStats.get("equippedKit").isJsonNull() ? uhcStats.get("equippedKit").getAsString() : "None";
            int totalKills = kills + killsSolo;
            int totalDeaths = deaths + deathsSolo;
            double kdr = totalDeaths == 0 ? 0.0 : (double) totalKills / totalDeaths;
            int totalWins = wins + winsSolo;
            int stars = calculateUHCStars(score);
            List<String> artifacts = new ArrayList<>();
            if (uhcStats.has("packages") && uhcStats.get("packages").isJsonArray()) {
                JsonArray packages = uhcStats.getAsJsonArray("packages");
                for (int i = 0; i < packages.size(); i++) {
                    String packageName = packages.get(i).getAsString();
                    int index = ARTIFACTS_TO_CHECK.indexOf(packageName);
                    if (index != -1) {
                        String translatedArtifact = ConfigManager.translate("artifact." + packageName);
                        artifacts.add(translatedArtifact);
                    }
                }
            }
            PlayerStats statsData = new PlayerStats(stars, totalKills, totalDeaths, kdr, totalWins, score, equippedKit, artifacts);
            playerStatsMap.put(playerName, statsData);
            playerQueryTasks.remove(playerName);
            statsCache.put(playerName, new CachedPlayerStats(statsData, System.currentTimeMillis()));
        } catch (Exception e) {
            if (UHCCMod.debugMode) {
                System.out.println("Unexpected error for " + playerName + ": " + e.getMessage());
            }
        }
    }

    private String getUUIDFromMojang(String playerName) {
        if (UHCCMod.debugMode) {
            System.out.println("Fetching UUID for " + playerName);
        }
        while (true) {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    JsonObject response = new JsonParser().parse(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                    if (response.has("id")) {
                        Thread.sleep(1000);
                        return response.get("id").getAsString();
                    }
                } else if (responseCode == 429) {
                    String retryAfterStr = conn.getHeaderField("Retry-After");
                    int retryAfter = retryAfterStr != null ? Integer.parseInt(retryAfterStr) * 1000 : 5000;
                    if (UHCCMod.debugMode) {
                        System.out.println("Mojang rate limit hit for " + playerName + ", waiting " + (retryAfter / 1000) + "s");
                    }
                    Thread.sleep(retryAfter);
                } else if (responseCode == 404) {
                    try (InputStreamReader reader = new InputStreamReader(conn.getErrorStream())) {
                        JsonObject errorResponse = new JsonParser().parse(reader).getAsJsonObject();
                        if (errorResponse.has("errorMessage") &&
                                errorResponse.get("errorMessage").getAsString().toLowerCase().contains("couldn't find any profile")) {
                            if (UHCCMod.debugMode) {
                                System.out.println(playerName + " marked as nick (Mojang: couldn't find profile)");
                            }
                            Thread.sleep(1000);
                            return null;
                        }
                    }
                } else {
                    if (UHCCMod.debugMode) {
                        System.out.println("Mojang error for " + playerName + ": " + responseCode);
                    }
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                if (UHCCMod.debugMode) {
                    System.out.println("Mojang API error for " + playerName + ": " + e.getMessage());
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }

    private JsonObject getHypixelStats(String uuid, String playerName) {
        while (true) {
            try {
                URL url = new URL("https://api.hypixel.net/player?key=" + UHCCMod.apiKey + "&uuid=" + uuid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    JsonObject response = new JsonParser().parse(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                    int remaining = Integer.parseInt(conn.getHeaderField("ratelimit-remaining") != null ? conn.getHeaderField("ratelimit-remaining") : "299");
                    int reset = Integer.parseInt(conn.getHeaderField("ratelimit-reset") != null ? conn.getHeaderField("ratelimit-reset") : "60");
                    if (remaining <= 1) {
                        if (UHCCMod.debugMode) {
                            System.out.println("Hypixel rate limit low for " + playerName + ", waiting " + reset + "s");
                        }
                        Thread.sleep(reset * 1000L);
                    }
                    return response;
                } else if (responseCode == 429) {
                    int reset = Integer.parseInt(conn.getHeaderField("ratelimit-reset") != null ? conn.getHeaderField("ratelimit-reset") : "60");
                    if (UHCCMod.debugMode) {
                        System.out.println("Hypixel rate limit hit for " + playerName + ", waiting " + reset + "s");
                    }
                    Thread.sleep(reset * 1000L);
                } else {
                    if (UHCCMod.debugMode) {
                        System.out.println("Hypixel error for " + playerName + ": " + responseCode);
                    }
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                if (UHCCMod.debugMode) {
                    System.out.println("Hypixel API error for " + playerName + ": " + e.getMessage());
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }

    private int calculateUHCStars(int score) {
        int[] thresholds = {0, 10, 60, 210, 460, 960, 1710, 2710, 5210, 10210, 13210, 16210, 19210, 22210, 25210};
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (score >= thresholds[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    private static class CachedPlayerStats {
        int stars;
        int kills;
        int deaths;
        double kdr;
        int wins;
        int score;
        String equippedKit;
        boolean isNick;
        List<String> artifacts;
        long timestamp;

        CachedPlayerStats(PlayerStats stats, long timestamp) {
            this.stars = stats.stars;
            this.kills = stats.kills;
            this.deaths = stats.deaths;
            this.kdr = stats.kdr;
            this.wins = stats.wins;
            this.score = stats.score;
            this.equippedKit = stats.equippedKit;
            this.isNick = stats.isNick;
            this.artifacts = new ArrayList<>(stats.artifacts);
            this.timestamp = timestamp;
        }
    }
}