package services.plasma.minecraftRats.entities;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import services.plasma.minecraftRats.MinecraftRats;
import services.plasma.minecraftRats.config.ConfigManager;
import services.plasma.minecraftRats.managers.RatManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Utility class for rat entity behavior
 */
public class RatEntity {

    private static final Random random = new Random();

    /**
     * Apply rat behavior to a rat entity
     *
     * @param rat The rat entity
     */
    public static void applyRatBehavior(LivingEntity rat) {
        MinecraftRats plugin = MinecraftRats.getInstance();
        ConfigManager configManager = plugin.getConfigManager();
        RatManager ratManager = plugin.getRatManager();

        if (!rat.hasMetadata("minecraftrats")) {
            return;
        }

        if (random.nextInt(configManager.getRatSoundInterval()) == 0) {
            rat.getWorld().playSound(rat.getLocation(), configManager.getRatSound(), 0.5f, 1.2f);
        }

        if (!configManager.getRatScaredOf().isEmpty()) {
            List<Entity> nearbyEntities = rat.getNearbyEntities(8, 8, 8);
            for (Entity entity : nearbyEntities) {
                if (configManager.getRatScaredOf().contains(entity.getType())) {
                    runAwayFrom(rat, entity.getLocation());
                    return;
                }
            }
        }

        if (configManager.isRatStealItems() && random.nextDouble() < 0.1) {
            stealNearbyItems(rat);
        }

        if (configManager.isNestsEnabled() && random.nextInt(200) == 0) {
            Location nestLocation = ratManager.getRatNest(rat.getUniqueId());
            if (nestLocation != null && rat.getLocation().distance(nestLocation) < configManager.getWanderDistance()) {
                moveTowards(rat, nestLocation);
                return;
            }
        }

        if (random.nextInt(20) == 0) {
            randomMovement(rat);
        }
    }

    /**
     * Make the rat run away from a location
     *
     * @param rat The rat entity
     * @param location The location to run away from
     */
    private static void runAwayFrom(LivingEntity rat, Location location) {
        Vector direction = rat.getLocation().toVector().subtract(location.toVector()).normalize();

        rat.setVelocity(direction.multiply(0.5));
    }

    /**
     * Make the rat move towards a location
     *
     * @param rat The rat entity
     * @param location The location to move towards
     */
    private static void moveTowards(LivingEntity rat, Location location) {
        Vector direction = location.toVector().subtract(rat.getLocation().toVector()).normalize();

        rat.setVelocity(direction.multiply(0.3));
    }

    /**
     * Make the rat move randomly
     *
     * @param rat The rat entity
     */
    private static void randomMovement(LivingEntity rat) {
        double x = (random.nextDouble() - 0.5) * 0.2;
        double z = (random.nextDouble() - 0.5) * 0.2;

        rat.setVelocity(new Vector(x, 0, z));
    }

    /**
     * Make the rat steal nearby items
     *
     * @param rat The rat entity
     */
    private static void stealNearbyItems(LivingEntity rat) {
        MinecraftRats plugin = MinecraftRats.getInstance();
        ConfigManager configManager = plugin.getConfigManager();

        int currentItems = 0;
        if (rat.hasMetadata("rat_items")) {
            currentItems = rat.getMetadata("rat_items").get(0).asInt();
        }

        if (currentItems >= configManager.getRatMaxItems()) {
            return;
        }

        List<Entity> nearbyEntities = rat.getNearbyEntities(2, 2, 2);
        List<Item> nearbyItems = new ArrayList<>();

        for (Entity entity : nearbyEntities) {
            if (entity instanceof Item) {
                nearbyItems.add((Item) entity);
            }
        }

        if (!nearbyItems.isEmpty()) {
            Item itemToSteal = nearbyItems.get(random.nextInt(nearbyItems.size()));

            ItemStack stolenItem = itemToSteal.getItemStack().clone();
            stolenItem.setAmount(1);
            itemToSteal.getItemStack().setAmount(itemToSteal.getItemStack().getAmount() - 1);

            if (itemToSteal.getItemStack().getAmount() <= 0) {
                itemToSteal.remove();
            }

            rat.setMetadata("rat_items", new FixedMetadataValue(plugin, currentItems + 1));

            rat.getWorld().playSound(rat.getLocation(), Sound.ENTITY_SILVERFISH_AMBIENT, 0.5f, 1.0f);

            if (configManager.isDebug()) {
                plugin.getLogger().info("Rat stole item: " + stolenItem.getType().name());
            }
        }
    }

    /**
     * Drop stolen items when the rat dies
     *
     * @param rat The rat entity
     */
    public static void dropStolenItems(LivingEntity rat) {
        if (rat.hasMetadata("rat_items")) {
            int stolenItems = rat.getMetadata("rat_items").get(0).asInt();

            for (int i = 0; i < stolenItems; i++) {
                EntityType[] commonItems = {
                        EntityType.DROPPED_ITEM
                };

                Location dropLocation = rat.getLocation();
                dropLocation.getWorld().dropItemNaturally(dropLocation, new ItemStack(org.bukkit.Material.GOLD_NUGGET, 1));
            }
        }
    }
}