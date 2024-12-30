package space.frahm.minecraft.idleshutdown;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.*;

public class IdleShutdownConfig {
    private static final int DEFAULT_MINUTES = 15;
    private int minutesUntilShutdown = DEFAULT_MINUTES;

    public int getMinutesUntilShutdown() {
        return minutesUntilShutdown;
    }

    public static IdleShutdownConfig createAndLoad(Logger LOGGER) {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File configFile = new File(configDir, "idle-shutdown.json");
        IdleShutdownConfig config = new IdleShutdownConfig();

        if (configFile.exists()) {
            LOGGER.info("Reading config from file.");
            try (Reader reader = new FileReader(configFile)) {
                config = new Gson().fromJson(reader, IdleShutdownConfig.class);
            } catch (Exception e) {
                LOGGER.error("Error loading config file", e);
            }
        } else {
            // Create default config
            LOGGER.info("Creating default config file.");
            try {
                configFile.getParentFile().mkdirs();
                try (Writer writer = new FileWriter(configFile)) {
                    new Gson().toJson(config, writer);
                }
            } catch (Exception e) {
                LOGGER.error("Error saving default config file", e);
            }
        }
        return config;
    }
}
