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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LaunchpadCommand implements CommandExecutor, TabCompleter {

    private final MythofyLaunchPads plugin;
    private final LaunchpadManager launchpadManager;

    public LaunchpadCommand(MythofyLaunchPads plugin) {
        this.plugin = plugin;
        this.launchpadManager = plugin.getLaunchpadManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle reload command
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
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

        // Handle help command
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
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

        ItemStack launchpadItem = launchpadManager.createLaunchpadItem(type);
        launchpadItem.setAmount(amount);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", type.getName());

        target.getInventory().addItem(launchpadItem);
        target.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                plugin.getConfigManager().getMessage("launchpad-given", placeholders));

        if (target != player) {
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                    "§aGave " + target.getName() + " " + amount + " " + type.getName() + "§a!");
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§6=== MythofyLaunchPads Help ===");
        sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§6/launchpad <type> [amount] [player] §7- Get a launchpad");
        if (sender.hasPermission("mythofylaunchpads.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§6/launchpad reload §7- Reload the configuration");
        }
        sender.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                "§6Available types: §e" + String.join(", ", plugin.getConfigManager().getLaunchpadTypes().keySet()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> commands = new ArrayList<>(plugin.getConfigManager().getLaunchpadTypes().keySet());

            // Add special commands
            commands.add("help");
            if (sender.hasPermission("mythofylaunchpads.reload")) {
                commands.add("reload");
            }

            completions.addAll(commands.stream()
                    .filter(cmd -> cmd.startsWith(partial))
                    .collect(Collectors.toList()));
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("help")) {
            // Suggest some common amounts
            String partial = args[1].toLowerCase();
            List<String> amounts = List.of("1", "5", "10", "32", "64");
            completions.addAll(amounts.stream()
                    .filter(amount -> amount.startsWith(partial))
                    .collect(Collectors.toList()));
        } else if (args.length == 3 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("help")
                && sender.hasPermission("mythofylaunchpads.admin")) {
            // Suggest online players
            String partial = args[2].toLowerCase();
            completions.addAll(plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList()));
        }

        return completions;
    }
}