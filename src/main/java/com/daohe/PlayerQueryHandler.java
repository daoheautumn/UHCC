package com.daohe;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerQueryHandler {
    private final UHCCMod mod;
    private static final DecimalFormat df = new DecimalFormat("#.##");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlayerQueryHandler(UHCCMod mod) {
        this.mod = mod;
    }

    public void handlePlayerQuery(ICommandSender sender, String targetPlayer) {
        handlePlayerQuery(sender, targetPlayer, false);
    }

    public void handlePlayerQuery(ICommandSender sender, String targetPlayer, boolean forceQuery) {
        if (UHCCMod.apiKey.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.setapi.no_key")));
            return;
        }
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.c.querying", targetPlayer)));
        executor.submit(() -> {
            mod.getStatsManager().submitPlayerQuery(targetPlayer, forceQuery);
            PlayerStats stats = fetchPlayerStatsWithRetry(targetPlayer);
            if (stats == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Failed to query " + targetPlayer));
                return;
            }
            displayPlayerStats(sender, targetPlayer, stats);
        });
    }

    private PlayerStats fetchPlayerStatsWithRetry(String playerName) {
        long startTime = System.currentTimeMillis();
        long timeout = 30000;
        while (System.currentTimeMillis() - startTime < timeout) {
            PlayerStats stats = mod.getStatsManager().playerStatsMap.get(playerName);
            if (stats != null && !stats.isQuerying) {
                return stats;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        if (UHCCMod.debugMode) {
            System.out.println("Query timeout for " + playerName);
        }
        return null;
    }

    private void displayPlayerStats(ICommandSender sender, String playerName, PlayerStats stats) {
        if (stats.isQuerying) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.c.querying", playerName)));
            return;
        }
        if (stats.hasApiError) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.c.error", playerName)));
            return;
        }
        if (stats.isNick) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + ConfigManager.translate("command.uc.c.nick", playerName)));
            return;
        }
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + playerName + " - " + stats.stars + "⭐"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.c.kills") + ": " + stats.kills + " | " +
                ConfigManager.translate("command.uc.c.deaths") + ": " + stats.deaths + " | " +
                ConfigManager.translate("command.uc.c.kdr") + ": " + df.format(stats.kdr)));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.c.wins") + ": " + stats.wins + " | " +
                ConfigManager.translate("command.uc.c.score") + ": " + stats.score));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + ConfigManager.translate("command.uc.c.kit") + ": " + stats.equippedKit));
        if (!stats.artifacts.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + ConfigManager.translate("command.uc.c.artifacts") + ":"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + String.join(", ", stats.artifacts)));
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}