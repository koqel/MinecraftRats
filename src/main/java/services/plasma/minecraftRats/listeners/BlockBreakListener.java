package services.plasma.minecraftRats.listeners;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import services.plasma.minecraftRats.MinecraftRats;
import services.plasma.minecraftRats.config.ConfigManager;
import services.plasma.minecraftRats.managers.RatManager;

import java.util.concurrent.ThreadLocalRandom;

public class BlockBreakListener implements Listener {

    private final MinecraftRats plugin;
    private final ConfigManager configManager;
    private final RatManager ratManager;

    public BlockBreakListener(MinecraftRats plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.ratManager = plugin.getRatManager();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        if (!configManager.isBlockSpawningEnabled()) {
            return;
        }

        String worldName = event.getBlock().getWorld().getName();
        if (!configManager.getEnabledWorlds().contains(worldName)) {
            return;
        }

        if (ratManager.getAllRats().size() >= configManager.getMaxRats()) {
            return;
        }

        Block block = event.getBlock();
        Material blockMaterial = block.getType();

        boolean isValidBlock = false;
        for (XMaterial xmat : configManager.getSpawnBlocks()) {
            if (xmat.parseMaterial() == blockMaterial) {
                isValidBlock = true;
                break;
            }
        }

        if (!isValidBlock) {
            return;
        }

        double random = ThreadLocalRandom.current().nextDouble();
        if (random > configManager.getBlockSpawnChance()) {
            return;
        }

        Location spawnLoc = block.getLocation().add(0.5, 0.1, 0.5);

        ratManager.spawnRat(spawnLoc);

        if (configManager.isDebug()) {
            plugin.getLogger().info("Spawned a rat from broken block at " +
                    spawnLoc.getWorld().getName() + " (" +
                    spawnLoc.getBlockX() + ", " +
                    spawnLoc.getBlockY() + ", " +
                    spawnLoc.getBlockZ() + ")");
        }
    }
}