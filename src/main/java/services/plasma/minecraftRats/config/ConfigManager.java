package services.plasma.minecraftRats.config;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import services.plasma.minecraftRats.MinecraftRats;

import java.io.File;
import java.util.*;

public class ConfigManager {

    private final MinecraftRats plugin;
    private FileConfiguration config;
    private File configFile;

    private boolean enabled;
    private boolean debug;

    private double ratHealth;
    private double ratSpeed;
    private double ratDamage;
    private boolean ratAttackPlayers;
    private boolean ratAttackMobs;
    private List<EntityType> ratScaredOf;
    private boolean ratStealItems;
    private int ratMaxItems;
    private Sound ratSound;
    private int ratSoundInterval;

    private boolean naturalSpawning;
    private int maxRats;
    private int minLightLevel;
    private int maxLightLevel;
    private double spawnChance;
    private List<String> enabledWorlds;
    private List<Biome> preferredBiomes;
    private List<Biome> disabledBiomes;

    private boolean blockSpawningEnabled;
    private double blockSpawnChance;
    private List<XMaterial> spawnBlocks;

    private boolean dropsEnabled;
    private Map<XMaterial, RatDrop> drops;

    private boolean nestsEnabled;
    private List<XMaterial> nestMaterials;
    private int maxNests;
    private int wanderDistance;
    private int returnInterval;

    private boolean customColorsEnabled;
    private Map<String, Double> colorChances;

    private String prefix;
    private String reloadMessage;
    private String spawnMessage;
    private String killMessage;
    private String noPermissionMessage;

    public ConfigManager(MinecraftRats plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    /**
     * Load the configuration
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadSettings();
    }

    /**
     * Reload the configuration
     */
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        loadSettings();
    }

    /**
     * Load all settings from the config
     */
    private void loadSettings() {
        enabled = config.getBoolean("general.enabled", true);
        debug = config.getBoolean("general.debug", false);

        ratHealth = config.getDouble("rat.health", 10.0);
        ratSpeed = config.getDouble("rat.speed", 0.25);
        ratDamage = config.getDouble("rat.damage", 2.0);
        ratAttackPlayers = config.getBoolean("rat.attack-players", true);
        ratAttackMobs = config.getBoolean("rat.attack-mobs", false);

        ratScaredOf = new ArrayList<>();
        List<String> scaredOfList = config.getStringList("rat.scared-of");
        for (String entityName : scaredOfList) {
            try {
                EntityType entityType = EntityType.valueOf(entityName.toUpperCase());
                ratScaredOf.add(entityType);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid entity type in scared-of list: " + entityName);
            }
        }

        ratStealItems = config.getBoolean("rat.steal-items", true);
        ratMaxItems = config.getInt("rat.max-items", 3);

        String soundName = config.getString("rat.sound", "ENTITY_BAT_AMBIENT");
        try {
            ratSound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName + ". Using default sound.");
            ratSound = Sound.ENTITY_BAT_AMBIENT;
        }

        ratSoundInterval = config.getInt("rat.sound-interval", 100);

        naturalSpawning = config.getBoolean("spawning.natural-spawning", true);
        maxRats = config.getInt("spawning.max-rats", 50);
        minLightLevel = config.getInt("spawning.min-light-level", 0);
        maxLightLevel = config.getInt("spawning.max-light-level", 7);
        spawnChance = config.getDouble("spawning.chance", 0.05);
        enabledWorlds = config.getStringList("spawning.enabled-worlds");

        preferredBiomes = new ArrayList<>();
        List<String> preferredBiomeList = config.getStringList("spawning.preferred-biomes");
        for (String biomeName : preferredBiomeList) {
            try {
                Biome biome = Biome.valueOf(biomeName.toUpperCase());
                preferredBiomes.add(biome);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid biome in preferred-biomes list: " + biomeName);
            }
        }

        disabledBiomes = new ArrayList<>();
        List<String> disabledBiomeList = config.getStringList("spawning.disabled-biomes");
        for (String biomeName : disabledBiomeList) {
            try {
                Biome biome = Biome.valueOf(biomeName.toUpperCase());
                disabledBiomes.add(biome);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid biome in disabled-biomes list: " + biomeName);
            }
        }

        blockSpawningEnabled = config.getBoolean("block-spawning.enabled", true);
        blockSpawnChance = config.getDouble("block-spawning.chance", 0.05);

        spawnBlocks = new ArrayList<>();
        List<String> blocksList = config.getStringList("block-spawning.blocks");
        for (String blockName : blocksList) {
            Optional<XMaterial> material = XMaterial.matchXMaterial(blockName.toUpperCase());
            if (material.isPresent()) {
                spawnBlocks.add(material.get());
            } else {
                plugin.getLogger().warning("Invalid material in spawn blocks list: " + blockName);
            }
        }

        dropsEnabled = config.getBoolean("drops.enabled", true);
        drops = new HashMap<>();

        List<String> dropsList = config.getStringList("drops.items");
        for (String dropString : dropsList) {
            String[] parts = dropString.split(":");
            if (parts.length == 3) {
                try {
                    String materialName = parts[0].toUpperCase();
                    Optional<XMaterial> material = XMaterial.matchXMaterial(materialName);
                    if (material.isPresent()) {
                        int amount = Integer.parseInt(parts[1]);
                        double chance = Double.parseDouble(parts[2]);
                        drops.put(material.get(), new RatDrop(material.get(), amount, chance));
                    } else {
                        plugin.getLogger().warning("Invalid material in drops list: " + materialName);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid drop format: " + dropString);
                }
            } else {
                plugin.getLogger().warning("Invalid drop format: " + dropString);
            }
        }

        nestsEnabled = config.getBoolean("nests.enabled", true);
        nestMaterials = new ArrayList<>();

        List<String> nestMaterialsList = config.getStringList("nests.materials");
        for (String materialName : nestMaterialsList) {
            Optional<XMaterial> material = XMaterial.matchXMaterial(materialName.toUpperCase());
            if (material.isPresent()) {
                nestMaterials.add(material.get());
            } else {
                plugin.getLogger().warning("Invalid material in nest materials list: " + materialName);
            }
        }

        maxNests = config.getInt("nests.max-nests", 15);
        wanderDistance = config.getInt("nests.wander-distance", 16);
        returnInterval = config.getInt("nests.return-interval", 12000);

        customColorsEnabled = config.getBoolean("colors.enabled", true);
        colorChances = new HashMap<>();

        ConfigurationSection colorsSection = config.getConfigurationSection("colors.list");
        if (colorsSection != null) {
            for (String color : colorsSection.getKeys(false)) {
                double chance = colorsSection.getDouble(color, 0.0);
                colorChances.put(color.toUpperCase(), chance);
            }
        }

        prefix = config.getString("messages.prefix", "&7[&eMinecraftRats&7] ");
        reloadMessage = config.getString("messages.reload", "&aConfiguration reloaded successfully!");
        spawnMessage = config.getString("messages.spawn", "&aSpawned %amount% rats!");
        killMessage = config.getString("messages.kill", "&cKilled %amount% rats!");
        noPermissionMessage = config.getString("messages.no-permission", "&cYou don't have permission to use this command!");
    }


    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public double getRatHealth() {
        return ratHealth;
    }

    public double getRatSpeed() {
        return ratSpeed;
    }

    public double getRatDamage() {
        return ratDamage;
    }

    public boolean isRatAttackPlayers() {
        return ratAttackPlayers;
    }

    public boolean isRatAttackMobs() {
        return ratAttackMobs;
    }

    public List<EntityType> getRatScaredOf() {
        return ratScaredOf;
    }

    public boolean isRatStealItems() {
        return ratStealItems;
    }

    public int getRatMaxItems() {
        return ratMaxItems;
    }

    public Sound getRatSound() {
        return ratSound;
    }

    public int getRatSoundInterval() {
        return ratSoundInterval;
    }

    public boolean isNaturalSpawningEnabled() {
        return naturalSpawning;
    }

    public int getMaxRats() {
        return maxRats;
    }

    public int getMinLightLevel() {
        return minLightLevel;
    }

    public int getMaxLightLevel() {
        return maxLightLevel;
    }

    public double getSpawnChance() {
        return spawnChance;
    }

    public List<String> getEnabledWorlds() {
        return enabledWorlds;
    }

    public List<Biome> getPreferredBiomes() {
        return preferredBiomes;
    }

    public List<Biome> getDisabledBiomes() {
        return disabledBiomes;
    }

    public boolean isBlockSpawningEnabled() {
        return blockSpawningEnabled;
    }

    public double getBlockSpawnChance() {
        return blockSpawnChance;
    }

    public List<XMaterial> getSpawnBlocks() {
        return spawnBlocks;
    }

    public boolean isDropsEnabled() {
        return dropsEnabled;
    }

    public Map<XMaterial, RatDrop> getDrops() {
        return drops;
    }

    public boolean isNestsEnabled() {
        return nestsEnabled;
    }

    public List<XMaterial> getNestMaterials() {
        return nestMaterials;
    }

    public int getMaxNests() {
        return maxNests;
    }

    public int getWanderDistance() {
        return wanderDistance;
    }

    public int getReturnInterval() {
        return returnInterval;
    }

    public boolean isCustomColorsEnabled() {
        return customColorsEnabled;
    }

    public Map<String, Double> getColorChances() {
        return colorChances;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getReloadMessage() {
        return reloadMessage;
    }

    public String getSpawnMessage() {
        return spawnMessage;
    }

    public String getKillMessage() {
        return killMessage;
    }

    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    /**
     * Inner class to represent a rat drop
     */
    public static class RatDrop {
        private final XMaterial material;
        private final int amount;
        private final double chance;

        public RatDrop(XMaterial material, int amount, double chance) {
            this.material = material;
            this.amount = amount;
            this.chance = chance;
        }

        public XMaterial getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        public double getChance() {
            return chance;
        }
    }
}