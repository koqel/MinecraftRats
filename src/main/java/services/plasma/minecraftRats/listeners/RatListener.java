package services.plasma.minecraftRats.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;
import services.plasma.minecraftRats.MinecraftRats;
import services.plasma.minecraftRats.config.ConfigManager;
import services.plasma.minecraftRats.entities.RatEntity;
import services.plasma.minecraftRats.managers.RatManager;

import java.util.List;

public class RatListener implements Listener {

    private final MinecraftRats plugin;
    private final ConfigManager configManager;
    private final RatManager ratManager;

    public RatListener(MinecraftRats plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.ratManager = plugin.getRatManager();
    }

    /**
     * Handle entity spawn events to apply rat behavior
     */
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof LivingEntity && entity.hasMetadata("minecraftrats")) {
            RatEntity.applyRatBehavior((LivingEntity) entity);
        }
    }

    /**
     * Handle entity death events to drop rat loot
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.hasMetadata("minecraftrats")) {
            event.getDrops().clear();

            if (configManager.isDropsEnabled()) {
                List<ItemStack> drops = ratManager.getRatDrops();
                event.getDrops().addAll(drops);

                RatEntity.dropStolenItems(entity);
            }

            if (entity.getUniqueId() != null) {
                plugin.getRatManager().getAllRats().remove(entity);
            }

            if (configManager.isDebug()) {
                plugin.getLogger().info("Rat died!");
            }
        }
    }

    /**
     * Handle entity targeting to implement rat AI
     */
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        Entity target = event.getTarget();

        if (entity.hasMetadata("minecraftrats")) {
            if (target instanceof Player && !configManager.isRatAttackPlayers()) {
                event.setCancelled(true);
                return;
            }

            if (!(target instanceof Player) && !configManager.isRatAttackMobs()) {
                event.setCancelled(true);
                return;
            }

            RatEntity.applyRatBehavior((LivingEntity) entity);
        }
    }

    /**
     * Handle damage events for rat combat
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damaged = event.getEntity();

        if (damager.hasMetadata("minecraftrats")) {
            event.setDamage(configManager.getRatDamage());

            if (damaged instanceof Player && !configManager.isRatAttackPlayers()) {
                event.setCancelled(true);
                return;
            }

            if (!(damaged instanceof Player) && !configManager.isRatAttackMobs()) {
                event.setCancelled(true);
                return;
            }
        }

        if (damaged.hasMetadata("minecraftrats")) {
            RatEntity.applyRatBehavior((LivingEntity) damaged);
        }
    }
}