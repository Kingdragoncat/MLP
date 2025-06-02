package net.mythofy.mythofyLaunchPads.listeners;

import net.mythofy.mythofyLaunchPads.MythofyLaunchPads;
import net.mythofy.mythofyLaunchPads.config.LaunchpadType;
import net.mythofy.mythofyLaunchPads.managers.LaunchpadManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class LaunchpadListerner implements Listener {

    private final MythofyLaunchPads plugin;
    private final LaunchpadManager launchpadManager;

    public LaunchpadListerner(MythofyLaunchPads plugin) {
        this.plugin = plugin;
        this.launchpadManager = plugin.getLaunchpadManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (!launchpadManager.isLaunchpadItem(item)) return;

        String typeId = launchpadManager.getLaunchpadTypeFromItem(item);
        if (typeId == null) return;

        Location location = event.getBlock().getLocation();
        LaunchpadType type = plugin.getConfigManager().getLaunchpadType(typeId);
        if (type == null) return;

        // Ensure the placed block matches the launchpad's block type
        if (event.getBlock().getType() != type.getBlockType()) return;

        launchpadManager.registerPlacedLaunchpad(location, typeId);

        // Play placement sound effect
        location.getWorld().playSound(location, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);

        // Show placement particle effect
        location.getWorld().spawnParticle(Particle.CLOUD, location.clone().add(0.5, 1.0, 0.5), 10, 0.25, 0.25, 0.25, 0.01);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (launchpadManager.isLaunchpad(block)) {
            Location location = block.getLocation();
            launchpadManager.removePlacedLaunchpad(location);

            // Play break sound effect
            location.getWorld().playSound(location, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);

            // Show break particle effect
            location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location.clone().add(0.5, 0.5, 0.5),
                    20, 0.4, 0.4, 0.4, 0.05, block.getBlockData());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!launchpadManager.isLaunchpad(block)) return;

        Player player = event.getPlayer();
        String typeId = launchpadManager.getLaunchpadType(block.getLocation());
        LaunchpadType type = plugin.getConfigManager().getLaunchpadType(typeId);

        if (type == null) {
            launchpadManager.removePlacedLaunchpad(block.getLocation());
            return;
        }

        if (launchpadManager.isOnCooldown(player, type)) {
            int secondsLeft = launchpadManager.getRemainingCooldown(player, type);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("seconds", String.valueOf(secondsLeft));
            player.sendMessage(plugin.getConfigManager().getMessage("cooldown-active", placeholders));

            // Play cooldown sound
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        launchpadManager.setOnCooldown(player, type);

        Vector direction = player.getLocation().getDirection().multiply(type.getForwardVelocity());
        direction.setY(type.getUpwardVelocity());
        player.setVelocity(direction);

        // Launch sound effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);

        // Launch particle effects
        Location launchLoc = block.getLocation().clone().add(0.5, 0.2, 0.5);

        // Base particle effect at the launchpad
        player.getWorld().spawnParticle(Particle.CLOUD, launchLoc, 20, 0.4, 0.1, 0.4, 0.2);

        // Trail effect behind the player based on launch power
        double effectStrength = (type.getUpwardVelocity() + type.getForwardVelocity()) / 2.0;
        int particleCount = (int)(20 * effectStrength);

        // Different particle effects based on launchpad strength
        if (effectStrength > 1.5) {
            // High-power launchpad
            player.getWorld().spawnParticle(Particle.FLAME, launchLoc, particleCount, 0.3, 0.1, 0.3, 0.1);
            player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, launchLoc, 3, 0.2, 0.1, 0.2, 0.01);
        } else if (effectStrength > 0.8) {
            // Medium-power launchpad
            player.getWorld().spawnParticle(Particle.CRIT, launchLoc, particleCount, 0.3, 0.1, 0.3, 0.1);
        } else {
            // Low-power launchpad
            player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, launchLoc, particleCount, 0.3, 0.1, 0.3, 0.05);
        }
    }
}
