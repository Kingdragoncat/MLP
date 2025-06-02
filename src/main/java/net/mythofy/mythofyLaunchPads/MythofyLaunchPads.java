package net.mythofy.mythofyLaunchPads;

import net.mythofy.mythofyLaunchPads.commands.LaunchpadCommand;
import net.mythofy.mythofyLaunchPads.config.ConfigManager;
import net.mythofy.mythofyLaunchPads.listeners.LaunchpadListerner;
import net.mythofy.mythofyLaunchPads.managers.LaunchpadManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MythofyLaunchPads extends JavaPlugin {

    private ConfigManager configManager;
    private LaunchpadManager launchpadManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.launchpadManager = new LaunchpadManager(this);

        // Register events
        getServer().getPluginManager().registerEvents(new LaunchpadListerner(this), this);

        // Register commands
        getCommand("launchpad").setExecutor(new LaunchpadCommand(this));
        getCommand("launchpad").setTabCompleter(new LaunchpadCommand(this));

        getLogger().info("MythofyLaunchPads has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save any data if needed
        getLogger().info("MythofyLaunchPads has been disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LaunchpadManager getLaunchpadManager() {
        return launchpadManager;
    }
}