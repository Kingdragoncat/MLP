package net.mythofy.mythofyLaunchPads.commands;

import net.mythofy.mythofyLaunchPads.MythofyLaunchPads;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SyncWandCommand implements CommandExecutor {

    private final MythofyLaunchPads plugin;
    private final NamespacedKey syncWandKey;

    public SyncWandCommand(MythofyLaunchPads plugin) {
        this.plugin = plugin;
        this.syncWandKey = new NamespacedKey(plugin, "sync_wand");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only", null));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mythofylaunchpads.syncwand")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission", null));
            return true;
        }

        ItemStack wand = createSyncWand();
        player.getInventory().addItem(wand);
        player.sendMessage(ChatColor.GREEN + "You have received a Sync Wand! Left-click to set start point, right-click to set end point.");

        return true;
    }

    private ItemStack createSyncWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "Sync Wand");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Left-click: Set start point");
        lore.add(ChatColor.GRAY + "Right-click: Set end point");
        lore.add(ChatColor.YELLOW + "Use /setsync <type> to create synced launchpad");
        meta.setLore(lore);
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(syncWandKey, PersistentDataType.STRING, "sync_wand");
        
        wand.setItemMeta(meta);
        return wand;
    }

    public boolean isSyncWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(syncWandKey, PersistentDataType.STRING);
    }
}