package com.daohe;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.text.DecimalFormat;

public class Utils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("#.##");

    public static boolean isGameStarted() {
        if (mc.thePlayer != null) {
            float maxHealth = mc.thePlayer.getMaxHealth();
            return maxHealth == 40.0f || maxHealth == 60.0f;
        }
        return false;
    }

    public static boolean isPlayerNearby(String playerName) {
        if (mc.thePlayer == null || mc.theWorld == null || !isGameStarted()) {
            return false;
        }
        EntityPlayer player = mc.theWorld.getPlayerEntityByName(playerName);
        if (player == null || player == mc.thePlayer || player.isDead) {
            return false;
        }
        double renderDistance = mc.gameSettings.renderDistanceChunks * 16;
        double distance = mc.thePlayer.getDistanceToEntity(player);
        return distance <= renderDistance && player.isEntityAlive();
    }

    public static void clearStatsAndResetTab(PlayerStatsManager statsManager, RenderHandler renderHandler) {
        statsManager.playerStatsMap.clear();
        renderHandler.needsResort = true;
        renderHandler.getCachedSortedPlayers().clear();
    }

    public static boolean isApiKeyExpired() {
        if (UHCCMod.apiKey.isEmpty() || ConfigManager.apiKeySetTime == 0) return false;
        long currentTime = System.currentTimeMillis();
        return (currentTime - ConfigManager.apiKeySetTime) > 24 * 60 * 60 * 1000;
    }
}