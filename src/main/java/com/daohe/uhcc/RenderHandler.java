package com.daohe.uhcc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RenderHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("#.##");
    private final UHCCMod mod;
    public static float overlayX = 155.0f;
    public static float overlayY = 15.0f;
    public static float overlayScale = 0.8f;
    public static float nametagHeight = 1.3f;
    public static boolean showOverlayStats = true;
    public static boolean showNametagStats = true;
    public static int overlayMaxPlayers = 25;
    public static KeyBinding overlayKey;
    private int currentPage = 0;
    private boolean wasRightClickPressed = false;
    public boolean needsResort = true;
    private List<Map.Entry<String, PlayerStats>> cachedSortedPlayers = new ArrayList<>();

    public RenderHandler(UHCCMod mod) {
        this.mod = mod;
        overlayKey = new KeyBinding("Open Overlay", Keyboard.KEY_GRAVE, "UHCC");
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (UHCCMod.apiKey.isEmpty()) return;
        if (event.type == RenderGameOverlayEvent.ElementType.ALL && showOverlayStats) {
            if (overlayKey.isKeyDown()) {
                renderPlayerStatsOverlay();
            }
        }
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Specials.Pre event) {
        if (!showNametagStats || !UHCCMod.isDetecting || event.entity == null || !(event.entity instanceof net.minecraft.entity.player.EntityPlayer)) return;
        String playerName = event.entity.getName();
        PlayerStats stats = mod.getStatsManager().playerStatsMap.get(playerName);
        if (stats == null) return;
        StringBuilder display = new StringBuilder();
        if (stats.isQuerying) {
            display.append(EnumChatFormatting.GRAY).append(ConfigManager.translate("tag.querying"));
        } else if (stats.isNick) {
            display.append(EnumChatFormatting.DARK_PURPLE).append(ConfigManager.translate("tag.nick"));
            KillCount.appendKillCount(display, stats);
        } else {
            display.append(EnumChatFormatting.RESET);
            display.append(EnumChatFormatting.GOLD).append(stats.stars).append("✫ ");
            display.append(EnumChatFormatting.GREEN).append("KDR: ").append(df.format(stats.kdr)).append(" ");
            display.append(EnumChatFormatting.YELLOW).append("W: ").append(stats.wins);
            KillCount.appendKillCount(display, stats);
        }
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef((float) event.x, (float) event.y + event.entity.height + nametagHeight, (float) event.z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-0.025F, -0.025F, 0.025F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            FontRenderer fontRenderer = mc.fontRendererObj;
            int width = fontRenderer.getStringWidth(display.toString());
            fontRenderer.drawStringWithShadow(display.toString(), -width / 2, 0, 0xFFFFFF);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_LIGHTING);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void renderPlayerStatsOverlay() {
        if (!UHCCMod.isDetecting || mc.thePlayer == null || mc.theWorld == null) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(overlayX, overlayY, 0.0f);
            GL11.glScalef(overlayScale, overlayScale, 1.0f);
            if (needsResort) {
                List<Map.Entry<String, PlayerStats>> allPlayers = new ArrayList<>(mod.getStatsManager().playerStatsMap.entrySet());
                cachedSortedPlayers.clear();
                String localPlayerName = mc.thePlayer.getName();
                List<Map.Entry<String, PlayerStats>> user = new ArrayList<>();
                List<Map.Entry<String, PlayerStats>> nearbyNicks = new ArrayList<>();
                List<Map.Entry<String, PlayerStats>> nearbyRegulars = new ArrayList<>();
                List<Map.Entry<String, PlayerStats>> nicks = new ArrayList<>();
                List<Map.Entry<String, PlayerStats>> regulars = new ArrayList<>();
                List<Map.Entry<String, PlayerStats>> querying = new ArrayList<>();
                for (Map.Entry<String, PlayerStats> entry : allPlayers) {
                    String playerName = entry.getKey();
                    PlayerStats stats = entry.getValue();
                    boolean isNearby = stats.isNearbyCached;
                    if (playerName.equals(localPlayerName)) {
                        user.add(entry);
                    } else if (stats.isQuerying) {
                        querying.add(entry);
                    } else if (isNearby && stats.isNick) {
                        nearbyNicks.add(entry);
                    } else if (isNearby) {
                        nearbyRegulars.add(entry);
                    } else if (stats.isNick) {
                        nicks.add(entry);
                    } else {
                        regulars.add(entry);
                    }
                }
                nearbyRegulars.sort(Comparator.comparingDouble((Map.Entry<String, PlayerStats> entry) -> entry.getValue().getKdr()).reversed());
                regulars.sort(Comparator.comparingDouble((Map.Entry<String, PlayerStats> entry) -> entry.getValue().getKdr()).reversed());
                cachedSortedPlayers.addAll(user);
                cachedSortedPlayers.addAll(nearbyNicks);
                cachedSortedPlayers.addAll(nearbyRegulars);
                cachedSortedPlayers.addAll(nicks);
                cachedSortedPlayers.addAll(regulars);
                cachedSortedPlayers.addAll(querying);
                needsResort = false;
            }
            FontRenderer fontRenderer = mc.fontRendererObj;
            int totalPlayers = cachedSortedPlayers.size();
            int rowHeight = 10;
            int y = 0;
            int totalPages = (int) Math.ceil((double) totalPlayers / overlayMaxPlayers);
            String titleText = ConfigManager.translate("mod.overlay.total_players", totalPlayers, currentPage + 1, totalPages);
            int[] baseColumnOffsets = new int[] {0, 40, 70, 150, 180};
            String[] columnHeaders = new String[] {
                    ConfigManager.translate("overlay.header.tag"),
                    ConfigManager.translate("overlay.header.stars"),
                    ConfigManager.translate("overlay.header.player"),
                    ConfigManager.translate("overlay.header.kdr"),
                    ConfigManager.translate("overlay.header.wins")
            };
            int[] adjustedColumnOffsets = new int[baseColumnOffsets.length];
            System.arraycopy(baseColumnOffsets, 0, adjustedColumnOffsets, 0, baseColumnOffsets.length);
            int startIdx = currentPage * overlayMaxPlayers;
            int endIdx = Math.min(startIdx + overlayMaxPlayers, totalPlayers);
            int maxWidth = 0;
            int visibleRows = Math.min(endIdx - startIdx, overlayMaxPlayers) + 2;
            int backgroundHeight = visibleRows * rowHeight;
            boolean hasKills = false;
            for (Map.Entry<String, PlayerStats> entry : cachedSortedPlayers) {
                if (entry.getValue().currentGameKills > 0) {
                    hasKills = true;
                    break;
                }
            }
            int killColumnOffset = adjustedColumnOffsets[4] + 50;
            if (hasKills) {
                killColumnOffset = adjustedColumnOffsets[4] + 50;
            }
            int titleWidth = fontRenderer.getStringWidth(titleText);
            if (titleWidth > maxWidth) {
                maxWidth = titleWidth;
            }
            for (int i = startIdx; i < endIdx; i++) {
                Map.Entry<String, PlayerStats> entry = cachedSortedPlayers.get(i);
                String playerName = entry.getKey();
                PlayerStats stats = entry.getValue();
                EnumChatFormatting nameColor = getPlayerColor(playerName);
                String tag = getPlayerTag(playerName, stats, false);
                int tagWidth = fontRenderer.getStringWidth(tag);
                int offsetIncrease = 0;
                if (!stats.isQuerying && tagWidth > (adjustedColumnOffsets[1] - adjustedColumnOffsets[0])) {
                    offsetIncrease = tagWidth - (adjustedColumnOffsets[1] - adjustedColumnOffsets[0]) + 5;
                    for (int j = 1; j < adjustedColumnOffsets.length; j++) {
                        adjustedColumnOffsets[j] += offsetIncrease;
                    }
                }
                String starsDisplay = "";
                int starsColor = 0xFFFFFF;
                if (stats.isQuerying) {
                    starsDisplay = "";
                } else if (stats.isNick) {
                    starsDisplay = EnumChatFormatting.DARK_GRAY.toString() + "-";
                    starsColor = 0x555555;
                } else if (!stats.hasApiError) {
                    String starsNumColor = stats.stars == 0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.YELLOW.toString();
                    starsDisplay = starsNumColor + stats.stars + EnumChatFormatting.YELLOW.toString() + "✰" + EnumChatFormatting.RESET.toString();
                    starsColor = stats.stars == 0 ? 0x808080 : 0xFFFF00;
                }
                int starsWidth = fontRenderer.getStringWidth(starsDisplay);
                if (starsWidth > (adjustedColumnOffsets[2] - adjustedColumnOffsets[1])) {
                    offsetIncrease = starsWidth - (adjustedColumnOffsets[2] - adjustedColumnOffsets[1]) + 10;
                    for (int j = 2; j < adjustedColumnOffsets.length; j++) {
                        adjustedColumnOffsets[j] += offsetIncrease;
                    }
                }
                String nameDisplay = nameColor + playerName + EnumChatFormatting.RESET.toString();
                int nameWidth = fontRenderer.getStringWidth(nameDisplay);
                if (nameWidth > (adjustedColumnOffsets[3] - adjustedColumnOffsets[2])) {
                    offsetIncrease = nameWidth - (adjustedColumnOffsets[3] - adjustedColumnOffsets[2]) + 10;
                    for (int j = 3; j < adjustedColumnOffsets.length; j++) {
                        adjustedColumnOffsets[j] += offsetIncrease;
                    }
                }
                String kdrDisplay = "";
                int kdrColor = 0xFFFFFF;
                if (stats.isQuerying) {
                    kdrDisplay = "";
                } else if (stats.isNick) {
                    kdrDisplay = EnumChatFormatting.DARK_GRAY.toString() + "-";
                    kdrColor = 0x555555;
                } else if (!stats.hasApiError) {
                    String kdrNumColor = stats.kdr == 0.0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.RED.toString();
                    kdrDisplay = kdrNumColor + df.format(stats.kdr) + EnumChatFormatting.RESET.toString();
                    kdrColor = stats.kdr == 0.0 ? 0x808080 : 0xFF0000;
                }
                int kdrWidth = fontRenderer.getStringWidth(kdrDisplay);
                if (kdrWidth > (adjustedColumnOffsets[4] - adjustedColumnOffsets[3])) {
                    offsetIncrease = kdrWidth - (adjustedColumnOffsets[4] - adjustedColumnOffsets[3]) + 15;
                    adjustedColumnOffsets[4] += offsetIncrease;
                }
                String winsDisplay = "";
                int winsColor = 0xFFFFFF;
                if (stats.isQuerying) {
                    winsDisplay = "";
                } else if (stats.isNick) {
                    winsDisplay = EnumChatFormatting.DARK_GRAY.toString() + "-";
                    winsColor = 0x555555;
                } else if (!stats.hasApiError) {
                    String winsNumColor = stats.wins == 0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.GOLD.toString();
                    winsDisplay = winsNumColor + stats.wins + EnumChatFormatting.RESET.toString();
                    winsColor = stats.wins == 0 ? 0x808080 : 0xFFD700;
                }
                int winsWidth = fontRenderer.getStringWidth(winsDisplay);
                if (winsWidth > (adjustedColumnOffsets[4] - adjustedColumnOffsets[3])) {
                    offsetIncrease = winsWidth - (adjustedColumnOffsets[4] - adjustedColumnOffsets[3]) + 15;
                    adjustedColumnOffsets[4] += offsetIncrease;
                }
                int totalRowWidth = adjustedColumnOffsets[4] + 50;
                if (hasKills) {
                    String killsDisplay = stats.currentGameKills > 0 ? EnumChatFormatting.DARK_RED.toString() + String.valueOf(stats.currentGameKills) + EnumChatFormatting.RESET.toString() : "";
                    int killsWidth = fontRenderer.getStringWidth(killsDisplay);
                    if (killsWidth > 0) {
                        totalRowWidth = killColumnOffset + killsWidth + 15;
                    }
                }
                if (totalRowWidth > maxWidth) {
                    maxWidth = totalRowWidth;
                }
            }
            int headerY = y + rowHeight;
            int dataStartY = headerY + rowHeight;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.4f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(-5, y - 5);
            GL11.glVertex2f(-5, y + backgroundHeight + 5);
            GL11.glVertex2f(maxWidth + 5, y + backgroundHeight + 5);
            GL11.glVertex2f(maxWidth + 5, y - 5);
            GL11.glEnd();
            int rowY = dataStartY;
            for (int i = startIdx; i < endIdx; i++) {
                float rowTop = rowY - 1;
                float rowBottom = rowY + rowHeight - 1;
                if ((i - startIdx) % 2 == 0) {
                    GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.4f);
                } else {
                    GL11.glColor4f(0.3f, 0.3f, 0.3f, 0.4f);
                }
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(-5, rowTop);
                GL11.glVertex2f(-5, rowBottom);
                GL11.glVertex2f(maxWidth + 5, rowBottom);
                GL11.glVertex2f(maxWidth + 5, rowTop);
                GL11.glEnd();
                rowY += rowHeight;
            }
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            fontRenderer.drawStringWithShadow(titleText, 0, y, 0xFFFFFF);
            for (int i = 0; i < columnHeaders.length; i++) {
                fontRenderer.drawStringWithShadow(columnHeaders[i], adjustedColumnOffsets[i], headerY, 0xFFFFFF);
            }
            if (hasKills) {
                fontRenderer.drawStringWithShadow(ConfigManager.translate("overlay.header.kills"), killColumnOffset, headerY, 0xFFFFFF);
            }
            rowY = dataStartY;
            for (int i = startIdx; i < endIdx; i++) {
                Map.Entry<String, PlayerStats> entry = cachedSortedPlayers.get(i);
                String playerName = entry.getKey();
                PlayerStats stats = entry.getValue();
                EnumChatFormatting nameColor = getPlayerColor(playerName);
                String tag = getPlayerTag(playerName, stats, false);
                fontRenderer.drawStringWithShadow(tag, adjustedColumnOffsets[0], rowY, 0xFFFFFF);
                String starsDisplay = "";
                int starsColor = 0xFFFFFF;
                if (stats.isQuerying) {
                    starsDisplay = "";
                } else if (stats.isNick) {
                    starsDisplay = EnumChatFormatting.DARK_GRAY.toString() + "-";
                    starsColor = 0x555555;
                } else if (!stats.hasApiError) {
                    String starsNumColor = stats.stars == 0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.YELLOW.toString();
                    starsDisplay = starsNumColor + stats.stars + EnumChatFormatting.YELLOW.toString() + "✰" + EnumChatFormatting.RESET.toString();
                    starsColor = stats.stars == 0 ? 0x808080 : 0xFFFF00;
                }
                fontRenderer.drawStringWithShadow(starsDisplay, adjustedColumnOffsets[1], rowY, starsColor);
                String nameDisplay = nameColor + playerName + EnumChatFormatting.RESET.toString();
                fontRenderer.drawStringWithShadow(nameDisplay, adjustedColumnOffsets[2], rowY, 0xFFFFFF);
                String kdrDisplay = "";
                int kdrColor = 0xFFFFFF;
                if (stats.isQuerying) {
                    kdrDisplay = "";
                } else if (stats.isNick) {
                    kdrDisplay = EnumChatFormatting.DARK_GRAY.toString() + "-";
                    kdrColor = 0x555555;
                } else if (!stats.hasApiError) {
                    String kdrNumColor = stats.kdr == 0.0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.RED.toString();
                    kdrDisplay = kdrNumColor + df.format(stats.kdr) + EnumChatFormatting.RESET.toString();
                    kdrColor = stats.kdr == 0.0 ? 0x808080 : 0xFF0000;
                }
                fontRenderer.drawStringWithShadow(kdrDisplay, adjustedColumnOffsets[3], rowY, kdrColor);
                String winsDisplay = "";
                int winsColor = 0xFFFFFF;
                if (stats.isQuerying) {
                    winsDisplay = "";
                } else if (stats.isNick) {
                    winsDisplay = EnumChatFormatting.DARK_GRAY.toString() + "-";
                    winsColor = 0x555555;
                } else if (!stats.hasApiError) {
                    String winsNumColor = stats.wins == 0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.GOLD.toString();
                    winsDisplay = winsNumColor + stats.wins + EnumChatFormatting.RESET.toString();
                    winsColor = stats.wins == 0 ? 0x808080 : 0xFFD700;
                }
                fontRenderer.drawStringWithShadow(winsDisplay, adjustedColumnOffsets[4], rowY, winsColor);
                if (hasKills && stats.currentGameKills > 0) {
                    String killsDisplay = EnumChatFormatting.DARK_RED.toString() + String.valueOf(stats.currentGameKills) + EnumChatFormatting.RESET.toString();
                    fontRenderer.drawStringWithShadow(killsDisplay, killColumnOffset, rowY, 0xFFFFFF);
                }
                rowY += rowHeight;
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    private String getPlayerTag(String playerName, PlayerStats stats, boolean isNametag) {
        if (isNametag) {
            if (stats.isNick) {
                return EnumChatFormatting.DARK_PURPLE.toString() + ConfigManager.translate("tag.nick");
            } else if (stats.isQuerying) {
                return EnumChatFormatting.GRAY.toString() + ConfigManager.translate("tag.querying");
            }
            return "";
        }
        List<String> tags = new ArrayList<>();
        if (playerName.equals(mc.thePlayer.getName())) {
            tags.add(EnumChatFormatting.BLUE.toString() + ConfigManager.translate("tag.user"));
        }
        if (!stats.isNick && !stats.isQuerying && !stats.hasApiError && stats.kdr > 10) {
            tags.add(EnumChatFormatting.RED.toString() + ConfigManager.translate("tag.bhop"));
        }
        if (isGameStarted() && stats.isNearbyCached) {
            tags.add(EnumChatFormatting.YELLOW.toString() + ConfigManager.translate("tag.nearby"));
        }
        if (stats.hasApiError) {
            tags.add(EnumChatFormatting.DARK_RED.toString() + ConfigManager.translate("tag.api_error"));
        } else if (stats.isNick) {
            return EnumChatFormatting.DARK_PURPLE.toString() + ConfigManager.translate("tag.nick");
        } else if (stats.isQuerying) {
            return EnumChatFormatting.GRAY.toString() + ConfigManager.translate("tag.querying");
        } else if (stats.stars <= 1 && stats.wins == 0 && stats.kdr < 0.5) {
            tags.add(EnumChatFormatting.DARK_GRAY.toString() + ConfigManager.translate("tag.noob"));
        }
        if (tags.size() > 1) {
            StringBuilder shortTags = new StringBuilder();
            for (int i = 0; i < tags.size(); i++) {
                String tag = tags.get(i);
                if (tag.contains(ConfigManager.translate("tag.user"))) {
                    shortTags.append(EnumChatFormatting.BLUE.toString()).append("[U]");
                } else if (tag.contains(ConfigManager.translate("tag.bhop"))) {
                    shortTags.append(EnumChatFormatting.RED.toString()).append("[B]");
                } else if (tag.contains(ConfigManager.translate("tag.nearby"))) {
                    shortTags.append(EnumChatFormatting.YELLOW.toString()).append("[N]");
                } else if (tag.contains(ConfigManager.translate("tag.noob"))) {
                    shortTags.append(EnumChatFormatting.DARK_GRAY.toString()).append("[N]");
                } else if (tag.contains(ConfigManager.translate("tag.api_error"))) {
                    shortTags.append(EnumChatFormatting.DARK_RED.toString()).append("[E]");
                }
                if (i < tags.size() - 1) {
                    shortTags.append(" ");
                }
            }
            return shortTags.toString();
        } else if (tags.size() == 1) {
            return tags.get(0);
        }
        return "";
    }

    private EnumChatFormatting getPlayerColor(String playerName) {
        if (mc.theWorld == null || mc.getNetHandler() == null) return EnumChatFormatting.WHITE;
        net.minecraft.client.network.NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(playerName);
        if (playerInfo == null || playerInfo.getPlayerTeam() == null) {
            return EnumChatFormatting.WHITE;
        }
        String prefix = playerInfo.getPlayerTeam().getColorPrefix();
        if (prefix == null) {
            return EnumChatFormatting.WHITE;
        }
        for (EnumChatFormatting color : EnumChatFormatting.values()) {
            if (color.toString().equals(prefix) && color.isColor()) {
                return color;
            }
        }
        return EnumChatFormatting.WHITE;
    }

    private boolean isGameStarted() {
        if (mc.thePlayer != null) {
            float maxHealth = mc.thePlayer.getMaxHealth();
            return maxHealth == 30.0f || maxHealth == 40.0f || maxHealth == 60.0f;
        }
        return false;
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int page) { this.currentPage = page; }
    public boolean getWasRightClickPressed() { return wasRightClickPressed; }
    public void setWasRightClickPressed(boolean value) { this.wasRightClickPressed = value; }
    public List<Map.Entry<String, PlayerStats>> getCachedSortedPlayers() { return cachedSortedPlayers; }
}