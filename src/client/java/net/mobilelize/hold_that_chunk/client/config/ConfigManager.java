package net.mobilelize.hold_that_chunk.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.mobilelize.hold_that_chunk.client.Hold_that_chunkClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File(MinecraftClient.getInstance().runDirectory, "config/HoldThatChunk_V2");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "hold_that_chunk.json");

    public static ConfigData configData = new ConfigData();

    public static class ConfigData {
        public boolean respectServerDistance = false; //Uses Server Render distance.
        public boolean linkRenderDistance = true;
        public boolean holdThatChunkEnabled = true;
        public int holdDistance = 64;
    }

    public static void loadConfig() {
        try {
            if (!CONFIG_FILE.exists()) {
                saveDefaultConfig();
            }
            try (FileReader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                configData = GSON.fromJson(reader, ConfigData.class);
            }
        } catch (Exception e) {
            Hold_that_chunkClient.logger.error("Failed to load config: {}", e.getMessage());
        }
    }

    public static void saveConfig() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(configData, writer);
            }
        } catch (Exception e) {
            Hold_that_chunkClient.logger.error("[HoldThatChunk] Failed to save config: {}", e.getMessage());
        }
    }

    private static void saveDefaultConfig() {
        saveConfig();
    }
}
