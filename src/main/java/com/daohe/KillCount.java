package com.daohe;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.concurrent.ConcurrentHashMap;

public class KillCount {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final UHCCMod mod;
    private final ConcurrentHashMap<String, Integer> killCounts = new ConcurrentHashMap<>();
    private boolean isGameActive = false;

    public KillCount(UHCCMod mod) {
        this.mod = mod;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;

        float maxHealth = mc.thePlayer.getMaxHealth();
        if (!isGameActive && mod.isDetecting && (maxHealth == 40.0F || maxHealth == 60.0F)) {
            isGameActive = true;
            killCounts.clear();
            System.out.println("检测到血量变动/游戏开始");
            if (UHCCMod.debugMode) {
                System.out.println("Game started: Max health = " + maxHealth);
            }
        } else if (isGameActive && maxHealth == 20.0F) {
            isGameActive = false;
            System.out.println("游戏结束");
            resetKills();
            if (UHCCMod.debugMode) {
                System.out.println("Game ended: Max health = 20");
            }
        }

        if (isGameActive) {
            for (String playerName : killCounts.keySet()) {
                UHCCMod.PlayerStats stats = mod.playerStatsMap.get(playerName);
                if (stats != null) {
                    stats.currentGameKills = killCounts.getOrDefault(playerName, 0);
                }
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isGameActive || !mod.isDetecting) return;

        String unformattedMessage = event.message.getUnformattedText();
        if (!unformattedMessage.contains(" by ")) return;

        if (unformattedMessage.contains("> ") || unformattedMessage.contains(": ")) {
            if (UHCCMod.debugMode) {
                System.out.println("忽略玩家聊天消息: " + unformattedMessage);
            }
            return;
        }

        String[] parts = unformattedMessage.split(" ");
        if (parts.length < 2) return;

        String killed = parts[0];
        int byIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("by")) {
                byIndex = i;
                break;
            }
        }

        if (byIndex == -1 || byIndex + 1 >= parts.length) return;

        String rawKiller = parts[byIndex + 1];
        String killer = rawKiller.replaceAll("[^a-zA-Z0-9_]", "");

        if (!mod.playerStatsMap.containsKey(killer)) {
            if (UHCCMod.debugMode) {
                System.out.println("忽略击杀消息：击杀者 " + killer + " 不在 Tab 列表中 (原始名称: " + rawKiller + ")");
            }
            return;
        }

        int kills = killCounts.getOrDefault(killer, 0) + 1;
        killCounts.put(killer, kills);
        System.out.println("检测到击杀，击杀者：" + killer);
        if (UHCCMod.debugMode) {
            System.out.println(killer + " made a kill, total kills: " + kills);
        }
    }

    public void resetKills() {
        killCounts.clear();
        for (UHCCMod.PlayerStats stats : mod.playerStatsMap.values()) {
            stats.currentGameKills = 0;
        }
        if (UHCCMod.debugMode) {
            System.out.println("Kill counts reset");
        }
    }

    public static void appendKillCount(StringBuilder display, UHCCMod.PlayerStats stats) {
        if (stats.currentGameKills > 0) {
            display.append(" ").append(net.minecraft.util.EnumChatFormatting.DARK_RED).append("K: ").append(stats.currentGameKills).append(net.minecraft.util.EnumChatFormatting.RESET);
        }
    }
}