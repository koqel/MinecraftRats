package services.plasma.minecraftRats.managers;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import services.plasma.minecraftRats.MinecraftRats;
import services.plasma.minecraftRats.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class RatManager {

    private final MinecraftRats plugin;
    private final ConfigManager configManager;
    private final List<UUID> activeRats;
    private final Map<UUID, Location> ratNests;
    private BukkitTask spawningTask;

    public RatManager(MinecraftRats plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.activeRats = new ArrayList<>();
        this.ratNests = new HashMap<>();
    }

    /**
     * Start the rat spawning task
     */
    public void startSpawningTask() {
        if (spawningTask != null) {
            spawningTask.cancel();
        }

        spawningTask = Bukkit.getScheduler().runTaskTimer(plugin, this::trySpawnRats, 200L, 600L);
    }

    /**
     * Stop the rat spawning task
     */
    public void stopSpawningTask() {
        if (spawningTask != null) {
            spawningTask.cancel();
            spawningTask = null;
        }
    }

    /**
     * Try to spawn rats in all enabled worlds
     */
    private void trySpawnRats() {
        if (!configManager.isNaturalSpawningEnabled()) {
            return;
        }

        if (activeRats.size() >= configManager.getMaxRats()) {
            return;
        }

        List<String> enabledWorldNames = configManager.getEnabledWorlds();
        for (String worldName : enabledWorldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                trySpawnRatsInWorld(world);
            }
        }
    }

    /**
     * Try to spawn rats in a specific world
     *
     * @param world The world to spawn rats in
     */
    private void trySpawnRatsInWorld(World world) {
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            if (Math.random() > configManager.getSpawnChance()) {
                continue;
            }

            if (activeRats.size() >= configManager.getMaxRats()) {
                return;
            }

            Location spawnLoc = findSpawnLocation(player.getLocation());
            if (spawnLoc != null) {
                spawnRat(spawnLoc);
            }
        }
    }

    /**
     * Find a valid location to spawn a rat
     *
     * @param playerLoc The player's location
     * @return A valid spawn location, or null if none was found
     */
    private Location findSpawnLocation(Location playerLoc) {
        World world = playerLoc.getWorld();
        if (world == null) {
            return null;
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            int xOffset = ThreadLocalRandom.current().nextInt(16, 33) * (Math.random() > 0.5 ? 1 : -1);
            int zOffset = ThreadLocalRandom.current().nextInt(16, 33) * (Math.random() > 0.5 ? 1 : -1);

            Location candidateLoc = playerLoc.clone().add(xOffset, 0, zOffset);

            candidateLoc.setY(world.getHighestBlockYAt(candidateLoc));

            if (isValidSpawnLocation(candidateLoc)) {
                return candidateLoc;
            }
        }

        return null;
    }

    /**
     * Check if a location is valid for spawning a rat
     *
     * @param location The location to check
     * @return True if the location is valid, false otherwise
     */
    private boolean isValidSpawnLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        if (!configManager.getEnabledWorlds().contains(world.getName())) {
            return false;
        }

        Block block = location.getBlock();
        Block blockBelow = block.getRelative(0, -1, 0);
        Block blockAbove = block.getRelative(0, 1, 0);

        if (!blockBelow.getType().isSolid()) {
            return false;
        }

        if (block.getType().isSolid() || blockAbove.getType().isSolid()) {
            return false;
        }

        Biome biome = block.getBiome();
        if (configManager.getDisabledBiomes().contains(biome)) {
            return false;
        }

        int lightLevel = block.getLightLevel();
        if (lightLevel < configManager.getMinLightLevel() || lightLevel > configManager.getMaxLightLevel()) {
            return false;
        }

        return true;
    }

    /**
     * Spawn a rat at the given location
     *
     * @param location The location to spawn the rat at
     * @return The spawned rat entity
     */
    public LivingEntity spawnRat(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        LivingEntity ratEntity = (LivingEntity) world.spawnEntity(location, EntityType.SILVERFISH);

        String color = getRandomRatColor();
        ratEntity.setCustomName("§7" + color + " Rat");
        ratEntity.setCustomNameVisible(true);

        ratEntity.setMetadata("minecraftrats", new FixedMetadataValue(plugin, "rat"));
        ratEntity.setMetadata("rat_color", new FixedMetadataValue(plugin, color));

        ratEntity.setMaxHealth(configManager.getRatHealth());
        ratEntity.setHealth(configManager.getRatHealth());

        ratEntity.setPersistent(true);

        activeRats.add(ratEntity.getUniqueId());

        if (configManager.isDebug()) {
            plugin.getLogger().info("Spawned a rat at " + location.getWorld().getName() +
                    " (" + location.getX() + ", " + location.getY() + ", " + location.getZ() + ")");
        }

        if (configManager.isNestsEnabled() && ratNests.size() < configManager.getMaxNests() && Math.random() < 0.3) {
            createNest(location, ratEntity.getUniqueId());
        }

        return ratEntity;
    }

    /**
     * Get a random rat color based on the configured chances
     *
     * @return A random rat color
     */
    private String getRandomRatColor() {
        if (!configManager.isCustomColorsEnabled() || configManager.getColorChances().isEmpty()) {
            return "Brown";
        }

        Map<String, Double> colorChances = configManager.getColorChances();

        double totalProbability = colorChances.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalProbability <= 0) {
            return "Brown";
        }

        double random = Math.random() * totalProbability;
        double cumulativeProbability = 0.0;

        for (Map.Entry<String, Double> entry : colorChances.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (random <= cumulativeProbability) {
                return entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1).toLowerCase();
            }
        }

        return "Brown";
    }

    /**
     * Create a rat nest at the given location
     *
     * @param location The location to create the nest at
     * @param ratId The UUID of the rat that the nest belongs to
     */
    private void createNest(Location location, UUID ratId) {
        if (!configManager.isNestsEnabled()) {
            return;
        }

        if (ratNests.size() >= configManager.getMaxNests()) {
            return;
        }

        ratNests.put(ratId, location);

        if (configManager.isDebug()) {
            plugin.getLogger().info("Created a rat nest at " + location.getWorld().getName() +
                    " (" + location.getX() + ", " + location.getY() + ", " + location.getZ() + ")");
        }
    }

    /**
     * Get all active rats in the server
     *
     * @return A list of active rat entities
     */
    public List<LivingEntity> getAllRats() {
        List<LivingEntity> rats = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getLivingEntities()) {
                if (entity instanceof LivingEntity && entity.hasMetadata("minecraftrats")) {
                    rats.add((LivingEntity) entity);
                }
            }
        }

        return rats;
    }

    /**
     * Remove all rats from the server
     *
     * @return The number of rats removed
     */
    public int removeAllRats() {
        int count = 0;

        List<LivingEntity> rats = getAllRats();

        for (LivingEntity rat : rats) {
            rat.remove();
            count++;
        }

        activeRats.clear();
        ratNests.clear();

        return count;
    }

    /**
     * Spawn a specific number of rats at a location
     *
     * @param location The location to spawn rats at
     * @param amount The number of rats to spawn
     * @return The number of rats successfully spawned
     */
    public int spawnRats(Location location, int amount) {
        int count = 0;

        for (int i = 0; i < amount; i++) {
            if (activeRats.size() >= configManager.getMaxRats()) {
                break;
            }

            LivingEntity rat = spawnRat(location);
            if (rat != null) {
                count++;
            }
        }

        return count;
    }

    /**
     * Get drops for a rat
     *
     * @return A list of items to drop
     */
    public List<ItemStack> getRatDrops() {
        List<ItemStack> drops = new ArrayList<>();

        if (!configManager.isDropsEnabled()) {
            return drops;
        }

        Map<XMaterial, ConfigManager.RatDrop> possibleDrops = configManager.getDrops();

        for (ConfigManager.RatDrop drop : possibleDrops.values()) {
            if (Math.random() <= drop.getChance()) {
                Optional<ItemStack> item = Optional.ofNullable(drop.getMaterial().parseItem());
                if (item.isPresent()) {
                    ItemStack dropItem = item.get();
                    dropItem.setAmount(drop.getAmount());
                    drops.add(dropItem);
                }
            }
        }

        return drops;
    }

    /**
     * Get the nest location for a rat
     *
     * @param ratId The UUID of the rat
     * @return The nest location, or null if the rat doesn't have a nest
     */
    public Location getRatNest(UUID ratId) {
        return ratNests.get(ratId);
    }
}