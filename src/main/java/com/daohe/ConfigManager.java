package com.daohe;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final UHCCMod mod;
    private Configuration config;
    private static final DecimalFormat df = new DecimalFormat("#.##");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static String currentLang = "en_US";
    private static final Map<String, Map<String, String>> languageMap = new HashMap<>();
    public static long apiKeySetTime = 0;

    private static final Map<String, String> DEFAULT_ARTIFACTS_EN = new HashMap<>();
    private static final Map<String, String> DEFAULT_ARTIFACTS_CN = new HashMap<>();

    static {
        DEFAULT_ARTIFACTS_EN.put("artemis_bow", "Artemis Bow");
        DEFAULT_ARTIFACTS_EN.put("flask_of_ichor", "Flask Of Ichor");
        DEFAULT_ARTIFACTS_EN.put("exodus", "Exodus");
        DEFAULT_ARTIFACTS_EN.put("hide_of_leviathan", "Hide Of Leviathan");
        DEFAULT_ARTIFACTS_EN.put("tablets_of_destiny", "Tablets Of Destiny");
        DEFAULT_ARTIFACTS_EN.put("axe_of_perun", "Axe Of Perun");
        DEFAULT_ARTIFACTS_EN.put("excalibur", "Excalibur");
        DEFAULT_ARTIFACTS_EN.put("anduril", "Anduril");
        DEFAULT_ARTIFACTS_EN.put("deaths_scythe", "Deaths Scythe");
        DEFAULT_ARTIFACTS_EN.put("chest_of_fate", "Chest Of Fate");
        DEFAULT_ARTIFACTS_EN.put("cornucopia", "Cornucopia");
        DEFAULT_ARTIFACTS_EN.put("essence_of_yggdrasil", "Essence Of Yggdrasil");
        DEFAULT_ARTIFACTS_EN.put("voidbox", "Voidbox");
        DEFAULT_ARTIFACTS_EN.put("deus_ex_machina", "Deus Ex Machina");
        DEFAULT_ARTIFACTS_EN.put("dice_of_god", "Dice Of God");
        DEFAULT_ARTIFACTS_EN.put("kings_rod", "Kings Rod");
        DEFAULT_ARTIFACTS_EN.put("daredevil", "Daredevil");
        DEFAULT_ARTIFACTS_EN.put("flask_of_cleansing", "Flask Of Cleansing");
        DEFAULT_ARTIFACTS_EN.put("shoes_of_vidar", "Shoes Of Vidar");
        DEFAULT_ARTIFACTS_EN.put("potion_of_vitality", "Potion Of Vitality");
        DEFAULT_ARTIFACTS_EN.put("miners_blessing", "Miners Blessing");
        DEFAULT_ARTIFACTS_EN.put("ambrosia", "Ambrosia");
        DEFAULT_ARTIFACTS_EN.put("bloodlust", "Bloodlust");
        DEFAULT_ARTIFACTS_EN.put("modular_bow", "Modular Bow");
        DEFAULT_ARTIFACTS_EN.put("expert_seal", "Expert Seal");
        DEFAULT_ARTIFACTS_EN.put("hermes_boots", "Hermes Boots");
        DEFAULT_ARTIFACTS_EN.put("barbarian_chestplate", "Barbarian Chestplate");

        DEFAULT_ARTIFACTS_CN.put("artemis_bow", "自瞄弓");
        DEFAULT_ARTIFACTS_CN.put("flask_of_ichor", "瞬三药");
        DEFAULT_ARTIFACTS_CN.put("exodus", "永生帽");
        DEFAULT_ARTIFACTS_CN.put("hide_of_leviathan", "潮汐裤");
        DEFAULT_ARTIFACTS_CN.put("tablets_of_destiny", "命运之书");
        DEFAULT_ARTIFACTS_CN.put("axe_of_perun", "雷斧");
        DEFAULT_ARTIFACTS_CN.put("excalibur", "王剑");
        DEFAULT_ARTIFACTS_CN.put("anduril", "安德鲁");
        DEFAULT_ARTIFACTS_CN.put("deaths_scythe", "镰刀");
        DEFAULT_ARTIFACTS_CN.put("chest_of_fate", "命运之箱");
        DEFAULT_ARTIFACTS_CN.put("cornucopia", "丰饶之角");
        DEFAULT_ARTIFACTS_CN.put("essence_of_yggdrasil", "世界树精华");
        DEFAULT_ARTIFACTS_CN.put("voidbox", "虚空箱");
        DEFAULT_ARTIFACTS_CN.put("deus_ex_machina", "无敌药");
        DEFAULT_ARTIFACTS_CN.put("dice_of_god", "上帝之骰");
        DEFAULT_ARTIFACTS_CN.put("kings_rod", "王竿");
        DEFAULT_ARTIFACTS_CN.put("daredevil", "骷髅马");
        DEFAULT_ARTIFACTS_CN.put("flask_of_cleansing", "肃清");
        DEFAULT_ARTIFACTS_CN.put("shoes_of_vidar", "水鞋");
        DEFAULT_ARTIFACTS_CN.put("potion_of_vitality", "活力药");
        DEFAULT_ARTIFACTS_CN.put("miners_blessing", "矿神的祝福");
        DEFAULT_ARTIFACTS_CN.put("ambrosia", "密酒");
        DEFAULT_ARTIFACTS_CN.put("bloodlust", "杀人剑");
        DEFAULT_ARTIFACTS_CN.put("modular_bow", "变幻弓");
        DEFAULT_ARTIFACTS_CN.put("expert_seal", "大师卷轴");
        DEFAULT_ARTIFACTS_CN.put("hermes_boots", "小飞鞋");
        DEFAULT_ARTIFACTS_CN.put("barbarian_chestplate", "力量甲");
    }

    public ConfigManager(UHCCMod mod) {
        this.mod = mod;
    }

    public void loadConfig(File configFile) {
        config = new Configuration(configFile);
        config.load();

        UHCCMod.apiKey = config.getString("apiKey", Configuration.CATEGORY_GENERAL, "", "");
        String apiKeyTimeStr = config.getString("apiKeySetTime", Configuration.CATEGORY_GENERAL, "", "");
        if (!apiKeyTimeStr.isEmpty()) {
            try {
                apiKeySetTime = sdf.parse(apiKeyTimeStr).getTime();
            } catch (Exception e) {
                apiKeySetTime = 0;
            }
        }

        RenderHandler.overlayX = getFormattedFloat(config, "overlayX", Configuration.CATEGORY_GENERAL, 90.0f, -1000.0f, 1000.0f);
        RenderHandler.overlayY = getFormattedFloat(config, "overlayY", Configuration.CATEGORY_GENERAL, 10.0f, -1000.0f, 1000.0f);
        RenderHandler.overlayScale = getFormattedFloat(config, "overlayScale", Configuration.CATEGORY_GENERAL, 0.6f, 0.01f, 1.0f);
        RenderHandler.nametagHeight = getFormattedFloat(config, "nametagHeight", Configuration.CATEGORY_GENERAL, 1.3f, -10.0f, 10.0f);
        RenderHandler.showOverlayStats = config.getBoolean("showOverlayStats", Configuration.CATEGORY_GENERAL, true, "");
        RenderHandler.showNametagStats = config.getBoolean("showNametagStats", Configuration.CATEGORY_GENERAL, true, "");
        RenderHandler.overlayMaxPlayers = config.getInt("overlayMaxPlayers", Configuration.CATEGORY_GENERAL, 25, 1, 1000, "");
        currentLang = config.getString("language", Configuration.CATEGORY_GENERAL, "en_US", "");

        config.save();

        loadLanguages(configFile.getParentFile());
        if (!languageMap.containsKey(currentLang)) {
            currentLang = "en_US";
        }
    }

    private float getFormattedFloat(Configuration config, String key, String category, float defaultValue, float minValue, float maxValue) {
        Property prop = config.get(category, key, defaultValue, "", minValue, maxValue);
        try {
            String valueStr = prop.getString();
            if (valueStr.isEmpty()) {
                prop.set(df.format(defaultValue));
                return defaultValue;
            }
            float value = Float.parseFloat(valueStr);
            if (value < minValue || value > maxValue) {
                prop.set(df.format(defaultValue));
                return defaultValue;
            }
            prop.set(df.format(value));
            return value;
        } catch (NumberFormatException e) {
            prop.set(df.format(defaultValue));
            return defaultValue;
        }
    }

    private void loadLanguages(File modDir) {
        File langDir = new File(modDir, "assets/uhccmod/lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        String[] langs = {"en_US", "zh_CN"};
        for (String lang : langs) {
            Map<String, String> translations = new HashMap<>();
            File langFile = new File(langDir, lang + ".lang");
            if (!langFile.exists()) {
                try (InputStream is = getClass().getResourceAsStream("/assets/uhccmod/lang/" + lang + ".lang")) {
                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                        parseLangFile(reader, translations);
                    }
                } catch (IOException e) {
                    System.out.println("Failed to load default language " + lang + ": " + e.getMessage());
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8))) {
                    parseLangFile(reader, translations);
                } catch (IOException e) {
                    System.out.println("Failed to load language file " + lang + ": " + e.getMessage());
                }
            }
            Map<String, String> artifactMap = lang.equals("en_US") ? DEFAULT_ARTIFACTS_EN : DEFAULT_ARTIFACTS_CN;
            for (Map.Entry<String, String> entry : artifactMap.entrySet()) {
                translations.put("uhccmod.artifact." + entry.getKey(), entry.getValue());
            }
            languageMap.put(lang, translations);
        }
    }

    private void parseLangFile(BufferedReader reader, Map<String, String> translations) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                translations.put(parts[0].trim(), parts[1].trim());
            }
        }
    }

    public Configuration getConfig() { return config; }
    public Map<String, Map<String, String>> getLanguageMap() { return languageMap; }

    public static String translate(String key, Object... args) {
        Map<String, String> translations = languageMap.getOrDefault(currentLang, languageMap.get("en_US"));
        if (translations == null || !translations.containsKey("uhccmod." + key)) {
            if (UHCCMod.debugMode) {
                System.out.println("Translation missing for key: uhccmod." + key + " in " + currentLang);
            }
            return "uhccmod." + key;
        }
        String format = translations.get("uhccmod." + key);
        try {
            return String.format(format, args);
        } catch (IllegalArgumentException e) {
            if (UHCCMod.debugMode) {
                System.out.println("Format error for key: uhccmod." + key + " with args: " + java.util.Arrays.toString(args));
            }
            return format;
        }
    }
}