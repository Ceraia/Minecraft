package dev.xdbl.commands.arena;

import dev.xdbl.Double;
import dev.xdbl.types.Arena;
import dev.xdbl.types.DoublePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandArena implements CommandExecutor, TabCompleter {

    private final Double plugin;

    public CommandArena(Double plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(this.plugin.getCommand("arena")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("xdbl.arena")) {
            this.plugin.noPermission((Player) sender);
            return true;
        }

        if (args.length == 0) {
            arenaHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            arenaList(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("scoreboard") || args[0].equalsIgnoreCase("top")) {
            Player p = (Player) sender;

            // Create and show a string list of the top 10 players with the highest elo
            List<Component> top = new ArrayList<>();
            AtomicInteger i = new AtomicInteger();
            i.set(1);

            top.add(MiniMessage.miniMessage().deserialize("<yellow><bold>Top 10 players with the highest ELO:"));

            plugin.getPlayerManager().getDoublePlayers().stream().sorted(Comparator.comparingInt(DoublePlayer::getElo).reversed()).limit(10).forEach(ap -> {
                String playerName = Bukkit.getOfflinePlayer(ap.getUUID()).getName();
                int elo = ap.getElo();

                String medal; // Default medal color for players outside the top 3

                // Check for 1st, 2nd, and 3rd place
                if (i.get() == 1) {
                    medal = "<gold>"; // Gold for 1st place
                } else if (i.get() == 2) {
                    medal = "<#C0C0C0>"; // Silver for 2nd place
                } else if (i.get() == 3) {
                    medal = "<#cd7f32>"; // Bronze for 3rd place
                } else {
                    medal = "<white>"; // Default medal color for players outside the top 3
                }

                top.add(MiniMessage.miniMessage().deserialize(medal + i + " " + playerName + " <dark_gray>- <gray>" + elo + " ELO (" + (ap.wins() + ap.losses()) + " games)"));
                i.getAndIncrement();
            });

            // Send the top 10 players with the highest elo to the player
            top.forEach(p::sendMessage);

            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            arenaDelete(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) {
            arenaCreate(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("sp1")) {
            arenaSP1(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("sp2")) {
            arenaSP2(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("public")) {
            arenaPublic(sender, args);
            return true;
        } else {
            plugin.badUsage((Player) sender);
            arenaHelp(sender);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> arenas = new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("list", "delete", "public", "create", "sp1", "sp2", "top", "scoreboard");
        } else if (args.length == 2 && (
                args[0].equalsIgnoreCase("delete") ||
                        args[0].equalsIgnoreCase("public") ||
                        args[0].equalsIgnoreCase("sp1") ||
                        args[0].equalsIgnoreCase("sp2"))) {
            plugin.getArenaManager().getArenas().forEach(a -> {
                if (a.getOwner().equals(sender.getName())) {
                    arenas.add(a.getName());
                }
            });
            return arenas;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            return List.of("<name>");
        } else if ((args.length == 3 && args[0].equalsIgnoreCase("public"))) {
            return Arrays.asList("true", "false");
        }
        return new ArrayList<>();
    }

    private void arenaSP1(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];

        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            notFound(sender);
            return;
        }
        if (!Objects.equals(arena.getOwner(), sender.getName())) {
            notYours(sender);
            return;
        }
        arena.setSpawnPoint1(((Player) sender).getLocation());
        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Spawn point 1 set")
        );
    }

    private void arenaSP2(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];

        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            notFound(sender);
            return;
        }

        if (!Objects.equals(arena.getOwner(), sender.getName())) {
            notYours(sender);
            return;
        }
        arena.setSpawnPoint2(((Player) sender).getLocation());
        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Spawn point 2 set")
        );
    }

    private void arenaCreate(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];

        // Check if the user is banned from creating arenas
        if (plugin.getPlayerManager().getDoublePlayer(((Player) sender).getUniqueId()).arenaBanned()) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>You are banned from creating arenas")
            );
            return;
        }

        // Check if the string is the same as <name>, if so state the user should put a name
        if (name.equalsIgnoreCase("<name>")) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Invalid arena name")
            );
            return;
        }
        // Check if the string is alphanumeric
        if (!name.matches("[a-zA-Z0-9]*")) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Invalid arena name, it may only contain letters and numbers")
            );
            return;
        }

        // Check if the arena already exists
        if (plugin.getArenaManager().getArenas().stream().filter(a -> a.getName().equalsIgnoreCase(name)).count() > 0) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Arena already exists")
            );
            return;
        }

        File file = new File(plugin.getDataFolder(), "data/arenas/" + name + ".yml");

        Arena arena = new Arena(plugin, name, sender.getName(), ((Player) sender).getLocation(), ((Player) sender).getLocation(), false, false, file);

        arena.setSpawnPoint1(((Player) sender).getLocation());
        arena.setSpawnPoint2(((Player) sender).getLocation());

        plugin.getArenaManager().addArena(arena);

        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Arena created")
        );
    }

    private void arenaDelete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            notFound(sender);
            return;
        }
        if (!arena.getOwner().equals(sender.getName())) {
            notYours(sender);
            return;
        }
        if (arena.getState() != Arena.ArenaState.WAITING) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Arena is not empty")
            );
            return;
        }

        arena.delete();
        plugin.getArenaManager().removeArena(arena);

        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Arena deleted")
        );
    }

    private void arenaPublic(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            notFound(sender);
            return;
        }
        if (!arena.getOwner().equals(sender.getName())) {
            notYours(sender);
            return;
        }

        boolean isPublic = arena.isPublic();

        if (isPublic) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<green>Arena made private")
            );
        } else {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<green>Arena made public")
            );
        }

        arena.setPublic(!isPublic);
    }

    private void arenaList(CommandSender sender) {
        List<Arena> arenas = plugin.getArenaManager().getArenas().stream().filter(arena -> arena.getOwner().equals(sender.getName())).toList();
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow><bold>Your arenas:" + arenas.stream().map(arena -> "\n<green>" + arena.getName()).collect(Collectors.joining())));
    }

    private void arenaHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                """
                        <yellow><bold>Arena Help
                        <green>/arena list - Show your arenas
                        <green>/arena delete <name> - Delete your arena
                        <green>/arena public <name> - Make arena public/private
                        <yellow>How to create a new arena:
                        <gold>1. <green>/arena create <name> - Create a new arena
                        <gold>2. <green>/arena sp1 <name> - Set spawn point for the first player
                        <gold>3. <green>/arena sp2 <name> - Set spawn point for the second player
                        """
        ));
    }

    private void notYours(CommandSender sender) {
        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<red>Arena is not yours.")
        );
    }

    private void notFound(CommandSender sender) {
        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<red>Arena not found.")
        );
    }
}
