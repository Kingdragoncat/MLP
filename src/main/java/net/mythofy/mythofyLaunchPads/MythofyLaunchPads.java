package net.mythofy.mythofyLaunchPads;

import net.mythofy.mythofyLaunchPads.commands.LaunchpadCommand;
import net.mythofy.mythofyLaunchPads.commands.SyncWandCommand;
import net.mythofy.mythofyLaunchPads.commands.SetSyncCommand;
import net.mythofy.mythofyLaunchPads.config.ConfigManager;
import net.mythofy.mythofyLaunchPads.listeners.LaunchpadListerner;
import net.mythofy.mythofyLaunchPads.managers.LaunchpadManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

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

        // Initialize commands
        SyncWandCommand syncWandCommand = new SyncWandCommand(this);
        SetSyncCommand setSyncCommand = new SetSyncCommand(this);
        LaunchpadCommand launchpadCommand = new LaunchpadCommand(this);

        // Register events
        LaunchpadListerner listener = new LaunchpadListerner(this);
        listener.setSyncWandCommand(syncWandCommand);
        listener.setSetSyncCommand(setSyncCommand);
        getServer().getPluginManager().registerEvents(listener, this);

        // Register commands
        getCommand("launchpad").setExecutor(launchpadCommand);
        getCommand("launchpad").setTabCompleter(launchpadCommand);
        getCommand("syncwand").setExecutor(syncWandCommand);
        getCommand("setsync").setExecutor(setSyncCommand);
        getCommand("setsync").setTabCompleter(setSyncCommand);

        // Initialize bStats metrics
        new Metrics(this, 26793);

        getLogger().info("MythofyLaunchPads has been enabled!");
    }

    @Override
    public void onDisable() {
        if (launchpadManager != null) {
            launchpadManager.saveData();
        }
        getLogger().info("MythofyLaunchPads has been disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LaunchpadManager getLaunchpadManager() {
        return launchpadManager;
    }
}
