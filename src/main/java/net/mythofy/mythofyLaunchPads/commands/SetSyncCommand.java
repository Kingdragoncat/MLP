package net.mythofy.mythofyLaunchPads.commands;

import net.mythofy.mythofyLaunchPads.MythofyLaunchPads;
import net.mythofy.mythofyLaunchPads.config.LaunchpadType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SetSyncCommand implements CommandExecutor, TabCompleter {

    private final MythofyLaunchPads plugin;
    private final Map<UUID, Location> startPoints = new HashMap<>();
    private final Map<UUID, Location> endPoints = new HashMap<>();

    public SetSyncCommand(MythofyLaunchPads plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only", null));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mythofylaunchpads.setsync")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission", null));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /setsync <type>");
            return true;
        }

        String typeId = args[0];
        LaunchpadType type = plugin.getConfigManager().getLaunchpadType(typeId);
        
        if (type == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-launchpad", null));
            return true;
        }

        UUID playerUUID = player.getUniqueId();
        Location start = startPoints.get(playerUUID);
        Location end = endPoints.get(playerUUID);

        if (start == null) {
            player.sendMessage(ChatColor.RED + "You must set a start point first! Use a sync wand to select points.");
            return true;
        }

        if (end == null) {
            player.sendMessage(ChatColor.RED + "You must set an end point first! Use a sync wand to select points.");
            return true;
        }

        if (!start.getWorld().equals(end.getWorld())) {
            player.sendMessage(ChatColor.RED + "Start and end points must be in the same world!");
            return true;
        }

        plugin.getLaunchpadManager().createSyncedLaunchpad(start, end, typeId);
        
        start.getBlock().setType(type.getBlockType());
        
        player.sendMessage(ChatColor.GREEN + "Synced launchpad created! Step on the " + 
                          type.getBlockType().name() + " to launch to your destination.");

        startPoints.remove(playerUUID);
        endPoints.remove(playerUUID);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            for (String typeId : plugin.getConfigManager().getLaunchpadTypeKeys()) {
                if (typeId.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(typeId);
                }
            }
        }
        
        return completions;
    }

    public void setStartPoint(UUID playerUUID, Location location) {
        startPoints.put(playerUUID, location);
    }

    public void setEndPoint(UUID playerUUID, Location location) {
        endPoints.put(playerUUID, location);
    }

    public Location getStartPoint(UUID playerUUID) {
        return startPoints.get(playerUUID);
    }

    public Location getEndPoint(UUID playerUUID) {
        return endPoints.get(playerUUID);
    }
}