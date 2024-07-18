package dev.xdbl.modules;

import dev.xdbl.Double;
import dev.xdbl.types.DoublePlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModuleSystem implements CommandExecutor, TabCompleter, Listener {

    private final Double plugin;

    public ModuleSystem(Double plugin) {
        this.plugin = plugin;

        // Register the commands
        Objects.requireNonNull(plugin.getCommand("jump")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("mod")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("version")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("day")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("night")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("discord")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch (command.getName().toLowerCase()) {
            case "jump" -> {
                if (sender instanceof Player) {
                    jump((Player) sender);
                }
                return true;
            }
            case "j" -> {
                if (sender instanceof Player) {
                    jump((Player) sender);
                }
            }
            case "mod" -> {
                if (!sender.hasPermission("xdbl.mod")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }

                if (args.length == 0) {
                    ModHelp(sender);
                    return true;
                }

                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("ban")) {
                        ModHelp(sender);
                        return true;
                    }
                }

                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("ban")) {
                        if (args[1].equalsIgnoreCase("pvp")) {
                            ModHelp(sender);
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("arena")) {
                            ModHelp(sender);
                            return true;
                        }
                    }
                }

                if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("ban")) {
                        Player target = Bukkit.getPlayer(args[2]);

                        if (args[1].equalsIgnoreCase("arena")) {
                            if (target == null) {
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found"));
                                return true;
                            }
                            DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(target.getUniqueId());
                            boolean arenabanned = doublePlayer.arenaBan();
                            if (arenabanned) {
                                doublePlayer.addLog("Banned from creation of arenas by " + sender.getName());
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Banned from creation of arenas by " + sender.getName()));
                            } else {
                                doublePlayer.addLog("Unbanned from creation of arenas by " + sender.getName());
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Unbanned from creation of arenas by " + sender.getName()));
                            }
                        }
                        if (args[1].equalsIgnoreCase("pvp")) {
                            if (target == null) {
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found"));
                                return true;
                            }
                            DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(target.getUniqueId());
                            boolean pvpbanned = doublePlayer.pvpBan();
                            if (pvpbanned) {
                                doublePlayer.addLog("Banned from PVPing by " + sender.getName());
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Banned from PVPing by " + sender.getName()));
                            } else {
                                doublePlayer.addLog("Unbanned from PVPing by " + sender.getName());
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Unbanned from PVPing by " + sender.getName()));
                            }
                        }
                    }
                }

                return true;
            }
            case "version" -> {
                sender.sendMessage(
                        MiniMessage.miniMessage().deserialize("<green>Running <white>Double <green>v" + plugin.getPluginMeta().getVersion() + " <green>by <white>Axodouble")
                );
                return true;
            }
            case "day" -> {
                if (!sender.hasPermission("xdbl.time")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }
                if (sender instanceof Player) {
                    ((Player) sender).getWorld().setTime(0);
                }
                return true;
            }
            case "night" -> {
                if (!sender.hasPermission("xdbl.time")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }
                if (sender instanceof Player) {
                    ((Player) sender).getWorld().setTime(13000);
                }
                return true;
            }
            case "discord" -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Click <white><click:copy_to_clipboard:"+ plugin.getConfig().getString("DISCORD_INVITE", "EXAMPLE_INVITE") +"><hover:show_text:"+plugin.getConfig().getString("DISCORD_INVITE", "EXAMPLE_INVITE")+">this</click></white> to copy the Discord link, or click here: <white>"+ plugin.getConfig().getString("DISCORD_INVITE", "EXAMPLE_INVITE")));
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch (command.getName().toLowerCase()) {
            case "mod" -> {
                if (args.length == 1) {
                    List<String> tabOptions = new ArrayList<>();
                    tabOptions.add("ban");
                    tabOptions.add("remove");
                    return tabOptions;
                }
                if (args.length == 2) {
                    List<String> tabOptions = new ArrayList<>();
                    if (args[1].equalsIgnoreCase("ban")) {
                        tabOptions.add("pvp");
                    }
                    tabOptions.add("arena");
                    return tabOptions;
                }
                if (args.length == 3) {
                    if (args[2].equalsIgnoreCase("pvp")) {
                        List<String> tabOptions = new ArrayList<>();
                        // If there is an argument, suggest online player names
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            tabOptions.add(player.getName());
                        }
                        return tabOptions;
                    }
                    if (args[2].equalsIgnoreCase("arena")) {
                        List<String> tabOptions = new ArrayList<>();
                        // If there is an argument, suggest all arena names
                        plugin.getArenaManager().getArenas().forEach(arena -> tabOptions.add(arena.getName()));
                        return tabOptions;
                    }
                }
                // If there is more than one argument, return an empty list
                return new ArrayList<>();
            }

        }
        return null;
    }

    public void jump(Player player) {
        if (!player.hasPermission("xdbl.jump")) {
            this.plugin.noPermission(player);
        }
        // Get the block the player is looking at even if it is out of reach
        Block block = player.getTargetBlockExact(1000);

        // Teleport the player on top of the block if it is not null
        if (block != null) {
            // Check if there is air above the block
            if (block.getRelative(0, 1, 0).isEmpty()) {
                player.teleport(block.getLocation().add(0.5, 1, 0.5));
            } else {
                // Get the highest block above the block
                Block highestBlock = block.getRelative(0, 1, 0);
                while (highestBlock.getRelative(0, 1, 0).getType() != Material.AIR) {
                    highestBlock = highestBlock.getRelative(0, 1, 0);
                }
                player.teleport(highestBlock.getLocation().add(0.5, 1, 0.5));
            }
        }
    }

    private void ModHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                """
                        <yellow><bold>Mod Help
                        <gray>/mod ban pvp <player>
                        <gray>/mod ban arena <player>
                        <gray>/mod remove pvp <player>
                        <gray>/mod remove arena <player>"""
        ));
    }
}
