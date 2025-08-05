package net.mythofy.mythofyLaunchPads.listeners;

import net.mythofy.mythofyLaunchPads.MythofyLaunchPads;
import net.mythofy.mythofyLaunchPads.config.LaunchpadType;
import net.mythofy.mythofyLaunchPads.managers.LaunchpadManager;
import net.mythofy.mythofyLaunchPads.commands.SyncWandCommand;
import net.mythofy.mythofyLaunchPads.commands.SetSyncCommand;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class LaunchpadListerner implements Listener {

    private final MythofyLaunchPads plugin;
    private final LaunchpadManager launchpadManager;
    private SyncWandCommand syncWandCommand;
    private SetSyncCommand setSyncCommand;

    public LaunchpadListerner(MythofyLaunchPads plugin) {
        this.plugin = plugin;
        this.launchpadManager = plugin.getLaunchpadManager();
    }

    public void setSyncWandCommand(SyncWandCommand syncWandCommand) {
        this.syncWandCommand = syncWandCommand;
    }

    public void setSetSyncCommand(SetSyncCommand setSyncCommand) {
        this.setSyncCommand = setSyncCommand;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (!launchpadManager.isLaunchpadItem(item))
            return;

        String typeId = launchpadManager.getLaunchpadTypeFromItem(item);
        if (typeId == null)
            return;

        LaunchpadType type = plugin.getConfigManager().getLaunchpadType(typeId);
        if (type == null)
            return;

        Block placedBlock = event.getBlock();
        if (placedBlock.getType() != type.getBlockType()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis launchpad can only be placed as " + type.getBlockType().name() + "!");
            return;
        }

        Location location = placedBlock.getLocation();
        launchpadManager.registerPlacedLaunchpad(location, typeId);

        location.getWorld().playSound(location, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
        location.getWorld().spawnParticle(Particle.CLOUD, location.clone().add(0.5, 1.0, 0.5), 10, 0.25, 0.25, 0.25,
                0.01);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (launchpadManager.isLaunchpad(block)) {
            Location location = block.getLocation();
            launchpadManager.removePlacedLaunchpad(location);

            location.getWorld().playSound(location, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
            location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location.clone().add(0.5, 0.5, 0.5),
                    20, 0.4, 0.4, 0.4, 0.05, block.getBlockData());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlock().equals(event.getTo().getBlock()))
            return;

        Player player = event.getPlayer();
        Block blockBelow = player.getLocation().getBlock().getRelative(0, -1, 0);
        
        if (!launchpadManager.isLaunchpad(blockBelow))
            return;

        String typeId = launchpadManager.getLaunchpadType(blockBelow.getLocation());
        LaunchpadType type = plugin.getConfigManager().getLaunchpadType(typeId);

        if (type == null) {
            launchpadManager.removePlacedLaunchpad(blockBelow.getLocation());
            return;
        }

        if (launchpadManager.isOnCooldown(player, type)) {
            int secondsLeft = launchpadManager.getRemainingCooldown(player, type);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("seconds", String.valueOf(secondsLeft));
            player.sendMessage(plugin.getConfigManager().getMessage("cooldown-active", placeholders));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        launchpadManager.setOnCooldown(player, type);
        launchpadManager.launchPlayer(player, type, blockBelow);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);
        Location launchLoc = blockBelow.getLocation().clone().add(0.5, 0.2, 0.5);
        player.getWorld().spawnParticle(Particle.CLOUD, launchLoc, 20, 0.4, 0.1, 0.4, 0.2);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (syncWandCommand != null && syncWandCommand.isSyncWand(item)) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                setSyncCommand.setStartPoint(player.getUniqueId(), block.getLocation());
                player.sendMessage(ChatColor.GREEN + "Start point set at " + 
                    block.getX() + ", " + block.getY() + ", " + block.getZ());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                event.setCancelled(true);
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                setSyncCommand.setEndPoint(player.getUniqueId(), block.getLocation());
                player.sendMessage(ChatColor.BLUE + "End point set at " + 
                    block.getX() + ", " + block.getY() + ", " + block.getZ());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                event.setCancelled(true);
            }
            return;
        }

        if (event.getAction() != Action.PHYSICAL)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        if (!launchpadManager.isLaunchpad(block))
            return;

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
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        launchpadManager.setOnCooldown(player, type);
        launchpadManager.launchPlayer(player, type, block);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);
        Location launchLoc = block.getLocation().clone().add(0.5, 0.2, 0.5);
        player.getWorld().spawnParticle(Particle.CLOUD, launchLoc, 20, 0.4, 0.1, 0.4, 0.2);
    }
}
