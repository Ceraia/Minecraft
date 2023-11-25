package dev.xdbl.xdblarenas.commands;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.types.ArenaPlayer;
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

    private final XDBLArena plugin;

    public CommandMod(XDBLArena plugin) {
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
                    ArenaPlayer arenaPlayer = plugin.getPlayerManager().getArenaPlayer(target.getUniqueId());
                    boolean arenabanned = arenaPlayer.arenaBan();
                    if (arenabanned) {
                        arenaPlayer.addLog("Banned from creation of arenas by " + sender.getName());
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.mod.ban.arena.banned")).replace("%player%", target.getName())));
                    } else {
                        arenaPlayer.addLog("Unbanned from creation of arenas by " + sender.getName());
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.mod.ban.arena.unbanned")).replace("%player%", target.getName())));
                    }
                }
                if (args[1].equalsIgnoreCase("pvp")) {
                    if (target == null) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.player_not_found"))));
                        return true;
                    }
                    ArenaPlayer arenaPlayer = plugin.getPlayerManager().getArenaPlayer(target.getUniqueId());
                    boolean pvpbanned = arenaPlayer.pvpBan();
                    if (pvpbanned) {
                        arenaPlayer.addLog("Banned from PVPing by " + sender.getName());
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.mod.ban.pvp.banned")).replace("%player%", target.getName())));
                    } else {
                        arenaPlayer.addLog("Unbanned from PVPing by " + sender.getName());
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
