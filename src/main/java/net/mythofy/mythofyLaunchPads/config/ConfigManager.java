package net.mythofy.mythofyLaunchPads.config;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

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
            if (launchpadSection == null) continue;

            String name = launchpadSection.getString("name", key);
            String blockTypeName = launchpadSection.getString("block-type", "STONE_PRESSURE_PLATE");
            double upwardVelocity = launchpadSection.getDouble("upward-velocity", 1.0);
            double forwardVelocity = launchpadSection.getDouble("forward-velocity", 1.0);
            int cooldown = launchpadSection.getInt("cooldown", 5);

            Material blockType;
            try {
                blockType = Material.valueOf(blockTypeName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid block type for launchpad " + key + ": " + blockTypeName);
                blockType = Material.STONE_PRESSURE_PLATE;
            }

            LaunchpadType launchpadType = new LaunchpadType(key, name, blockType, upwardVelocity, forwardVelocity, cooldown);
            launchpadTypes.put(key, launchpadType);
            plugin.getLogger().info("Loaded launchpad type: " + key);
        }
    }

    public LaunchpadType getLaunchpadType(String key) {
        return launchpadTypes.get(key);
    }

    public Map<String, LaunchpadType> getLaunchpadTypes() {
        return launchpadTypes;
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
