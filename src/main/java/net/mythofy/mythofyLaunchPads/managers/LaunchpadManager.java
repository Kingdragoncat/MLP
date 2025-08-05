package net.mythofy.mythofyLaunchPads.managers;

import net.mythofy.mythofyLaunchPads.MythofyLaunchPads;
import net.mythofy.mythofyLaunchPads.config.LaunchpadType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;
import java.io.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LaunchpadManager {
    private final MythofyLaunchPads plugin;
    private final NamespacedKey launchpadTypeKey;
    private final Map<String, String> placedLaunchpads;
    private final Map<UUID, Map<String, Long>> playerCooldowns;
    private final Map<String, Location> syncedLaunchpads;
    private final File dataFile;

    public LaunchpadManager(MythofyLaunchPads plugin) {
        this.plugin = plugin;
        this.launchpadTypeKey = new NamespacedKey(plugin, "launchpad_type");
        this.placedLaunchpads = new HashMap<>();
        this.playerCooldowns = new HashMap<>();
        this.syncedLaunchpads = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "launchpads.yml");
        loadData();
    }

    public void launchPlayer(Player player, LaunchpadType type, Block block) {
        Location blockLoc = block.getLocation();
        String locationKey = locationKey(blockLoc);
        
        if (syncedLaunchpads.containsKey(locationKey)) {
            launchPlayerToTarget(player, syncedLaunchpads.get(locationKey));
        } else {
            Location playerLoc = player.getLocation();
            Vector direction = playerLoc.getDirection().normalize();
            
            Vector velocity = new Vector();
            velocity.setX(direction.getX() * type.getForwardVelocity());
            velocity.setZ(direction.getZ() * type.getForwardVelocity());
            velocity.setY(type.getUpwardVelocity());
            
            player.setVelocity(velocity);
        }
        player.setFallDistance(0);
    }

    private void launchPlayerToTarget(Player player, Location target) {
        Location startLoc = player.getLocation();
        
        double deltaX = target.getX() - startLoc.getX();
        double deltaY = target.getY() - startLoc.getY();
        double deltaZ = target.getZ() - startLoc.getZ();
        
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        double gravity = 0.08;
        double angle = Math.toRadians(45);
        
        double velocity = Math.sqrt((horizontalDistance * gravity) / Math.sin(2 * angle));
        
        double velocityX = (deltaX / horizontalDistance) * velocity * Math.cos(angle);
        double velocityZ = (deltaZ / horizontalDistance) * velocity * Math.cos(angle);
        double velocityY = velocity * Math.sin(angle);
        
        if (deltaY > 0) {
            velocityY += Math.sqrt(2 * gravity * deltaY) * 0.5;
        }
        
        Vector launchVector = new Vector(velocityX, velocityY, velocityZ);
        player.setVelocity(launchVector);
    }

    public boolean isLaunchpad(Block block) {
        String typeId = placedLaunchpads.get(locationKey(block.getLocation()));
        if (typeId == null)
            return false;
        LaunchpadType type = plugin.getConfigManager().getLaunchpadType(typeId);
        if (type == null)
            return false;
        return block.getType() == type.getBlockType();
    }

    private String locationKey(Location location) {
        if (location == null || location.getWorld() == null)
            return "null";
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":"
                + location.getBlockZ();
    }

    public boolean isLaunchpadItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(launchpadTypeKey, PersistentDataType.STRING);
    }

    public String getLaunchpadTypeFromItem(ItemStack item) {
        if (!isLaunchpadItem(item))
            return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(launchpadTypeKey, PersistentDataType.STRING);
    }


    public String getLaunchpadType(Location location) {
        return placedLaunchpads.get(locationKey(location));
    }

    public boolean isOnCooldown(Player player, LaunchpadType type) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> cooldowns = playerCooldowns.getOrDefault(uuid, Collections.emptyMap());
        Long lastUsed = cooldowns.get(type.getId());
        if (lastUsed == null)
            return false;
        long cooldownTime = type.getCooldown() * 1000L;
        return System.currentTimeMillis() - lastUsed < cooldownTime;
    }

    public int getRemainingCooldown(Player player, LaunchpadType type) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> cooldowns = playerCooldowns.getOrDefault(uuid, Collections.emptyMap());
        Long lastUsed = cooldowns.get(type.getId());
        if (lastUsed == null)
            return 0;
        long cooldownTime = type.getCooldown() * 1000L;
        long timeLeft = cooldownTime - (System.currentTimeMillis() - lastUsed);
        return Math.max(0, (int) Math.ceil(timeLeft / 1000.0));
    }

    public void setOnCooldown(Player player, LaunchpadType type) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> cooldowns = playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        cooldowns.put(type.getId(), System.currentTimeMillis());
    }

    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            if (config.contains("placed-launchpads")) {
                for (String key : config.getConfigurationSection("placed-launchpads").getKeys(false)) {
                    placedLaunchpads.put(key, config.getString("placed-launchpads." + key));
                }
            }
            if (config.contains("synced-launchpads")) {
                for (String key : config.getConfigurationSection("synced-launchpads").getKeys(false)) {
                    String locationStr = config.getString("synced-launchpads." + key);
                    Location target = parseLocation(locationStr);
                    if (target != null) {
                        syncedLaunchpads.put(key, target);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load launchpad data: " + e.getMessage());
        }
    }

    public void saveData() {
        try {
            FileConfiguration config = new YamlConfiguration();
            
            if (!placedLaunchpads.isEmpty()) {
                for (Map.Entry<String, String> entry : placedLaunchpads.entrySet()) {
                    config.set("placed-launchpads." + entry.getKey(), entry.getValue());
                }
            }
            
            if (!syncedLaunchpads.isEmpty()) {
                for (Map.Entry<String, Location> entry : syncedLaunchpads.entrySet()) {
                    config.set("synced-launchpads." + entry.getKey(), locationToString(entry.getValue()));
                }
            }
            
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save launchpad data: " + e.getMessage());
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }

    private Location parseLocation(String str) {
        try {
            String[] parts = str.split(":");
            if (parts.length != 4) return null;
            
            return new Location(
                plugin.getServer().getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
            );
        } catch (Exception e) {
            return null;
        }
    }

    public void registerPlacedLaunchpad(Location location, String typeId) {
        placedLaunchpads.put(locationKey(location), typeId);
        saveData();
    }

    public void removePlacedLaunchpad(Location location) {
        String key = locationKey(location);
        placedLaunchpads.remove(key);
        syncedLaunchpads.remove(key);
        saveData();
    }

    public void createSyncedLaunchpad(Location start, Location end, String typeId) {
        String startKey = locationKey(start);
        placedLaunchpads.put(startKey, typeId);
        syncedLaunchpads.put(startKey, end.clone().add(0.5, 1, 0.5));
        saveData();
    }

    public boolean isSyncedLaunchpad(Location location) {
        return syncedLaunchpads.containsKey(locationKey(location));
    }
}
