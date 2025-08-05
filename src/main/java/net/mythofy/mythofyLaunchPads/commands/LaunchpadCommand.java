package net.mythofy.mythofyLaunchPads.commands;

import net.mythofy.mythofyLaunchPads.MythofyLaunchPads;
import net.mythofy.mythofyLaunchPads.config.LaunchpadType;
import net.mythofy.mythofyLaunchPads.managers.LaunchpadManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LaunchpadCommand implements CommandExecutor, TabCompleter {

    private final LaunchpadManager launchpadManager;
    private final MythofyLaunchPads plugin;

    public LaunchpadCommand(MythofyLaunchPads plugin) {
        this.plugin = plugin;
        this.launchpadManager = plugin.getLaunchpadManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    return handleReload(sender);
                case "help":
                    sendHelpMessage(sender);
                    return true;
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mythofylaunchpads.command")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sendHelpMessage(sender);
            return true;
        }

        return handleGiveLaunchpad(player, args);
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("mythofylaunchpads.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.reloadConfig();
        plugin.getConfigManager().reloadConfig();
        sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                "§aConfiguration reloaded successfully!");
        return true;
    }

    private boolean handleGiveLaunchpad(Player player, String[] args) {
        String typeId = args[0].toLowerCase();
        LaunchpadType type = plugin.getConfigManager().getLaunchpadType(typeId);

        if (type == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-launchpad"));
            return true;
        }

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                amount = Math.max(1, Math.min(64, amount));
            } catch (NumberFormatException ignored) {
                // Use default amount of 1
            }
        }

        Player target = player;
        if (args.length >= 3 && player.hasPermission("mythofylaunchpads.admin")) {
            target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§cPlayer not found!");
                return true;
            }
        }

        giveLaunchpad(player, target, type, amount);
        return true;
    }

    private void giveLaunchpad(Player player, Player target, LaunchpadType type, int amount) {
        ItemStack launchpadItem = new ItemStack(type.getBlockType(), amount);
        ItemMeta meta = launchpadItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getName());
            launchpadItem.setItemMeta(meta);
        }
        target.getInventory().addItem(launchpadItem);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", type.getName());

        target.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                plugin.getConfigManager().getMessage("launchpad-given", placeholders));

        if (target != player) {
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                    "§aGave " + target.getName() + " " + amount + " " + type.getName() + "§a!");
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        List<String> help = new ArrayList<>();
        String types = String.join(", ", plugin.getConfigManager().getLaunchpadTypes().keySet());
        help.add("&dMythofy LaunchPads Help:");
        help.add("&e/launchpad give <type> &7- Get a launchpad item of the specified type.");
        help.add("&e/launchpad types &7- List all available launchpad types.");
        help.add("&e/setsync endpoint &7- Link a synced launchpad to an endpoint block.");
        help.add("&e/launchpad reload &7- Reload the plugin config.");
        help.add("&e/syncwand &7- Get the Sync Wand for selecting synced launchpads.");
        help.add("&dHow to use synced launchpads:");
        help.add("&7 1. Use /syncwand to get the wand.");
        help.add("&7 2. Right-click a synced launchpad with the wand to select it.");
        help.add("&7 3. Look at the destination block and run /setsync endpoint.");
        for (String line : help) {
            String coloredLine = org.bukkit.ChatColor.translateAlternateColorCodes('&', line.replace("%types%", types));
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + coloredLine);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return getFirstArgCompletions(sender, args[0]);
        }
        if (args.length == 2) {
            return getSecondArgCompletions(args[0], args[1]);
        }
        if (args.length == 3) {
            return getThirdArgCompletions(sender, args[0], args[2]);
        }
        return List.of();
    }

    private List<String> getFirstArgCompletions(CommandSender sender, String partial) {
        List<String> commands = new ArrayList<>(plugin.getConfigManager().getLaunchpadTypes().keySet());
        commands.add("help");
        if (sender.hasPermission("mythofylaunchpads.reload")) {
            commands.add("reload");
        }
        return commands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getSecondArgCompletions(String command, String partial) {
        if (command.equalsIgnoreCase("reload") || command.equalsIgnoreCase("help")) {
            return List.of();
        }
        List<String> amounts = List.of("1", "5", "10", "32", "64");
        return amounts.stream()
                .filter(amount -> amount.startsWith(partial))
                .collect(Collectors.toList());
    }

    private List<String> getThirdArgCompletions(CommandSender sender, String command, String partial) {
        if (command.equalsIgnoreCase("reload") || command.equalsIgnoreCase("help")
                || !sender.hasPermission("mythofylaunchpads.admin")) {
            return List.of();
        }
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }
}
