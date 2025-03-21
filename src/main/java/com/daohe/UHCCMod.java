package com.daohe;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

@Mod(modid = "uhccmod", name = "UHC Checker", version = "1.2.5", clientSideOnly = true)
public class UHCCMod {
    public static String apiKey = "";
    public static boolean debugMode = false;
    public static volatile boolean isDetecting = false;

    private final ConfigManager configManager = new ConfigManager(this);
    private final PlayerStatsManager statsManager = new PlayerStatsManager(this);
    private final CommandHandler commandHandler = new CommandHandler(this);
    private final RenderHandler renderHandler = new RenderHandler(this);
    private final EventHandler eventHandler = new EventHandler(this);
    private final PlayerQueryHandler queryHandler = new PlayerQueryHandler(this);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configManager.loadConfig(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(eventHandler);
        MinecraftForge.EVENT_BUS.register(renderHandler);
        MinecraftForge.EVENT_BUS.register(new KillCount(this));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(commandHandler.getCommand());
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        statsManager.shutdownExecutor();
        queryHandler.shutdown();
    }

    public ConfigManager getConfigManager() { return configManager; }
    public PlayerStatsManager getStatsManager() { return statsManager; }
    public RenderHandler getRenderHandler() { return renderHandler; }
    public EventHandler getEventHandler() { return eventHandler; }
}