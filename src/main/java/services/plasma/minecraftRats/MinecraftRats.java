package services.plasma.minecraftRats;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import services.plasma.minecraftRats.commands.RatsCommand;
import services.plasma.minecraftRats.config.ConfigManager;
import services.plasma.minecraftRats.listeners.BlockBreakListener;
import services.plasma.minecraftRats.listeners.RatListener;
import services.plasma.minecraftRats.managers.RatManager;

public final class MinecraftRats extends JavaPlugin {

    private static MinecraftRats instance;
    private ConfigManager configManager;
    private RatManager ratManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        ratManager = new RatManager(this);

        getCommand("rats").setExecutor(new RatsCommand(this));

        Bukkit.getPluginManager().registerEvents(new RatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(this), this);

        if (configManager.isNaturalSpawningEnabled()) {
            ratManager.startSpawningTask();
        }

        getLogger().info("MinecraftRats plugin has been enabled!");
        getLogger().info("Adding rats to your Minecraft world...");
    }

    @Override
    public void onDisable() {
        // Stop spawning task
        if (ratManager != null) {
            ratManager.stopSpawningTask();
            ratManager.removeAllRats();
        }

        getLogger().info("MinecraftRats plugin has been disabled!");
    }

    /**
     * Get the plugin instance
     * @return MinecraftRats instance
     */
    public static MinecraftRats getInstance() {
        return instance;
    }

    /**
     * Get the configuration manager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the rat manager
     * @return RatManager instance
     */
    public RatManager getRatManager() {
        return ratManager;
    }

    /**
     * Reload the plugin configuration
     */
    public void reload() {
        configManager.reloadConfig();

        if (ratManager != null) {
            ratManager.stopSpawningTask();

            if (configManager.isNaturalSpawningEnabled()) {
                ratManager.startSpawningTask();
            }
        }

        getLogger().info("MinecraftRats configuration has been reloaded!");
    }
}