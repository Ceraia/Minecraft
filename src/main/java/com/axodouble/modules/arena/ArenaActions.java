package com.axodouble.modules.arena;

import com.axodouble.Double;
import com.axodouble.types.DoublePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ArenaActions {
    private static Double plugin;
    public static final Map<Player, Player> invites = new HashMap<>();
    public static final Map<Player, Player> playersByGroup = new HashMap<>();
    public static final Map<Player, List<Player>> groups = new HashMap<>();

    public ArenaActions(Double plugin) {
        this.plugin = plugin;
    }

    public static void arenaSP1(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];

        Arena arena = plugin.getArenaModule().arenaManager.getArena(name);
        if (arena == null) {
            ArenaDefaultMessages.notFound(sender);
            return;
        }
        if (!Objects.equals(arena.getOwner(), sender.getName())) {
            ArenaDefaultMessages.notYours(sender);
            return;
        }
        arena.setSpawnPoint1(((Player) sender).getLocation());
        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Spawn point 1 set")
        );
    }

    public static void arenaSP2(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];

        Arena arena = plugin.getArenaModule().arenaManager.getArena(name);
        if (arena == null) {
            ArenaDefaultMessages.notFound(sender);
            return;
        }

        if (!Objects.equals(arena.getOwner(), sender.getName())) {
            ArenaDefaultMessages.notYours(sender);
            return;
        }
        arena.setSpawnPoint2(((Player) sender).getLocation());
        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Spawn point 2 set")
        );
    }

    public static void arenaCreate(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];

        // Check if the user is banned from creating arenas
        if (plugin.getPlayerManager().getDoublePlayer(((Player) sender).getUniqueId()).isArenaBanned()) {
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
        if (plugin.getArenaModule().arenaManager.getArenas().stream().anyMatch(a -> a.getName().equalsIgnoreCase(name))) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Arena already exists")
            );
            return;
        }

        File file = new File(plugin.getDataFolder(), "data/arenas/" + name + ".yml");

        Arena arena = new Arena(plugin, name, sender.getName(), ((Player) sender).getLocation(), ((Player) sender).getLocation(), false, file);

        arena.setSpawnPoint1(((Player) sender).getLocation());
        arena.setSpawnPoint2(((Player) sender).getLocation());

        plugin.getArenaModule().arenaManager.addArena(arena);

        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Arena created")
        );
    }

    public static void arenaDelete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaModule().arenaManager.getArena(name);
        if (arena == null) {
            ArenaDefaultMessages.notFound(sender);
            return;
        }
        if (!arena.getOwner().equals(sender.getName())) {
            ArenaDefaultMessages.notYours(sender);
            return;
        }
        if (arena.getState() != Arena.ArenaState.WAITING) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Arena is not empty")
            );
            return;
        }

        arena.delete();
        plugin.getArenaModule().arenaManager.removeArena(arena);

        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Arena deleted")
        );
    }

    public static void arenaPublic(CommandSender sender, String[] args) {
        if (args.length == 1) {
            plugin.badUsage((Player) sender);
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaModule().arenaManager.getArena(name);
        if (arena == null) {
            ArenaDefaultMessages.notFound(sender);
            return;
        }
        if (!arena.getOwner().equals(sender.getName())) {
            ArenaDefaultMessages.notYours(sender);
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

    public static void arenaList(CommandSender sender) {
        List<Arena> arenas = plugin.getArenaModule().arenaManager.getArenas().stream().filter(arena -> arena.getOwner().equals(sender.getName())).toList();
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow><bold>Your arenas:" + arenas.stream().map(arena -> "\n<green>" + arena.getName()).collect(Collectors.joining())));
    }

    public static List<Player> getPlayersByGroup(Player player) {
        return groups.getOrDefault(player, null);
    }

    public static void calculateElo(Player loser, Player winner) {
        UUID winnerUUID = winner.getUniqueId();
        UUID loserUUID = loser.getUniqueId();

        // Get the win chance
        int winChance = plugin.getPlayerManager().calculateWinChance(winnerUUID, loserUUID);

        Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<green>" + winner.getName() + " just killed " + loser.getName() + " in the " + plugin.getArenaModule().arenaManager.getArena(loser).getName() + " arena with a win chance of " + winChance + "%!"));

        // Handle ELO calculations
        plugin.getPlayerManager().playerKill(winnerUUID, loserUUID);

        updateScoreboard();
    }

    public static void leaveGang(Player player) {
        if (!playersByGroup.containsKey(player)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are not in a group"));
            return;
        }

        Player owner = playersByGroup.get(player);
        List<Player> group = groups.get(owner);

        if (group != null || group.size() <= 2) {
            group.forEach(pl -> {
                pl.sendMessage(MiniMessage.miniMessage().deserialize("<red>Group has been disbanded"));
                playersByGroup.remove(pl);
            });

            groups.remove(owner);
        } else {
            group.forEach(p -> player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + player.getName() + " has left the group")));

            group.remove(player);
            groups.put(owner, group);

            playersByGroup.remove(player);
        }
    }

    public static void updateScoreboard() {
        // Update scoreboard
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboardDefault = scoreboardManager.getNewScoreboard();
        Objective objectivePlayerList = scoreboardDefault.registerNewObjective("eloObjectivePlayerList", "dummy", MiniMessage.miniMessage().deserialize("Top Arena Players"));
        Objective objectiveBelowName = scoreboardDefault.registerNewObjective("eloObjectiveBelowName", "dummy", MiniMessage.miniMessage().deserialize("<green>ELO"));

        // Get all online players and set their score to their Elo rating
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(onlinePlayer.getUniqueId());

            objectivePlayerList.getScore(onlinePlayer.getName()).setScore(doublePlayer.getElo());
            objectiveBelowName.getScore(onlinePlayer.getName()).setScore(doublePlayer.getElo());
            objectivePlayerList.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            objectiveBelowName.setDisplaySlot(DisplaySlot.BELOW_NAME);

            onlinePlayer.setScoreboard(scoreboardDefault);
        }
    }

    public static void leaderboard(CommandSender sender) {
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

            top.add(MiniMessage.miniMessage().deserialize(medal + i + " " + playerName + " <dark_gray>- <gray>" + elo + " ELO (" + (ap.getWins() + ap.getLosses()) + " games)"));
            i.getAndIncrement();
        });

        // Send the top 10 players with the highest elo to the player
        top.forEach(p::sendMessage);
    }
}
