package com.daohe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private final UHCCMod mod;
    private long lastUpdateTime = 0;
    public static volatile boolean isStopped = false;

    public EventHandler(UHCCMod mod) {
        this.mod = mod;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (isStopped || event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !UHCCMod.isDetecting) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= 1000) {
            updatePlayerListFromTab();
            lastUpdateTime = currentTime;

            if (Utils.isGameStarted()) {
                for (Map.Entry<String, PlayerStats> entry : mod.getStatsManager().playerStatsMap.entrySet()) {
                    String playerName = entry.getKey();
                    PlayerStats stats = entry.getValue();
                    boolean isNearby = Utils.isPlayerNearby(playerName);
                    if (stats.isNearbyCached != isNearby) {
                        stats.isNearbyCached = isNearby;
                        mod.getRenderHandler().needsResort = true;
                    }
                }
            }

            if (!UHCCMod.apiKey.isEmpty() && Utils.isApiKeyExpired()) {
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.setapi.expired")));
                isStopped = true;
                UHCCMod.isDetecting = false;
                Utils.clearStatsAndResetTab(mod.getStatsManager(), mod.getRenderHandler());
                mod.getStatsManager().cancelAllQueries();
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (isStopped || mc.thePlayer == null || mc.theWorld == null || UHCCMod.apiKey.isEmpty()) return;

        String message = event.message.getUnformattedText();
        if (message.contains(" has joined ") && message.contains("/") && UHCCMod.isDetecting) {
            String playerName = message.split(" has joined ")[0].trim();
            if (UHCCMod.debugMode) {
                System.out.println(playerName + " joined");
            }
            mod.getStatsManager().playerStatsMap.put(playerName, new PlayerStats(false, true));
            mod.getStatsManager().submitPlayerQuery(playerName);
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (RenderHandler.overlayKey.isKeyDown() && Mouse.isButtonDown(1)) {
            if (!mod.getRenderHandler().getWasRightClickPressed()) {
                int totalPages = (int) Math.ceil((double) mod.getStatsManager().playerStatsMap.size() / RenderHandler.overlayMaxPlayers);
                mod.getRenderHandler().setCurrentPage(mod.getRenderHandler().getCurrentPage() + 1);
                if (mod.getRenderHandler().getCurrentPage() >= totalPages) {
                    mod.getRenderHandler().setCurrentPage(0);
                }
                if (UHCCMod.debugMode) {
                    System.out.println("Overlay page flipped to: " + (mod.getRenderHandler().getCurrentPage() + 1));
                }
                mod.getRenderHandler().setWasRightClickPressed(true);
            }
        } else {
            mod.getRenderHandler().setWasRightClickPressed(false);
        }
    }

    public void updatePlayerListFromTab() {
        if (mc.getNetHandler() == null || UHCCMod.apiKey.isEmpty()) return;

        Collection<NetworkPlayerInfo> playerInfoMap = mc.getNetHandler().getPlayerInfoMap();
        if (playerInfoMap == null) return;

        Set<String> currentPlayers = new HashSet<>();
        for (NetworkPlayerInfo playerInfo : playerInfoMap) {
            if (playerInfo == null || playerInfo.getGameProfile() == null) continue;
            String playerName = playerInfo.getGameProfile().getName();
            if (playerName == null) continue;
            currentPlayers.add(playerName);
            if (!mod.getStatsManager().playerStatsMap.containsKey(playerName)) {
                if (UHCCMod.debugMode) {
                    System.out.println(playerName + " joined");
                }
                mod.getStatsManager().playerStatsMap.put(playerName, new PlayerStats(false, true));
                mod.getStatsManager().submitPlayerQuery(playerName);
            }
        }

        mod.getStatsManager().playerStatsMap.entrySet().removeIf(entry -> {
            String playerName = entry.getKey();
            boolean shouldRemove = !currentPlayers.contains(playerName);
            if (shouldRemove) {
                if (UHCCMod.debugMode) {
                    System.out.println(playerName + " quit, removing from list and canceling query");
                }
                mod.getStatsManager().cancelPlayerQuery(playerName);
                mod.getStatsManager().playerStatsMap.remove(playerName);
            }
            return shouldRemove;
        });
        mod.getRenderHandler().needsResort = true;
    }
}