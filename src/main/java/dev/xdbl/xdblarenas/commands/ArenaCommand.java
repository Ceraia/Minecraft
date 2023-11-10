package dev.xdbl.xdblarenas.commands;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.arenas.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArenaCommand implements CommandExecutor {

    private final XDBLArena plugin;
    private final Map<String, Arena> creatingArenas = new HashMap<>();

    public ArenaCommand(XDBLArena plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            arenaHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            arenaList(sender);
            return true;
        } else if (args[0].equalsIgnoreCase("delete")) {
            arenaDelete(sender, args);
            return true;
        } else if (args[0].equalsIgnoreCase("create")) {
            arenaCreate(sender, args);
            return true;
        } else if (args[0].equalsIgnoreCase("sp1")) {
            arenaSP1(sender, args);
            return true;
        } else if (args[0].equalsIgnoreCase("sp2")) {
            arenaSP2(sender, args);
            return true;
        } else if (args[0].equalsIgnoreCase("public")) {
            arenaPublic(sender, args);
            return true;
        } else {
            badUsage(sender);
            arenaHelp(sender);
            return true;
        }
    }

    private void arenaSP1(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];

        Arena arena = creatingArenas.get(name);
        if (arena == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp1.not_found").replace("&", "§")
            );
            return;
        }
        if (arena.getOwner() != sender.getName()) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp1.not_yours").replace("&", "§")
            );
            return;
        }
        arena.setSpawnPoint1(((Player) sender).getLocation());
        sender.sendMessage(
                plugin.getConfig().getString("messages.arena.sp1.success").replace("&", "§")
        );
    }

    private void arenaSP2(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];
        Arena arena = creatingArenas.get(name);

        if (arena == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp2.not_found").replace("&", "§")
            );
            return;
        }
        if (arena.getOwner() != sender.getName()) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp2.not_yours").replace("&", "§")
            );
            return;
        }

        boolean ready = arena.setSpawnPoint2(((Player) sender).getLocation());
        arena.setReady(ready);
        if (!ready) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp2.error").replace("&", "§")
            );
            return;
        }

        plugin.getArenaManager().addArena(arena);

        sender.sendMessage(
                plugin.getConfig().getString("messages.arena.sp2.success").replace("&", "§")
        );
    }

    private void arenaCreate(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];
        if (plugin.getArenaManager().getArenas().stream().filter(a -> a.getName().equalsIgnoreCase(name)).count() > 0) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.create.exists").replace("&", "§")
            );
            return;
        }

        Arena arena = new Arena(plugin, name, sender.getName());
        creatingArenas.put(name, arena);

        sender.sendMessage(
                plugin.getConfig().getString("messages.arena.create.success").replace("&", "§")
        );
    }

    private void arenaDelete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().getArenas().stream().filter(a -> a.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (arena == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.delete.not_found").replace("&", "§")
            );
            return;
        }
        if (!arena.getOwner().equals(sender.getName())) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.delete.not_yours").replace("&", "§")
            );
            return;
        }
        if (arena.getState() != Arena.ArenaState.WAITING) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.delete.running").replace("&", "§")
            );
            return;
        }

        arena.delete();
        plugin.getArenaManager().removeArena(arena);

        sender.sendMessage(
                plugin.getConfig().getString("messages.arena.delete.delete").replace("&", "§")
        );
    }

    private void arenaPublic(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().getArenas().stream().filter(a -> a.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (arena == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.public_command.not_found").replace("&", "§")
            );
            return;
        }
        if (!arena.getOwner().equals(sender.getName())) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.public_command.not_yours").replace("&", "§")
            );
            return;
        }

        boolean isPublic = arena.isPublic();

        if (isPublic) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.public_command.success_private").replace("&", "§")
            );
        } else {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.public_command.success_public").replace("&", "§")
            );
        }

        arena.setPublic(!isPublic);
    }

    private void badUsage(CommandSender sender) {
        sender.sendMessage(plugin.getConfig().getString("messages.bad_usage").replace("&", "§"));
    }

    private void arenaList(CommandSender sender) {
        List<Arena> arenas = plugin.getArenaManager().getArenas().stream().filter(arena -> arena.getOwner().equals(sender.getName())).collect(Collectors.toList());
        plugin.getConfig().getStringList("messages.arena.list").forEach(s -> {
            if (s.contains("%arenas")) {
                arenas.forEach(a -> {
                    sender.sendMessage(s.replace("%arenas", a.getName()).replace("&", "§") +
                            (a.getSpawnPoint1() != null ? " §8(" + a.getSpawnPoint1().getBlockX() + ", " + a.getSpawnPoint1().getBlockY() + ", " + a.getSpawnPoint1().getBlockZ() + ")" : ""));
                });
            } else {
                sender.sendMessage(s.replace("&", "§"));
            }
        });
    }

    private void arenaHelp(CommandSender sender) {
        plugin.getConfig().getStringList("messages.arena.help").forEach(s -> {
            sender.sendMessage(s.replace("&", "§"));
        });
    }
}
