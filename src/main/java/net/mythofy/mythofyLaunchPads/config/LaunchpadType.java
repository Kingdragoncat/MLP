package net.mythofy.mythofyLaunchPads.config;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public class LaunchpadType {

    private final String id;
    private final String name;
    private final Material blockType;
    private final double upwardVelocity;
    private final double forwardVelocity;
    private final int cooldown;

    public LaunchpadType(String id, String name, Material blockType, double upwardVelocity, double forwardVelocity, int cooldown) {
        this.id = id;
        this.name = ChatColor.translateAlternateColorCodes('&', name);
        this.blockType = blockType;
        this.upwardVelocity = upwardVelocity;
        this.forwardVelocity = forwardVelocity;
        this.cooldown = cooldown;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Material getBlockType() {
        return blockType;
    }

    public double getUpwardVelocity() {
        return upwardVelocity;
    }

    public double getForwardVelocity() {
        return forwardVelocity;
    }

    public int getCooldown() {
        return cooldown;
    }
}
