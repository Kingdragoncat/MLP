package net.mythofy.mythofyLaunchPads.managers;

import net.mythofy.mythofyLaunchPads.MythofyLaunchPads;
import net.mythofy.mythofyLaunchPads.config.LaunchpadType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LaunchpadManager {

    private final MythofyLaunchPads plugin;
    private final NamespacedKey launchpadTypeKey;
    private final Map<Location, String> placedLaunchpads;
    private final Map<UUID, Map<String, Long>> playerCooldowns;

    public LaunchpadManager(MythofyLaunchPads plugin) {
        this.plugin = plugin;
        this.launchpadTypeKey = new NamespacedKey(plugin, "launchpad_type");
        this.placedLaunchpads = new HashMap<>();
        this.playerCooldowns = new HashMap<>();
    }

    public ItemStack createLaunchpadItem(LaunchpadType type) {
        ItemStack item = new ItemStack(type.getBlockType());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(type.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Place this launchpad to create a jump boost!");
        lore.add(ChatColor.GRAY + "Upward Velocity: " + ChatColor.YELLOW + type.getUpwardVelocity());
        lore.add(ChatColor.GRAY + "Forward Velocity: " + ChatColor.YELLOW + type.getForwardVelocity());
        lore.add(ChatColor.GRAY + "Cooldown: " + ChatColor.YELLOW + type.getCooldown() + "s");
        meta.setLore(lore);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(launchpadTypeKey, PersistentDataType.STRING, type.getId());

        item.setItemMeta(meta);
        return item;
    }

    public void registerPlacedLaunchpad(Location location, String typeId) {
        placedLaunchpads.put(location, typeId);
    }

    public void removePlacedLaunchpad(Location location) {
        placedLaunchpads.remove(location);
    }

    public String getLaunchpadType(Location location) {
        return placedLaunchpads.get(location);
    }

    public boolean isLaunchpad(Block block) {
        String typeId = placedLaunchpads.get(block.getLocation());
        if (typeId == null) return false;
        LaunchpadType type = plugin.getConfigManager().getLaunchpadType(typeId);
        if (type == null) return false;
        return block.getType() == type.getBlockType();
    }

    public boolean isLaunchpadItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(launchpadTypeKey, PersistentDataType.STRING);
    }

    public String getLaunchpadTypeFromItem(ItemStack item) {
        if (!isLaunchpadItem(item)) return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.get(launchpadTypeKey, PersistentDataType.STRING);
    }

    public boolean isOnCooldown(Player player, LaunchpadType type) {
        UUID uuid = player.getUniqueId();
        if (!playerCooldowns.containsKey(uuid)) return false;

        Map<String, Long> cooldowns = playerCooldowns.get(uuid);
        if (!cooldowns.containsKey(type.getId())) return false;

        long lastUsed = cooldowns.get(type.getId());
        long cooldownTime = type.getCooldown() * 1000L;

        return System.currentTimeMillis() - lastUsed < cooldownTime;
    }

    public int getRemainingCooldown(Player player, LaunchpadType type) {
        UUID uuid = player.getUniqueId();
        if (!playerCooldowns.containsKey(uuid)) return 0;

        Map<String, Long> cooldowns = playerCooldowns.get(uuid);
        if (!cooldowns.containsKey(type.getId())) return 0;

        long lastUsed = cooldowns.get(type.getId());
        long cooldownTime = type.getCooldown() * 1000L;
        long timeLeft = cooldownTime - (System.currentTimeMillis() - lastUsed);

        return Math.max(0, (int) Math.ceil(timeLeft / 1000.0));
    }

    public void setOnCooldown(Player player, LaunchpadType type) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> cooldowns = playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        cooldowns.put(type.getId(), System.currentTimeMillis());
    }
}
