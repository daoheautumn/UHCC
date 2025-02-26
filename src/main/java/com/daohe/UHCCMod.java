package com.daohe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.minecraft.util.EnumChatFormatting;

@Mod(modid = "uhccmod", name = "UHC Checker", version = "1.0", clientSideOnly = true)
public class UHCCMod {
    public static String apiKey = "";
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Configuration config;
    private static boolean debugMode = false;
    private static boolean isDetecting = false;
    private static boolean isStopped = false;
    private static boolean showTabStats;
    private static boolean showOverlayStats;
    private static boolean showNametagStats;
    private static int overlayMaxPlayers = 25;
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);
    private static final ConcurrentHashMap<String, PlayerStats> playerStatsMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Future<?>> playerQueryTasks = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static List<Map.Entry<String, PlayerStats>> cachedSortedPlayers = new ArrayList<>();
    private static boolean needsResort = true;

    private static float overlayX = 155.0f;
    private static float overlayY = 15.0f;
    private static float overlayScale = 0.8f;
    private static float nametagHeight = 1.3f;
    private static long lastUpdateTime = 0;
    private static long apiKeySetTime = 0;

    private static final DecimalFormat df = new DecimalFormat("#.##");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private static KeyBinding overlayKey;

    private static final List<String> ARTIFACTS_TO_CHECK = Arrays.asList(
            "artemis_bow", "flask_of_ichor", "exodus", "hide_of_leviathan", "tablets_of_destiny",
            "axe_of_perun", "excalibur", "anduril", "deaths_scythe", "chest_of_fate",
            "cornucopia", "essence_of_yggdrasil", "voidbox", "deus_ex_machina", "dice_of_god",
            "kings_rod", "daredevil", "flask_of_cleansing", "shoes_of_vidar", "potion_of_vitality",
            "miners_blessing", "ambrosia", "bloodlust", "modular_bow", "expert_seal",
            "hermes_boots", "barbarian_chestplate"
    );
    private static final List<String> ARTIFACT_TRANSLATIONS = Arrays.asList(
            "自瞄弓", "瞬三药", "永生帽", "潮汐裤", "命运之书",
            "雷斧", "王剑", "安德鲁", "镰刀", "命运之箱",
            "丰饶之角", "世界树精华", "虚空箱", "无敌药", "上帝之骰",
            "王竿", "骷髅马", "肃清", "水鞋", "活力药",
            "矿神的祝福", "密酒", "杀人剑", "变幻弓", "大师卷轴",
            "小飞鞋", "力量甲"
    );

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        apiKey = config.getString("apiKey", Configuration.CATEGORY_GENERAL, "", "Hypixel API 密钥");
        String apiKeyTimeStr = config.getString("apiKeySetTime", Configuration.CATEGORY_GENERAL, "", "API Key 设置时间 (格式: yyyy-MM-dd HH:mm)");
        if (!apiKeyTimeStr.isEmpty()) {
            try {
                apiKeySetTime = sdf.parse(apiKeyTimeStr).getTime();
            } catch (Exception e) {
                apiKeySetTime = 0;
            }
        }
        overlayX = Float.parseFloat(df.format(config.getFloat("overlayX", Configuration.CATEGORY_GENERAL, 155.0f, -1000.0f, 1000.0f, "统计覆盖层的X位置")));
        overlayY = Float.parseFloat(df.format(config.getFloat("overlayY", Configuration.CATEGORY_GENERAL, 15.0f, -1000.0f, 1000.0f, "统计覆盖层的Y位置")));
        overlayScale = Float.parseFloat(df.format(config.getFloat("overlayScale", Configuration.CATEGORY_GENERAL, 0.8f, 0.01f, 1.0f, "统计覆盖层的缩放比例")));
        nametagHeight = Float.parseFloat(df.format(config.getFloat("nametagHeight", Configuration.CATEGORY_GENERAL, 1.3f, -10.0f, 10.0f, "头顶统计的高度偏移")));
        showTabStats = config.getBoolean("showTabStats", Configuration.CATEGORY_GENERAL, true, "在Tab列表中显示统计数据");
        showOverlayStats = config.getBoolean("showOverlayStats", Configuration.CATEGORY_GENERAL, true, "按键按下或聊天框打开时显示统计覆盖层");
        showNametagStats = config.getBoolean("showNametagStats", Configuration.CATEGORY_GENERAL, true, "在玩家头顶显示统计数据");
        overlayMaxPlayers = config.getInt("overlayMaxPlayers", Configuration.CATEGORY_GENERAL, 25, 1, 1000, "覆盖层每列最大玩家数");
        config.save();

        overlayKey = new KeyBinding("key.overlay", Keyboard.KEY_GRAVE, "key.categories.uhcchecker");
        ClientRegistry.registerKeyBinding(overlayKey);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new UCCommand());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        executor.shutdownNow();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (isStopped || event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !isDetecting) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= 1000) {
            updatePlayerListFromTab();
            lastUpdateTime = currentTime;

            if (!apiKey.isEmpty() && (currentTime - apiKeySetTime) > 24 * 60 * 60 * 1000) {
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "API Key 已过期！请前往 developer.hypixel.net 申请新密钥并使用 /uc setapi 更新"));
                isStopped = true;
                isDetecting = false;
                clearStatsAndResetTab();
                cancelAllQueries();
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (isStopped || mc.thePlayer == null || mc.theWorld == null || apiKey.isEmpty()) return;

        String message = event.message.getUnformattedText();
        if (message.contains(" has joined ") && message.contains("/") && isDetecting) {
            String playerName = message.split(" has joined ")[0].trim();
            if (debugMode) {
                System.out.println(playerName + " joined");
            }
            playerStatsMap.put(playerName, new PlayerStats(false, true));
            submitPlayerQuery(playerName);
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (isStopped || apiKey.isEmpty()) return;

        if (event.type == RenderGameOverlayEvent.ElementType.ALL && showOverlayStats) {
            if (overlayKey.isKeyDown() || mc.currentScreen instanceof GuiChat) {
                renderPlayerStatsOverlay();
            }
        }
        if (event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST && showTabStats) {
            renderTabListStats();
        }
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Specials.Pre event) {
        if (!showNametagStats || isStopped || !isDetecting || apiKey.isEmpty() || !(event.entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.entity;
        String playerName = player.getName();
        PlayerStats stats = playerStatsMap.get(playerName);

        if (stats != null) {
            FontRenderer fontRenderer = mc.fontRendererObj;
            String statText;
            if (stats.isQuerying) {
                statText = EnumChatFormatting.GRAY + "[查询中]" + EnumChatFormatting.RESET;
            } else if (stats.isNick) {
                statText = EnumChatFormatting.DARK_PURPLE + "[nick]" + EnumChatFormatting.RESET;
            } else if (stats.hasApiError) {
                return;
            } else {
                statText = EnumChatFormatting.LIGHT_PURPLE + "[" + stats.stars + "✰]" + EnumChatFormatting.RESET + "  " +
                        EnumChatFormatting.GOLD + "kdr:" + String.format("%.2f", stats.kdr) + "  " + stats.wins + "w" + EnumChatFormatting.RESET;
            }
            float scale = 0.02666667F;
            double x = event.x;
            double y = event.y + player.height + nametagHeight;
            double z = event.z;

            GL11.glPushMatrix();
            GL11.glTranslatef((float) x, (float) y, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-scale, -scale, scale);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            int width = fontRenderer.getStringWidth(statText.replaceAll("§[0-9a-fk-or]", "")) / 2;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.25F);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(-width - 1, -1);
            GL11.glVertex2f(-width - 1, 8);
            GL11.glVertex2f(width + 1, 8);
            GL11.glVertex2f(width + 1, -1);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            fontRenderer.drawString(statText, -width, 0, 0xFFFFFF);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }
    }

    private static void updatePlayerListFromTab() {
        if (mc.getNetHandler() == null || mc.getNetHandler().getPlayerInfoMap() == null || apiKey.isEmpty()) return;

        Set<String> currentPlayers = new HashSet<>();
        for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
            String playerName = playerInfo.getGameProfile().getName();
            currentPlayers.add(playerName);
            if (!playerStatsMap.containsKey(playerName)) {
                if (debugMode) {
                    System.out.println(playerName + " joined");
                }
                playerStatsMap.put(playerName, new PlayerStats(false, true));
                submitPlayerQuery(playerName);
            }
        }

        playerStatsMap.entrySet().removeIf(entry -> {
            String playerName = entry.getKey();
            boolean shouldRemove = !currentPlayers.contains(playerName);
            if (shouldRemove) {
                if (debugMode) {
                    System.out.println(playerName + " quit");
                }
                cancelPlayerQuery(playerName);
            }
            return shouldRemove;
        });
        needsResort = true;
    }

    private static void submitPlayerQuery(String playerName) {
        Future<?> task = executor.submit(() -> getPlayerStats(playerName));
        playerQueryTasks.put(playerName, task);
    }

    private static void cancelPlayerQuery(String playerName) {
        Future<?> task = playerQueryTasks.remove(playerName);
        if (task != null && !task.isDone()) {
            task.cancel(true);
        }
    }

    private static void cancelAllQueries() {
        for (Map.Entry<String, Future<?>> entry : playerQueryTasks.entrySet()) {
            Future<?> task = entry.getValue();
            if (!task.isDone()) {
                task.cancel(true);
            }
        }
        playerQueryTasks.clear();
        if (debugMode) {
            System.out.println("All queries stopped");
        }
    }

    private void renderPlayerStatsOverlay() {
        GL11.glPushMatrix();
        GL11.glTranslatef(overlayX, overlayY, 0.0f);
        GL11.glScalef(overlayScale, overlayScale, 1.0f);

        if (needsResort) {
            cachedSortedPlayers = new ArrayList<>(playerStatsMap.entrySet());
            cachedSortedPlayers.sort(Comparator
                    .comparing(Map.Entry<String, PlayerStats>::getValue, Comparator
                            .comparing(PlayerStats::isNick).reversed()
                            .thenComparing(PlayerStats::getKdr, Comparator.reverseOrder())
                            .thenComparing(PlayerStats::getStars, Comparator.reverseOrder())));
            needsResort = false;
        }

        String localPlayerName = mc.thePlayer.getName();
        int totalPlayers = cachedSortedPlayers.size();
        int columnWidth = 250;
        int rowHeight = 10;
        int y = 0;

        mc.fontRendererObj.drawStringWithShadow("总玩家数: " + totalPlayers, 0, y, 0xFFFFFF);
        y += rowHeight + 5;

        int columns = (int) Math.ceil((double) totalPlayers / overlayMaxPlayers);
        for (int col = 0; col < columns; col++) {
            int startIdx = col * overlayMaxPlayers;
            int endIdx = Math.min(startIdx + overlayMaxPlayers, totalPlayers);

            for (int i = startIdx; i < endIdx; i++) {
                Map.Entry<String, PlayerStats> entry = cachedSortedPlayers.get(i);
                String playerName = entry.getKey();
                PlayerStats stats = entry.getValue();
                EnumChatFormatting nameColor = getPlayerColor(playerName);
                String namePrefix = playerName.equals(localPlayerName) ? EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD : nameColor.toString();
                String prefix = stats.hasApiError ? EnumChatFormatting.DARK_RED + "[API_ERR] " : stats.isNick ? EnumChatFormatting.DARK_PURPLE + "[nick] " : stats.isQuerying ? EnumChatFormatting.GRAY + "[查询中] " : "";
                String display;

                if (stats.isNick || stats.isQuerying || stats.hasApiError) {
                    display = prefix + namePrefix + playerName + EnumChatFormatting.RESET;
                } else {
                    String starsNumColor = stats.stars == 0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.YELLOW.toString();
                    String kdrNumColor = stats.kdr == 0.0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.RED.toString();
                    String winsNumColor = stats.wins == 0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.GOLD.toString();

                    display = String.format("%s%-16s" + EnumChatFormatting.RESET + " | " + EnumChatFormatting.YELLOW + "%s%2d" + EnumChatFormatting.YELLOW + "✰" + EnumChatFormatting.RESET + " | " + EnumChatFormatting.RED + "KDR:" + "%s%-6.2f" + EnumChatFormatting.RESET + " | " + EnumChatFormatting.GOLD + "W:" + "%s%-3d" + EnumChatFormatting.RESET,
                            prefix + namePrefix, playerName, starsNumColor, stats.stars, kdrNumColor, stats.kdr, winsNumColor, stats.wins);
                }
                mc.fontRendererObj.drawStringWithShadow(display, col * columnWidth, y, 0xFFFFFF);
                y += rowHeight;
            }
            y = rowHeight + 5;
        }

        GL11.glPopMatrix();
    }

    private void renderTabListStats() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null || apiKey.isEmpty()) return;

        for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
            String playerName = playerInfo.getGameProfile().getName();
            PlayerStats stats = playerStatsMap.get(playerName);
            if (stats != null) {
                EnumChatFormatting nameColor = getPlayerColor(playerName);
                String prefix = stats.hasApiError ? EnumChatFormatting.DARK_RED + "[API_ERR] " : stats.isNick ? EnumChatFormatting.DARK_PURPLE + "[nick] " : stats.isQuerying ? EnumChatFormatting.GRAY + "[查询中] " : "";
                String displayName;

                if (stats.isNick || stats.isQuerying || stats.hasApiError) {
                    displayName = prefix + nameColor + playerName + EnumChatFormatting.RESET;
                } else {
                    String starsNumColor = stats.stars == 0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.YELLOW.toString();
                    String kdrNumColor = stats.kdr == 0.0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.RED.toString();
                    String winsNumColor = stats.wins == 0 ? EnumChatFormatting.GRAY.toString() : EnumChatFormatting.GOLD.toString();

                    displayName = prefix + nameColor + playerName + EnumChatFormatting.RESET + " - " +
                            EnumChatFormatting.YELLOW + starsNumColor + stats.stars + EnumChatFormatting.YELLOW + "✰" + EnumChatFormatting.RESET + " " +
                            EnumChatFormatting.RED + "KDR:" + kdrNumColor + String.format("%.2f", stats.kdr) + EnumChatFormatting.RESET + " " +
                            EnumChatFormatting.GOLD + "W:" + winsNumColor + stats.wins + EnumChatFormatting.RESET;
                }
                playerInfo.setDisplayName(new ChatComponentText(displayName));
            } else {
                playerInfo.setDisplayName(null);
            }
        }
    }

    private EnumChatFormatting getPlayerColor(String playerName) {
        if (mc.theWorld == null || mc.getNetHandler() == null) return EnumChatFormatting.WHITE;
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(playerName);
        if (playerInfo != null && playerInfo.getPlayerTeam() != null) {
            String prefix = playerInfo.getPlayerTeam().getColorPrefix();
            for (EnumChatFormatting color : EnumChatFormatting.values()) {
                if (color.toString().equals(prefix) && color.isColor()) {
                    return color;
                }
            }
        }
        return EnumChatFormatting.WHITE;
    }

    private static void clearStatsAndResetTab() {
        playerStatsMap.clear();
        needsResort = true;
        cachedSortedPlayers.clear();
        resetTabDisplay();
    }

    private static void resetTabDisplay() {
        if (mc.theWorld != null && mc.getNetHandler() != null) {
            for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                playerInfo.setDisplayName(null);
            }
        }
    }

    private static boolean isApiKeyExpired() {
        if (apiKey.isEmpty() || apiKeySetTime == 0) return false;
        long currentTime = System.currentTimeMillis();
        return (currentTime - apiKeySetTime) > 24 * 60 * 60 * 1000;
    }

    public static class UCCommand extends CommandBase {
        @Override
        public String getCommandName() {
            return "uc";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/uc <start|stop|setapi|debug|help|c|size|xpos|ypos|resetpos|clear|toggle <tab/overlay/nametag>|overlaymaxid <number>|nametagheight <number>>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            if (mc.theWorld == null || !mc.theWorld.isRemote) return;

            try {
                if (args.length == 0) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "指令错误！请输入 /uc help 获取帮助"));
                    return;
                }

                String command = args[0].toLowerCase();

                if (command.equals("start") && args.length == 1) {
                    if (apiKey.isEmpty()) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "请先使用 /uc setapi <API_KEY> 设置 Hypixel API Key"));
                        return;
                    }
                    if (isApiKeyExpired()) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "API Key 已过期！请前往 developer.hypixel.net 申请新密钥并使用 /uc setapi 更新"));
                        return;
                    }
                    if (isStopped) {
                        isStopped = false;
                        isDetecting = true;
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "已重新启用 Mod 并开始检测 Tab 列表。"));
                        updatePlayerListFromTab();
                    } else if (!isDetecting) {
                        isDetecting = true;
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "已开始检测 Tab 列表以获取玩家统计数据。"));
                        updatePlayerListFromTab();
                    } else {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Mod 已处于检测状态，使用 /uc stop 停止。"));
                    }
                } else if (command.equals("stop") && args.length == 1) {
                    isStopped = true;
                    isDetecting = false;
                    clearStatsAndResetTab();
                    cancelAllQueries();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "已停止所有 Mod 动作并清空列表，使用 /uc start 重新启用。"));
                } else if (command.equals("debug") && args.length == 1) {
                    debugMode = !debugMode;
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "调试模式已" + (debugMode ? "启用" : "禁用") + "！"));
                    if (debugMode) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "使用 /uc start 启用检测。"));
                    }
                } else if (command.equals("help") && args.length == 1) {
                    showHelpPanel(sender);
                } else if (command.equals("setapi") && args.length == 2) {
                    apiKey = args[1];
                    apiKeySetTime = System.currentTimeMillis();
                    config.get(Configuration.CATEGORY_GENERAL, "apiKey", "").set(apiKey);
                    config.get(Configuration.CATEGORY_GENERAL, "apiKeySetTime", "").set(sdf.format(new Date(apiKeySetTime)));
                    config.save();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "API Key 已成功设置并保存！"));
                    if (debugMode) {
                        System.out.println("API Key set at " + sdf.format(new Date(apiKeySetTime)));
                    }
                } else if (command.equals("c") && args.length == 2) {
                    String targetPlayer = args[1];
                    if (apiKey.isEmpty()) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "请先使用 /uc setapi <API_KEY> 设置 Hypixel API Key"));
                        return;
                    }
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "正在查询 " + targetPlayer + " 的 UHC 统计数据..."));
                    executor.submit(() -> {
                        PlayerStats stats = getPlayerStats(targetPlayer);
                        if (stats != null) {
                            if (stats.isNick) {
                                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "[nick] " + targetPlayer));
                            } else if (stats.hasApiError) {
                                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "查询 " + targetPlayer + " 失败"));
                            } else {
                                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + targetPlayer + " - " + EnumChatFormatting.YELLOW + stats.stars + "✰"));
                                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "击杀: " + stats.kills + " | 死亡: " + stats.deaths + " | KDR: " + EnumChatFormatting.RED + String.format("%.2f", stats.kdr)));
                                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "胜利: " + stats.wins + " | 分数: " + stats.score));
                                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "装备套件: " + stats.equippedKit));
                                if (!stats.artifacts.isEmpty()) {
                                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "额外神器: " + String.join("，", stats.artifacts)));
                                }
                            }
                        }
                    });
                } else if (command.equals("size") && args.length == 2) {
                    try {
                        float newScale = Float.parseFloat(args[1]);
                        if (newScale >= 0.01f && newScale <= 1.0f) {
                            overlayScale = Float.parseFloat(df.format(newScale));
                            config.get(Configuration.CATEGORY_GENERAL, "overlayScale", 0.8f).set(overlayScale);
                            config.save();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "统计覆盖层缩放已设置为 " + overlayScale));
                        } else {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "缩放值必须在 0.01 到 1.0 之间"));
                        }
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无效的缩放值: " + args[1]));
                    }
                } else if (command.equals("xpos") && args.length == 2) {
                    try {
                        float newX = Float.parseFloat(args[1]);
                        if (newX >= -1000.0f && newX <= 1000.0f) {
                            overlayX = Float.parseFloat(df.format(newX));
                            config.get(Configuration.CATEGORY_GENERAL, "overlayX", 155.0f).set(overlayX);
                            config.save();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "覆盖层 X 位置已设置为 " + overlayX));
                        } else {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "X 位置值必须在 -1000 到 1000 之间"));
                        }
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无效的 X 位置值: " + args[1]));
                    }
                } else if (command.equals("ypos") && args.length == 2) {
                    try {
                        float newY = Float.parseFloat(args[1]);
                        if (newY >= -1000.0f && newY <= 1000.0f) {
                            overlayY = Float.parseFloat(df.format(newY));
                            config.get(Configuration.CATEGORY_GENERAL, "overlayY", 15.0f).set(overlayY);
                            config.save();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "覆盖层 Y 位置已设置为 " + overlayY));
                        } else {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Y 位置值必须在 -1000 到 1000 之间"));
                        }
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无效的 Y 位置值: " + args[1]));
                    }
                } else if (command.equals("resetpos") && args.length == 1) {
                    overlayX = 155.0f;
                    overlayY = 15.0f;
                    overlayScale = 0.8f;
                    config.get(Configuration.CATEGORY_GENERAL, "overlayX", 155.0f).set(overlayX);
                    config.get(Configuration.CATEGORY_GENERAL, "overlayY", 15.0f).set(overlayY);
                    config.get(Configuration.CATEGORY_GENERAL, "overlayScale", 0.8f).set(overlayScale);
                    config.save();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "统计覆盖层位置和缩放已重置为默认值"));
                } else if (command.equals("clear") && args.length == 1) {
                    playerStatsMap.clear();
                    needsResort = true;
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "已清除所有玩家统计数据"));
                } else if (command.equals("toggle") && args.length == 2) {
                    if (args[1].equalsIgnoreCase("tab")) {
                        showTabStats = !showTabStats;
                        if (!showTabStats) {
                            resetTabDisplay();
                        }
                        config.get(Configuration.CATEGORY_GENERAL, "showTabStats", true).set(showTabStats);
                        config.save();
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Tab 统计显示已" + (showTabStats ? "开启" : "关闭")));
                    } else if (args[1].equalsIgnoreCase("overlay")) {
                        showOverlayStats = !showOverlayStats;
                        config.get(Configuration.CATEGORY_GENERAL, "showOverlayStats", true).set(showOverlayStats);
                        config.save();
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "覆盖层统计显示已" + (showOverlayStats ? "开启" : "关闭")));
                    } else if (args[1].equalsIgnoreCase("nametag")) {
                        showNametagStats = !showNametagStats;
                        config.get(Configuration.CATEGORY_GENERAL, "showNametagStats", true).set(showNametagStats);
                        config.save();
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "头顶统计显示已" + (showNametagStats ? "开启" : "关闭")));
                    } else {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "用法: /uc toggle <tab/overlay/nametag>"));
                    }
                } else if (command.equals("overlaymaxid") && args.length == 2) {
                    try {
                        int maxPlayers = Integer.parseInt(args[1]);
                        if (maxPlayers >= 1 && maxPlayers <= 1000) {
                            overlayMaxPlayers = maxPlayers;
                            config.get(Configuration.CATEGORY_GENERAL, "overlayMaxPlayers", 25).set(overlayMaxPlayers);
                            config.save();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "覆盖层每列最大玩家数已设置为 " + maxPlayers));
                        } else {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "值必须在 1 到 1000 之间"));
                        }
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无效的数字: " + args[1]));
                    }
                } else if (command.equals("nametagheight") && args.length == 2) {
                    try {
                        float newHeight = Float.parseFloat(args[1]);
                        if (newHeight >= -10.0f && newHeight <= 10.0f) {
                            nametagHeight = Float.parseFloat(df.format(newHeight));
                            config.get(Configuration.CATEGORY_GENERAL, "nametagHeight", 1.3f).set(nametagHeight);
                            config.save();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "头顶统计高度已设置为 " + nametagHeight));
                        } else {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "高度值必须在 -10 到 10 之间"));
                        }
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无效的高度值: " + args[1]));
                    }
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "指令错误！请输入 /uc help 获取帮助"));
                }
            } catch (Exception e) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "命令执行失败: " + e.getMessage()));
                if (debugMode) {
                    System.out.println("Command error: " + e.getMessage());
                }
            }
        }

        @Override
        public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
            if (args.length == 1) {
                return CommandBase.getListOfStringsMatchingLastWord(args, "start", "stop", "setapi", "debug", "help", "c", "size", "xpos", "ypos", "resetpos", "clear", "toggle", "overlaymaxid", "nametagheight");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
                return CommandBase.getListOfStringsMatchingLastWord(args, "tab", "overlay", "nametag");
            }
            return null;
        }

        private void showHelpPanel(ICommandSender sender) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "--- UHC Checker 帮助 ---"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc start - 开始检测 Tab 列表以获取玩家统计数据"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc stop - 停止所有 Mod 动作并清空列表"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc setapi <API_KEY> - 设置 Hypixel API Key"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc debug - 切换调试模式"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc help - 显示此帮助面板"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc c <id> - 查询指定玩家的 UHC 统计数据"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc size <0.01-1> - 设置覆盖层缩放"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc xpos <number> - 设置覆盖层 X 位置"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc ypos <number> - 设置覆盖层 Y 位置"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc resetpos - 重置覆盖层位置和缩放"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc clear - 清除所有玩家统计数据"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc toggle <tab/overlay/nametag> - 切换 Tab/覆盖层/头顶统计的显示"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc overlaymaxid <number> - 设置覆盖层每列最大玩家数"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/uc nametagheight <number> - 设置头顶统计高度"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "------------------------"));
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }

    private static PlayerStats getPlayerStats(String playerName) {
        if (isStopped || mc.thePlayer == null || mc.theWorld == null) {
            if (debugMode) {
                System.out.println("Mod stopped for " + playerName);
            }
            return null;
        }
        if (apiKey.isEmpty()) {
            return null;
        }
        if (!playerName.matches("^[a-zA-Z0-9_]{3,16}$")) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无效的玩家名: " + playerName));
            return null;
        }
        if (debugMode) {
            System.out.println("Querying " + playerName);
        }

        try {
            String uuid = null;
            JsonObject mojangResponse = null;
            boolean mojangError = false;
            for (int retry = 0; retry < 6; retry++) {
                if (Thread.currentThread().isInterrupted()) {
                    if (debugMode) {
                        System.out.println(playerName + " query interrupted");
                    }
                    return null;
                }
                if (retry > 0) {
                    int sleepTime = 8000 + random.nextInt(5000);
                    if (debugMode) {
                        System.out.println("Waiting " + (sleepTime / 1000.0) + "s for retry " + retry);
                    }
                    Thread.sleep(sleepTime);
                }
                mojangResponse = getMojangResponse(playerName);
                if (mojangResponse != null) {
                    if (mojangResponse.has("id")) {
                        uuid = mojangResponse.get("id").getAsString();
                        break;
                    } else if (mojangResponse.has("errorMessage")) {
                        String errorMessage = mojangResponse.get("errorMessage").getAsString().toLowerCase();
                        if (errorMessage.contains("couldn't find any profile")) {
                            if (debugMode) {
                                System.out.println(playerName + " not found, marked as nick");
                            }
                            PlayerStats nickStats = new PlayerStats(true);
                            playerStatsMap.put(playerName, nickStats);
                            needsResort = true;
                            playerQueryTasks.remove(playerName);
                            return nickStats;
                        }
                    }
                }
                mojangError = true;
            }

            if (uuid == null) {
                if (debugMode) {
                    System.out.println(playerName + " UUID fetch failed");
                }
                PlayerStats nickStats = new PlayerStats(true);
                nickStats.hasApiError = mojangError;
                playerStatsMap.put(playerName, nickStats);
                needsResort = true;
                playerQueryTasks.remove(playerName);
                return nickStats;
            }
            if (debugMode) {
                System.out.println(playerName + " UUID: " + uuid);
            }

            JsonObject hypixelResponse = null;
            boolean hypixelError = false;
            for (int retry = 0; retry < 6; retry++) {
                if (Thread.currentThread().isInterrupted()) {
                    if (debugMode) {
                        System.out.println(playerName + " query interrupted");
                    }
                    return null;
                }
                if (retry > 0) {
                    int sleepTime = 8000 + random.nextInt(5000);
                    if (debugMode) {
                        System.out.println("Waiting " + (sleepTime / 1000.0) + "s for retry " + retry);
                    }
                    Thread.sleep(sleepTime);
                }
                URL url = new URL("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    hypixelResponse = new JsonParser().parse(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                    break;
                } else if (responseCode == 429) {
                    String retryAfter = conn.getHeaderField("Retry-After");
                    int delay = retryAfter != null ? Integer.parseInt(retryAfter) * 1000 : (8000 + random.nextInt(5000));
                    if (debugMode) {
                        System.out.println("Rate limit, waiting " + (delay / 1000.0) + "s");
                    }
                    Thread.sleep(delay);
                } else {
                    if (debugMode) {
                        System.out.println("Hypixel error: " + responseCode);
                    }
                    hypixelError = true;
                }
            }

            if (hypixelResponse == null) {
                if (debugMode) {
                    System.out.println(playerName + " Hypixel fetch failed");
                }
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无法获取 " + playerName + " 的 Hypixel 数据，已尝试 6 次"));
                PlayerStats errorStats = new PlayerStats(false);
                errorStats.hasApiError = true;
                playerStatsMap.put(playerName, errorStats);
                needsResort = true;
                playerQueryTasks.remove(playerName);
                return errorStats;
            }

            if (hypixelResponse.get("success").getAsBoolean() && hypixelResponse.get("player").isJsonNull()) {
                if (debugMode) {
                    System.out.println(playerName + " marked as nick");
                }
                PlayerStats nickStats = new PlayerStats(true);
                playerStatsMap.put(playerName, nickStats);
                needsResort = true;
                playerQueryTasks.remove(playerName);
                return nickStats;
            }

            JsonObject player = hypixelResponse.getAsJsonObject("player");
            if (player == null) {
                if (debugMode) {
                    System.out.println(playerName + " no player data");
                }
                PlayerStats nickStats = new PlayerStats(true);
                playerStatsMap.put(playerName, nickStats);
                needsResort = true;
                playerQueryTasks.remove(playerName);
                return nickStats;
            }
            JsonObject stats = player.getAsJsonObject("stats");
            if (stats == null || !stats.has("UHC")) {
                if (debugMode) {
                    System.out.println(playerName + " no UHC stats");
                }
                PlayerStats statsData = new PlayerStats(0, 0, 0, 0.0, 0, 0, "None", new ArrayList<>());
                playerStatsMap.put(playerName, statsData);
                needsResort = true;
                playerQueryTasks.remove(playerName);
                return statsData;
            }
            if (debugMode) {
                System.out.println(playerName + " parsing stats");
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
                        artifacts.add(ARTIFACT_TRANSLATIONS.get(index));
                    }
                }
            }

            if (debugMode && !artifacts.isEmpty()) {
                System.out.println(playerName + " artifacts: " + String.join(", ", artifacts));
            }
            PlayerStats statsData = new PlayerStats(stars, totalKills, totalDeaths, kdr, totalWins, score, equippedKit, artifacts);
            playerStatsMap.put(playerName, statsData);
            needsResort = true;
            playerQueryTasks.remove(playerName);
            return statsData;
        } catch (Exception e) {
            if (debugMode) {
                System.out.println(playerName + " query error: " + e.getMessage());
            }
            PlayerStats nickStats = new PlayerStats(true);
            playerStatsMap.put(playerName, nickStats);
            needsResort = true;
            playerQueryTasks.remove(playerName);
            return nickStats;
        }
    }

    private static JsonObject getMojangResponse(String playerName) throws Exception {
        if (mc.thePlayer == null || mc.theWorld == null) {
            if (debugMode) {
                System.out.println("Null world/player for " + playerName);
            }
            return null;
        }
        if (debugMode) {
            System.out.println("Fetching UUID for " + playerName);
        }
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();

        if (responseCode == 200) {
            JsonObject response = new JsonParser().parse(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
            if (debugMode) {
                System.out.println(playerName + " UUID fetched");
            }
            return response;
        } else if (responseCode == 429) {
            String retryAfter = conn.getHeaderField("Retry-After");
            int delay = retryAfter != null ? Integer.parseInt(retryAfter) * 1000 : (8000 + random.nextInt(5000));
            if (debugMode) {
                System.out.println("Mojang rate limit, waiting " + (delay / 1000.0) + "s");
            }
            Thread.sleep(delay);
            return null;
        } else if (responseCode == 404) {
            JsonObject errorResponse = new JsonParser().parse(new InputStreamReader(conn.getErrorStream())).getAsJsonObject();
            if (debugMode) {
                System.out.println(playerName + " not found");
            }
            return errorResponse;
        } else {
            if (debugMode) {
                System.out.println("Mojang error: " + responseCode);
            }
            return null;
        }
    }

    private static int calculateUHCStars(int score) {
        int[] thresholds = {0, 10, 60, 210, 460, 960, 1710, 2710, 5210, 10210, 13210, 16210, 19210, 22210, 25210};
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (score >= thresholds[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    private static class PlayerStats {
        int stars;
        int kills;
        int deaths;
        double kdr;
        int wins;
        int score;
        String equippedKit;
        boolean isNick;
        boolean isQuerying;
        boolean hasApiError;
        List<String> artifacts;

        PlayerStats(int stars, int kills, int deaths, double kdr, int wins, int score, String equippedKit, List<String> artifacts) {
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
        }

        PlayerStats(boolean isNick) {
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
        }

        PlayerStats(boolean isNick, boolean isQuerying) {
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
        }

        public int getStars() {
            return stars;
        }

        public double getKdr() {
            return kdr;
        }

        public boolean isNick() {
            return isNick;
        }
    }
}