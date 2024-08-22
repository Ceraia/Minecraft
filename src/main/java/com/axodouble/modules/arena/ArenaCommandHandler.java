package com.axodouble.modules.arena;

import com.axodouble.Double;
import com.axodouble.types.DoublePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ArenaCommandHandler implements CommandExecutor, TabCompleter {
    private Double plugin;

    public ArenaCommandHandler(Double plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(this.plugin.getCommand("arena")).setExecutor(this);
        Objects.requireNonNull(this.plugin.getCommand("pvp")).setExecutor(this);
        Objects.requireNonNull(this.plugin.getCommand("gvg")).setExecutor(this);
        Objects.requireNonNull(this.plugin.getCommand("leaderboard")).setExecutor(this);
        Objects.requireNonNull(this.plugin.getCommand("profile")).setExecutor(this);

        Objects.requireNonNull(this.plugin.getCommand("arena")).setTabCompleter(this);
        Objects.requireNonNull(this.plugin.getCommand("pvp")).setTabCompleter(this);
        Objects.requireNonNull(this.plugin.getCommand("gvg")).setTabCompleter(this);
        Objects.requireNonNull(this.plugin.getCommand("leaderboard")).setTabCompleter(this);
        Objects.requireNonNull(this.plugin.getCommand("profile")).setTabCompleter(this);
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        switch (cmd.getName()) {
            case "arena" -> {
                if (!sender.hasPermission("double.arena")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }

                if (args.length == 0) {
                    ArenaDefaultMessages.arenaHelp(sender);
                    return true;
                }

                if (args[0].equalsIgnoreCase("list")) {
                    plugin.getArenaModule().getArenaActions().arenaList(sender);
                    plugin.getArenaModule().getArenaActions().arenaList(sender);
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

                        top.add(MiniMessage.miniMessage().deserialize(medal + i + " " + playerName + " <dark_gray>- <gray>" + elo + " ELO (" + (ap.getWins()+ ap.getLosses()) + " games)"));
                        i.getAndIncrement();
                    });

                    // Send the top 10 players with the highest elo to the player
                    top.forEach(p::sendMessage);

                    return true;
                }

                if (args[0].equalsIgnoreCase("delete")) {
                    plugin.getArenaModule().getArenaActions().arenaDelete(sender, args);
                    return true;
                }
                if (args[0].equalsIgnoreCase("create")) {
                    plugin.getArenaModule().getArenaActions().arenaCreate(sender, args);
                    return true;
                }
                if (args[0].equalsIgnoreCase("sp1")) {
                    plugin.getArenaModule().getArenaActions().arenaSP1(sender, args);
                    return true;
                }
                if (args[0].equalsIgnoreCase("sp2")) {
                    plugin.getArenaModule().getArenaActions().arenaSP2(sender, args);
                    return true;
                }
                if (args[0].equalsIgnoreCase("public")) {
                    plugin.getArenaModule().getArenaActions().arenaPublic(sender, args);
                } else {
                    plugin.badUsage((Player) sender);
                    ArenaDefaultMessages.arenaHelp(sender);
                }
                return true;
            }
            case "pvp" -> {
                if (!sender.hasPermission("double.pvp")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }

                if (args.length == 0) {
                    ArenaDefaultMessages.pvpHelp(sender);
                    return true;
                }

                // Check if the sender is pvpbanned
                DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(((Player) sender).getUniqueId());
                if (doublePlayer.isPvpBanned()) {
                    sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>You are banned from PvP!")
                    );
                    return true;
                }

                if (args[0].equalsIgnoreCase("accept")) {
                    Player p = (Player) sender;

                    // Check if the player is banned
                    if (plugin.getPlayerManager().getDoublePlayer(p.getUniqueId()).isPvpBanned()) {
                        sender.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>This player is banned from PvP!")
                        );
                        return true;
                    }

                    ArenaInviteManager.Invite invite = plugin.getArenaInviteManager().invites.get(p);

                    if (invite == null) {
                        sender.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>You don't have any invites!")
                        );
                        plugin.getArenaInviteManager().invites.remove(p);
                        return true;
                    }

                    if (!invite.invited.isOnline() || !invite.inviter.isOnline()) {
                        sender.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>Player not found!")
                        );
                        return true;
                    }

                    if (invite.accepted) {
                        sender.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>You already accepted this invite!")
                        );
                        return true;
                    }

                    if (invite.arena.getState() != Arena.ArenaState.WAITING || plugin.getArenaModule().arenaManager.getArenas().stream().noneMatch(
                            a -> a.getName().equalsIgnoreCase(invite.arena.getName())
                    )) {
                        for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                            pl.sendMessage(
                                    MiniMessage.miniMessage().deserialize("<red>Arena not found!")
                            );
                        }
                        plugin.getArenaInviteManager().invites.remove(p);
                        return true;
                    }

                    List<Player> playersToFight = new ArrayList<>();

                    if (invite.group) {
                        List<Player> group1 = plugin.getArenaModule().getArenaActions().getPlayersByGroup(invite.inviter);
                        List<Player> group2 = plugin.getArenaModule().getArenaActions().getPlayersByGroup(invite.invited);

                        if (group1 == null || group2 == null) {
                            for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                        MiniMessage.miniMessage().deserialize("<red>Group not found!")
                                );
                            }
                            plugin.getArenaInviteManager().invites.remove(p);
                            return true;
                        }

                        if (group1.size() < 2 || group2.size() < 2) {
                            for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                        MiniMessage.miniMessage().deserialize("<red>Group must have at least 2 players!")
                                );
                            }
                            plugin.getArenaInviteManager().invites.remove(p);
                            return true;
                        }

                        boolean allPlayersAreReady = true;

                        for (Player pl : group1) {
                            if (plugin.getArenaModule().arenaManager.getArena(pl) != null) {
                                return true;
                            }
                        }

                        for (Player pl : group2) {
                            if (plugin.getArenaModule().arenaManager.getArena(pl) != null) {
                                return true;
                            }
                        }

                        if (!allPlayersAreReady) {
                            for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                        MiniMessage.miniMessage().deserialize("<red>One or more players are already in an arena!")
                                );
                            }
                            plugin.getArenaInviteManager().invites.remove(p);
                            return true;
                        }

                        playersToFight.addAll(group1);
                        playersToFight.addAll(group2);

                        group1.forEach(pl -> invite.arena.addPlayer(pl, 1));
                        group2.forEach(pl -> invite.arena.addPlayer(pl, 2));
                    } else {
                        playersToFight.add(invite.invited);
                        playersToFight.add(invite.inviter);
                        invite.arena.addPlayer(invite.invited, 1);
                        invite.arena.addPlayer(invite.inviter, 2);
                    }

                    // Starting arena
                    invite.accepted = true;
                    invite.arena.start(invite, playersToFight);

                    return true;
                }

                // open gui for invite player

                String playerName = args[0];

                // reload for op
                if (playerName.equalsIgnoreCase("reload") && sender.isOp()) {
                    plugin.reloadConfig();
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<green>Reloaded!")
                    );
                    return true;
                } // If the player is reload and the sender is op, reload the config

                Player invited = Bukkit.getPlayer(playerName);

                if (invited == null) {
                    sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Player not found!")
                    );
                    return true;
                } // If the player is offline, return

                Player inviter = (Player) sender;

                if (inviter == invited) {
                    sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>You can't invite yourself!")
                    );
                    return true;
                } // If the inviter is the same as the invited, return

                plugin.getArenaSelectGUI().openArenaList(inviter, invited);
                return true;
            }
            case "gvg" -> {
                if (!sender.hasPermission("double.gvg")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }

                if (args.length == 0) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("""
                                    <red>Usage: /gvg invite <player>
                                    /gvg accept
                                    /gvg leave
                                    /gvg kick <player>
                                    /gvg fight <player>
                            """));
                    return true;
                }

                Player player = (Player) sender;

                if (args[0].equalsIgnoreCase("invite")) {

                    if (plugin.getArenaModule().getArenaActions().getPlayersByGroup().containsKey(player) && !plugin.getArenaModule().getArenaActions().getGroups().containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already in a group"));
                        return true;
                    }

                    if (args.length == 1) {
                        plugin.badUsage((Player) sender);
                        return true;
                    }

                    List<Player> targets = new ArrayList<>();
                    List<String> notOnline = new ArrayList<>();

                    for (int i = 1; i < args.length; i++) {
                        Player target = Bukkit.getPlayer(args[i]);
                        if (target == null) {
                            notOnline.add(args[i]);
                        } else {
                            targets.add(target);
                        }

                        if (plugin.getArenaModule().getArenaActions().getPlayersByGroup().containsKey(target)) {
                            assert target != null;
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + target.getName() + " is already in a group"));
                            return true;
                        }
                    }

                    if (targets.contains(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot invite yourself"));
                        return true;
                    }

                    if (!notOnline.isEmpty()) {
                        System.out.println(notOnline);
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player(s) " + String.join(", ", notOnline) + " are not online"));
                        return true;
                    }

                    for (Player target : targets) {
                        target.sendMessage(MiniMessage.miniMessage().deserialize(
                                "You have been invited to a group by " + player.getName() + "\n" +
                                        "<green>/gvg accept</green> to accept the invite"
                        ));

                        plugin.getArenaModule().getArenaActions().getInvites().put(target, player);
                    }

                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Invites sent to " + targets.stream().map(Player::getName).collect(Collectors.joining(", "))));
                    return true;
                } else if (args[0].equalsIgnoreCase("accept")) {
                    Player inviter = plugin.getArenaModule().getArenaActions().getInvites().get(player);

                    if (inviter == null || !inviter.isOnline()) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No invites found"));
                        return true;
                    }

                    if (plugin.getArenaModule().getArenaActions().getPlayersByGroup().containsKey(inviter) && !plugin.getArenaModule().getArenaActions().getGroups().containsKey(inviter)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + inviter.getName() + " is already in a group"));
                        return true;
                    }

                    if (plugin.getArenaModule().getArenaActions().getPlayersByGroup().containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already in a group"));
                        return true;
                    }

                    List<Player> group = plugin.getArenaModule().getArenaActions().getGroups().get(inviter);
                    if (group == null) {
                        group = new ArrayList<>();
                        group.add(inviter);
                        plugin.getArenaModule().getArenaActions().getPlayersByGroup().put(inviter, inviter);
                    }
                    group.add(player);
                    plugin.getArenaModule().getArenaActions().getGroups().put(inviter, group);

                    plugin.getArenaModule().getArenaActions().getPlayersByGroup().put(player, inviter);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Invite accepted"));
                    for (Player pl : group) {
                        pl.sendMessage(MiniMessage.miniMessage().deserialize("<green>Player " + player.getName() + " has joined the group"));
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("leave")) {
                    plugin.getArenaModule().getArenaActions().leaveGang(player);
                } else if (args[0].equalsIgnoreCase("kick")) {
                    if (!plugin.getArenaModule().getArenaActions().getGroups().containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are not in a group"));
                        return true;
                    }

                    if (args.length == 1) {
                        plugin.badUsage((Player) sender);
                        return true;
                    }

                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + args[1] + " is not online"));
                        return true;
                    }

                    List<Player> group = plugin.getArenaModule().getArenaActions().getGroups().get(player);

                    if (!group.contains(target)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + target.getName() + " is not in your group"));
                        return true;
                    }

                    group.remove(target);
                    plugin.getArenaModule().getArenaActions().getPlayersByGroup().remove(target);

                    target.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have been kicked from the group by " + player.getName()));

                    if (group.size() <= 1) {

                        plugin.getArenaModule().getArenaActions().getGroups().get(player).forEach(p -> {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Group has been disbanded"));
                            plugin.getArenaModule().getArenaActions().getPlayersByGroup().remove(p);
                            plugin.getArenaModule().getArenaActions().getGroups().remove(player);
                        });

                        plugin.getArenaModule().getArenaActions().getGroups().remove(player);
                    } else {
                        group.forEach(p -> p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + target.getName() + " has been kicked from the group by " + player.getName())));
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("fight")) {
                    String playerName = args[1];
                    Player invited = Bukkit.getPlayer(playerName);

                    if (invited == null) {
                        sender.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>Player " + playerName + " is not online"));
                        return true;
                    }

                    if (invited == player) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot fight yourself"));
                        return true;
                    }

                    if (!plugin.getArenaModule().getArenaActions().getGroups().containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are not in a group"));
                        return true;
                    }

                    if (!plugin.getArenaModule().getArenaActions().getGroups().containsKey(invited) || (plugin.getArenaModule().getArenaActions().getGroups().get(player).contains(invited) || plugin.getArenaModule().getArenaActions().getGroups().get(invited).contains(player))) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + playerName + " is not in a group"));
                        return true;
                    }

                    plugin.getArenaSelectGUI().openArenaList(player, invited);
                }

                return true;
            }
            case "top", "leaderboard" -> plugin.getArenaModule().getArenaActions().leaderboard(sender);
            case "profile" -> {
                if (!sender.hasPermission("double.pvp")) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid usage"));
                    return true;
                }

                Player player;
                if (args.length == 1) {
                    player = Bukkit.getPlayer(args[0]);
                    if (player == null) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("<red>Invalid usage"))));
                        return true;
                    }
                } else {
                    player = (Player) sender;
                }

                // Return the player's profile
                DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(player.getUniqueId());
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<yellow><bold>Profile of " + player.getName() +
                                "\n<yellow><bold>ELO: <green>" + doublePlayer.getElo() +
                                "\n<yellow><bold>Games: <green>" + (doublePlayer.getWins() + doublePlayer.getLosses()) +
                                "\n<yellow><bold>Wins: <green>" + doublePlayer.getWins() +
                                "\n<yellow><bold>Losses: <green>" + doublePlayer.getLosses() +
                                "\n<yellow><bold>PVP-Banned: <green>" + doublePlayer.isPvpBanned() +
                                "\n<yellow><bold>Arena-Banned: <green>" + doublePlayer.isArenaBanned()
                ));

                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        switch (cmd.getName()) {
            case "arena" -> {
                if (args.length == 1) {
                    return Arrays.asList("list", "delete", "public", "create", "sp1", "sp2", "top", "scoreboard");
                } else if (args.length == 2 && (
                        args[0].equalsIgnoreCase("delete") ||
                                args[0].equalsIgnoreCase("public") ||
                                args[0].equalsIgnoreCase("sp1") ||
                                args[0].equalsIgnoreCase("sp2"))) {
                    List<String> tabOptions = new ArrayList<>();
                    plugin.getArenaModule().arenaManager.getArenas().forEach(a -> {
                        if (a.getOwner().equals(sender.getName())) {
                            tabOptions.add(a.getName());
                        }
                    });
                    List<String> returnedOptions = new ArrayList<>();
                    StringUtil.copyPartialMatches(args[args.length - 1], tabOptions, returnedOptions);

                    return returnedOptions;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
                    return List.of("<name>");
                } else if ((args.length == 3 && args[0].equalsIgnoreCase("public"))) {
                    return Arrays.asList("true", "false");
                }
                return new ArrayList<>();
            }
            case "pvp" -> {
                if (args.length == 1) {
                    List<String> tabOptions = new ArrayList<>();
                    // If there is an argument, suggest online player names
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        // Exclude the sender's name from the suggestions
                        if (!player.getName().equals(sender.getName())) {
                            tabOptions.add(player.getName());
                        }
                    }
                    List<String> returnedOptions = new ArrayList<>();
                    StringUtil.copyPartialMatches(args[args.length - 1], tabOptions, returnedOptions);

                    return returnedOptions;
                }
                // If there is more than one argument, return an empty list
                return new ArrayList<>();
            }
            case "gvg" -> {
                if (args.length == 1) {
                    return Arrays.asList("invite", "accept", "leave", "kick", "fight");
                } else if (args.length == 2 && args[0].equalsIgnoreCase("kick")) {
                    ArrayList<String> tabOptions = new ArrayList<>();
                    if (plugin.getArenaModule().getArenaActions().getGroups().containsKey((Player) sender)) {
                        plugin.getArenaModule().getArenaActions().getGroups().get((Player) sender).forEach(p -> tabOptions.add(p.getName()));
                    }
                    List<String> returnedOptions = new ArrayList<>();
                    StringUtil.copyPartialMatches(args[args.length - 1], tabOptions, returnedOptions);
                    return returnedOptions;
                } else if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("fight"))) {
                    List<String> returnedOptions = new ArrayList<>();
                    StringUtil.copyPartialMatches(args[args.length - 1], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), returnedOptions);

                    return returnedOptions;
                }

                return new ArrayList<>();
            }
            case "top" -> {
                return new ArrayList<>();
            }
            case "profile" -> {
                if (args.length == 1) {
                    // Return a list of all players
                    List<String> tabOptions = new ArrayList<>();
                    Bukkit.getOnlinePlayers().forEach(p -> tabOptions.add(p.getName()));
                    List<String> returnedOptions = new ArrayList<>();
                    StringUtil.copyPartialMatches(args[args.length - 1], tabOptions, returnedOptions);
                    return returnedOptions;
                }
            }
        }
        return null;
    }
}
