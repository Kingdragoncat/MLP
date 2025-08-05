
package net.mythofy.mythofyLaunchPads.config;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

public class ConfigManager {
    public List<String> getMessageList(String path) {
        List<String> list = plugin.getConfig().getStringList("messages." + path);
        if (list == null || list.isEmpty()) {
            String fallback = plugin.getConfig().getString("messages." + path);
            if (fallback != null) {
                return List.of(fallback);
            }
            return List.of("&cMessage list not found: " + path);
        }
        return list;
    }

    private final JavaPlugin plugin;
    private final Map<String, LaunchpadType> launchpadTypes;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.launchpadTypes = new HashMap<>();
        loadLaunchpadTypes();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        launchpadTypes.clear();
        loadLaunchpadTypes();
    }

    private void loadLaunchpadTypes() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("launchpads");
        if (section == null) {
            plugin.getLogger().warning("No launchpads found in config!");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection launchpadSection = section.getConfigurationSection(key);
            if (launchpadSection == null)
                continue;

            String name = launchpadSection.getString("name", key);
            String blockTypeName = launchpadSection.getString("block-type", "STONE_PRESSURE_PLATE");
            double upwardVelocity = launchpadSection.getDouble("upward-velocity", 1.0);
            double forwardVelocity = launchpadSection.getDouble("forward-velocity", 1.0);
            int cooldown = launchpadSection.getInt("cooldown", 5);
            boolean synced = launchpadSection.getBoolean("synced", false);

            Material blockType;
            try {
                blockType = Material.valueOf(blockTypeName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid block type for launchpad " + key + ": " + blockTypeName);
                blockType = Material.STONE_PRESSURE_PLATE;
            }

            LaunchpadType launchpadType = new LaunchpadType(key, name, blockType, upwardVelocity, forwardVelocity,
                    cooldown, synced);
            launchpadTypes.put(key, launchpadType);
            plugin.getLogger().info("Loaded launchpad type: " + key);
        }

        // Add new synced launchpad type if not present in config
        if (!launchpadTypes.containsKey("synced")) {
            LaunchpadType syncedType = new LaunchpadType(
                    "synced",
                    "Synced Launchpad",
                    Material.END_PORTAL_FRAME, // or any block you want
                    1.0,
                    1.0,
                    5,
                    true);
            launchpadTypes.put("synced", syncedType);
            plugin.getLogger().info("Loaded default synced launchpad type.");
        }
    }

    public LaunchpadType getLaunchpadType(String key) {
        return launchpadTypes.get(key);
    }

    public Map<String, LaunchpadType> getLaunchpadTypes() {
        return launchpadTypes;
    }

    public Set<String> getLaunchpadTypeKeys() {
        return launchpadTypes.keySet();
    }

    public String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages." + path, "&cMessage not found: " + path));
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }
}
