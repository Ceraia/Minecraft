package com.ceraia.arena.commands.system;

import com.ceraia.Double;
import com.ceraia.arena.types.DoublePlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandMod implements CommandExecutor, TabCompleter {

    private final Double plugin;

    public CommandMod(Double plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("xdbl.mod")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.no_permission"))));
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
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.player_not_found"))));
                        return true;
                    }
                    DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(target.getUniqueId());
                    boolean arenabanned = doublePlayer.arenaBan();
                    if (arenabanned) {
                        doublePlayer.addLog("Banned from creation of arenas by " + sender.getName());
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.mod.ban.arena.banned")).replace("%player%", target.getName())));
                    } else {
                        doublePlayer.addLog("Unbanned from creation of arenas by " + sender.getName());
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.mod.ban.arena.unbanned")).replace("%player%", target.getName())));
                    }
                }
                if (args[1].equalsIgnoreCase("pvp")) {
                    if (target == null) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.player_not_found"))));
                        return true;
                    }
                    DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(target.getUniqueId());
                    boolean pvpbanned = doublePlayer.pvpBan();
                    if (pvpbanned) {
                        doublePlayer.addLog("Banned from PVPing by " + sender.getName());
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.mod.ban.pvp.banned")).replace("%player%", target.getName())));
                    } else {
                        doublePlayer.addLog("Unbanned from PVPing by " + sender.getName());
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.mod.ban.pvp.unbanned")).replace("%player%", target.getName())));
                    }
                }
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> tabOptions = new ArrayList<>();
            tabOptions.add("ban");
            tabOptions.add("remove");
            return tabOptions;
        }
        if (args.length == 2) {
            List<String> tabOptions = new ArrayList<>();
            if (args[1].equalsIgnoreCase("ban")) tabOptions.add("pvp");
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

    private void ModHelp(CommandSender sender) {
        plugin.getConfig().getStringList("messages.mod.help").forEach(s -> sender.sendMessage(MiniMessage.miniMessage().deserialize(s)));
    }
}
