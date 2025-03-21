package com.daohe;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommandHandler {
    private final UHCCMod mod;
    private final UCCommand command;
    private final PlayerQueryHandler queryHandler;
    private static final DecimalFormat df = new DecimalFormat("#.##");

    public CommandHandler(UHCCMod mod) {
        this.mod = mod;
        this.command = new UCCommand();
        this.queryHandler = new PlayerQueryHandler(mod);
    }

    public CommandBase getCommand() {
        return command;
    }

    class UCCommand extends CommandBase {
        @Override
        public String getCommandName() {
            return "uc";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return ConfigManager.translate("command.uc.usage");
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            if (args.length == 0) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                return;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "start":
                    if (args.length != 1) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    if (UHCCMod.apiKey.isEmpty()) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.setapi.no_key")));
                        return;
                    }
                    if (Utils.isApiKeyExpired()) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.setapi.expired")));
                        return;
                    }
                    if (EventHandler.isStopped) {
                        EventHandler.isStopped = false;
                        UHCCMod.isDetecting = true;
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.start.reenabled")));
                        mod.getEventHandler().updatePlayerListFromTab();
                    } else if (!UHCCMod.isDetecting) {
                        UHCCMod.isDetecting = true;
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.start.detecting")));
                        mod.getEventHandler().updatePlayerListFromTab();
                    } else {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.start.already")));
                    }
                    break;

                case "stop":
                    if (args.length != 1) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    EventHandler.isStopped = true;
                    UHCCMod.isDetecting = false;
                    Utils.clearStatsAndResetTab(mod.getStatsManager(), mod.getRenderHandler());
                    mod.getStatsManager().cancelAllQueries();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.stop")));
                    break;

                case "setapi":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    UHCCMod.apiKey = args[1];
                    ConfigManager.apiKeySetTime = System.currentTimeMillis();
                    mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "apiKey", "", "").set(UHCCMod.apiKey);
                    mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "apiKeySetTime", "", "").set(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(ConfigManager.apiKeySetTime)));
                    mod.getConfigManager().getConfig().save();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.setapi.success")));
                    if (UHCCMod.debugMode) {
                        System.out.println("API Key set");
                    }
                    break;

                case "c":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    String targetPlayer = args[1];
                    queryHandler.handlePlayerQuery(sender, targetPlayer, true);
                    break;

                case "debug":
                    if (args.length != 1) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    UHCCMod.debugMode = !UHCCMod.debugMode;
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate(UHCCMod.debugMode ? "command.uc.debug.on" : "command.uc.debug.off")));
                    if (UHCCMod.debugMode) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.debug.hint")));
                    }
                    break;

                case "help":
                    if (args.length != 1) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    showHelpPanel(sender);
                    break;

                case "size":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    try {
                        float scale = Float.parseFloat(args[1]);
                        if (scale < 0.01f || scale > 1.0f) {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.size.invalid")));
                            return;
                        }
                        String formattedScale = df.format(scale);
                        RenderHandler.overlayScale = Float.parseFloat(formattedScale);
                        Property prop = mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "overlayScale", 0.6f, "");
                        prop.set(formattedScale);
                        mod.getConfigManager().getConfig().save();
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.size.set", formattedScale)));
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.size.invalid")));
                    }
                    break;

                case "xpos":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    try {
                        float x = Float.parseFloat(args[1]);
                        if (x < -1000.0f || x > 1000.0f) {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.xpos.invalid")));
                            return;
                        }
                        String formattedX = df.format(x);
                        RenderHandler.overlayX = Float.parseFloat(formattedX);
                        Property prop = mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "overlayX", 90.0f, "");
                        prop.set(formattedX);
                        mod.getConfigManager().getConfig().save();
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.xpos.set", formattedX)));
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.xpos.invalid")));
                    }
                    break;

                case "ypos":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    try {
                        float y = Float.parseFloat(args[1]);
                        if (y < -1000.0f || y > 1000.0f) {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.ypos.invalid")));
                            return;
                        }
                        String formattedY = df.format(y);
                        RenderHandler.overlayY = Float.parseFloat(formattedY);
                        Property prop = mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "overlayY", 10.0f, "");
                        prop.set(formattedY);
                        mod.getConfigManager().getConfig().save();
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.ypos.set", formattedY)));
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.ypos.invalid")));
                    }
                    break;

                case "resetpos":
                    if (args.length != 1) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    String formattedX = df.format(90.0f);
                    String formattedY = df.format(10.0f);
                    RenderHandler.overlayX = Float.parseFloat(formattedX);
                    RenderHandler.overlayY = Float.parseFloat(formattedY);
                    Property propX = mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "overlayX", 90.0f, "");
                    Property propY = mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "overlayY", 10.0f, "");
                    propX.set(formattedX);
                    propY.set(formattedY);
                    mod.getConfigManager().getConfig().save();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.resetpos")));
                    break;

                case "toggle":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    switch (args[1].toLowerCase()) {
                        case "overlay":
                            RenderHandler.showOverlayStats = !RenderHandler.showOverlayStats;
                            mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "showOverlayStats", true, "").set(RenderHandler.showOverlayStats);
                            mod.getConfigManager().getConfig().save();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate(RenderHandler.showOverlayStats ? "command.uc.toggle.overlay.on" : "command.uc.toggle.overlay.off")));
                            break;
                        case "nametag":
                            RenderHandler.showNametagStats = !RenderHandler.showNametagStats;
                            mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "showNametagStats", true, "").set(RenderHandler.showNametagStats);
                            mod.getConfigManager().getConfig().save();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate(RenderHandler.showNametagStats ? "command.uc.toggle.nametag.on" : "command.uc.toggle.nametag.off")));
                            break;
                        default:
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.toggle.usage")));
                            break;
                    }
                    break;

                case "overlaymaxid":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    try {
                        int maxId = Integer.parseInt(args[1]);
                        if (maxId < 1 || maxId > 1000) {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.overlaymaxid.invalid")));
                            return;
                        }
                        RenderHandler.overlayMaxPlayers = maxId;
                        mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "overlayMaxPlayers", 25, "").set(maxId);
                        mod.getConfigManager().getConfig().save();
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.overlaymaxid.set", maxId)));
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.overlaymaxid.invalid")));
                    }
                    break;

                case "nametagheight":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    try {
                        float height = Float.parseFloat(args[1]);
                        if (height < -10.0f || height > 10.0f) {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.nametagheight.invalid")));
                            return;
                        }
                        String formattedHeight = df.format(height);
                        RenderHandler.nametagHeight = Float.parseFloat(formattedHeight);
                        Property prop = mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "nametagHeight", 1.3f, "");
                        prop.set(formattedHeight);
                        mod.getConfigManager().getConfig().save();
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.nametagheight.set", formattedHeight)));
                    } catch (NumberFormatException e) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.nametagheight.invalid")));
                    }
                    break;

                case "lang":
                    if (args.length != 2) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                        return;
                    }
                    String lang = args[1].toLowerCase();
                    if (!lang.equals("en") && !lang.equals("cn")) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.lang.invalid")));
                        return;
                    }
                    ConfigManager.currentLang = lang.equals("en") ? "en_US" : "zh_CN";
                    mod.getConfigManager().getConfig().get(Configuration.CATEGORY_GENERAL, "language", "en_US", "").set(ConfigManager.currentLang);
                    mod.getConfigManager().getConfig().save();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + ConfigManager.translate("command.uc.lang.set", lang.equals("en") ? "English" : "中文")));
                    break;

                default:
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + ConfigManager.translate("command.uc.error")));
                    break;
            }
        }

        private void showHelpPanel(ICommandSender sender) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + ConfigManager.translate("command.uc.help.header")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.start")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.stop")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.setapi")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.c")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.debug")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.size")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.xpos")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.ypos")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.resetpos")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.toggle")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.overlaymaxid")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.nametagheight")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.lang")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + ConfigManager.translate("command.uc.help.help")));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + ConfigManager.translate("command.uc.help.footer")));
        }

        @Override
        public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "start", "stop", "setapi", "c", "debug", "help", "size", "xpos", "ypos", "resetpos", "toggle", "overlaymaxid", "nametagheight", "lang");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
                return getListOfStringsMatchingLastWord(args, "overlay", "nametag");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("lang")) {
                return getListOfStringsMatchingLastWord(args, "en", "cn");
            }
            return null;
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
}