package com.axodouble.modules;

import com.axodouble.Double;
import com.axodouble.managers.InviteManager;
import com.axodouble.types.Arena;
import com.axodouble.types.DoublePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ModuleArena implements CommandExecutor, TabCompleter, Listener {
    private final Double plugin;
    private final Map<Player, List<Player>> groups = new HashMap<>();
    private final Map<Player, Player> playersByGroup = new HashMap<>();

    private final Map<Player, Player> invites = new HashMap<>();
    public ModuleArena(Double plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
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

                        top.add(MiniMessage.miniMessage().deserialize(medal + i + " " + playerName + " <dark_gray>- <gray>" + elo + " ELO (" + (ap.getWins()+ ap.getLosses()) + " games)"));
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
                } else {
                    plugin.badUsage((Player) sender);
                    arenaHelp(sender);
                }
                return true;
            }
            case "pvp" -> {
                if (!sender.hasPermission("double.pvp")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }

                if (args.length == 0) {
                    pvpHelp(sender);
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

                    InviteManager.Invite invite = plugin.getInviteManager().invites.get(p);

                    if (invite == null) {
                        sender.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>You don't have any invites!")
                        );
                        plugin.getInviteManager().invites.remove(p);
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

                    if (invite.arena.getState() != Arena.ArenaState.WAITING || plugin.getArenaManager().getArenas().stream().noneMatch(
                            a -> a.getName().equalsIgnoreCase(invite.arena.getName())
                    )) {
                        for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                            pl.sendMessage(
                                    MiniMessage.miniMessage().deserialize("<red>Arena not found!")
                            );
                        }
                        plugin.getInviteManager().invites.remove(p);
                        return true;
                    }

                    List<Player> playersToFight = new ArrayList<>();

                    if (invite.group) {
                        List<Player> group1 = getPlayersByGroup(invite.inviter);
                        List<Player> group2 = getPlayersByGroup(invite.invited);

                        if (group1 == null || group2 == null) {
                            for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                        MiniMessage.miniMessage().deserialize("<red>Group not found!")
                                );
                            }
                            plugin.getInviteManager().invites.remove(p);
                            return true;
                        }

                        if (group1.size() < 2 || group2.size() < 2) {
                            for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                        MiniMessage.miniMessage().deserialize("<red>Group must have at least 2 players!")
                                );
                            }
                            plugin.getInviteManager().invites.remove(p);
                            return true;
                        }

                        boolean allPlayersAreReady = true;

                        for (Player pl : group1) {
                            if (plugin.getArenaManager().getArena(pl) != null) {
                                return true;
                            }
                        }

                        for (Player pl : group2) {
                            if (plugin.getArenaManager().getArena(pl) != null) {
                                return true;
                            }
                        }

                        if (!allPlayersAreReady) {
                            for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                        MiniMessage.miniMessage().deserialize("<red>One or more players are already in an arena!")
                                );
                            }
                            plugin.getInviteManager().invites.remove(p);
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
                    if (playersByGroup.containsKey(player) && !groups.containsKey(player)) {
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

                        if (playersByGroup.containsKey(target)) {
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

                        invites.put(target, player);
                    }

                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Invites sent to " + targets.stream().map(Player::getName).collect(Collectors.joining(", "))));
                    return true;
                } else if (args[0].equalsIgnoreCase("accept")) {
                    Player inviter = invites.get(player);

                    if (inviter == null || !inviter.isOnline()) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No invites found"));
                        return true;
                    }

                    if (playersByGroup.containsKey(inviter) && !groups.containsKey(inviter)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + inviter.getName() + " is already in a group"));
                        return true;
                    }

                    if (playersByGroup.containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already in a group"));
                        return true;
                    }

                    List<Player> group = groups.get(inviter);
                    if (group == null) {
                        group = new ArrayList<>();
                        group.add(inviter);
                        playersByGroup.put(inviter, inviter);
                    }
                    group.add(player);
                    groups.put(inviter, group);

                    playersByGroup.put(player, inviter);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Invite accepted"));
                    for (Player pl : group) {
                        pl.sendMessage(MiniMessage.miniMessage().deserialize("<green>Player " + player.getName() + " has joined the group"));
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("leave")) {
                    leaveGang(player);
                } else if (args[0].equalsIgnoreCase("kick")) {
                    if (!groups.containsKey(player)) {
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

                    List<Player> group = groups.get(player);

                    if (!group.contains(target)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + target.getName() + " is not in your group"));
                        return true;
                    }

                    group.remove(target);
                    playersByGroup.remove(target);

                    target.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have been kicked from the group by " + player.getName()));

                    if (group.size() <= 1) {

                        groups.get(player).forEach(p -> {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Group has been disbanded"));
                            playersByGroup.remove(p);
                            groups.remove(player);
                        });

                        groups.remove(player);
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

                    if (!groups.containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are not in a group"));
                        return true;
                    }

                    if (!groups.containsKey(invited) || (groups.get(player).contains(invited) || groups.get(invited).contains(player))) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + playerName + " is not in a group"));
                        return true;
                    }

                    plugin.getArenaSelectGUI().openArenaList(player, invited);
                }

                return true;
            }
            case "top", "leaderboard" -> leaderboard(sender);
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
                    plugin.getArenaManager().getArenas().forEach(a -> {
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
                    if (groups.containsKey((Player) sender)) {
                        groups.get((Player) sender).forEach(p -> tabOptions.add(p.getName()));
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

    // If a block gets placed in the arena, add it to the list of placed blocks
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Arena arena = plugin.getArenaManager().getArena(e.getPlayer());
        if (arena == null) {
            return;
        }

        arena.placeBlock(e.getBlockPlaced().getLocation());
    }

    // If a block gets broken in the arena, remove it from the list of placed blocks
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Arena arena = plugin.getArenaManager().getArena(e.getPlayer());
        if (arena == null) {
            return;
        }

        if (arena.getPlacedBlocks().contains(e.getBlock().getLocation())) {
            arena.removeBlock(e.getBlock().getLocation());
            return;
        }

        e.setCancelled(true);
    }

    private boolean isInArena(Player player) {
        return plugin.getArenaManager().getArena(player) != null;
    }

    // TNT
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        Player source = null;
        if (e.getEntityType().equals(EntityType.TNT)) {
            TNTPrimed tnt = (TNTPrimed) e.getEntity();
            Entity entity = tnt.getSource();
            if (!(entity instanceof Player)) {
                return;
            }
            source = (Player) entity;
        }

        if (source == null || !isInArena(source)) {
            return;
        }
        e.blockList().clear();
    }

    // End crystal
    @EventHandler
    public void onHitCrystal(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();

        Player source;

        if (!(entity instanceof EnderCrystal)) {
            return;
        }

        if (damager instanceof Player) {
            source = (Player) damager;
        } else if (damager instanceof Arrow arrow) {
            Entity entity2 = (Entity) arrow.getShooter();
            if (!(entity2 instanceof Player)) {
                return;
            }
            source = (Player) entity2;
        } else {
            return;
        }

        if (!isInArena(source)) {
            return;
        }

        e.setCancelled(true);
        if (e.getEntity().isValid()) {
            e.getEntity().remove();
        }
        e.getEntity().getWorld().createExplosion(
                e.getEntity().getLocation(),
                6,
                false,
                false
        );
    }

    // Respawn Anchor
    @EventHandler
    public void onFillAnchor(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        if (!e.getClickedBlock().getType().equals(Material.RESPAWN_ANCHOR)) {
            return;
        }

        Block block = e.getClickedBlock();
        RespawnAnchor data = (RespawnAnchor) block.getBlockData();
        if (data.getCharges() < data.getMaximumCharges()) {
            return;
        }

        if (!isInArena(e.getPlayer())) {
            return;
        }

        e.setCancelled(true);
        block.setType(Material.AIR);
        block.getWorld().createExplosion(
                block.getLocation(),
                5,
                false,
                false
        );
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player player)) {
            return;
        }

        // If neither the damager nor the player are in an arena, return
        if (!isInArena(damager) && !isInArena(player)) {
            return;
        }

        // If the damager and the player are in different arenas, return
        if (((isInArena(damager) && !isInArena(player)) && (!isInArena(damager) && isInArena(player))) && !Objects.equals(plugin.getArenaManager().getArena(damager).getName(), plugin.getArenaManager().getArena(player).getName())) {
            e.setCancelled(true);
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(damager);

        // If the arena is null, return
        if (arena == null) {
            return;
        }

        // If the arena is not running, return
        if (arena.getState() != Arena.ArenaState.RUNNING) {
            e.setCancelled(true);
            return;
        }

        // If the damager and the player are in the same team, return
        if (arena.getTeam1().contains(damager) && arena.getTeam1().contains(player)) {
            e.setCancelled(true);
            return;
        }

        // If the damager and the player are in the same team, return
        if (arena.getTeam2().contains(damager) && arena.getTeam2().contains(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }

        if (!isInArena(victim)) {
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(victim);

        double healthAfter = victim.getHealth() - e.getFinalDamage();
        if (healthAfter <= 0) {
            // Check if during the fight totems are allowed
            if (arena.getTotems()) {
                if (
                        (victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) ||
                                (victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING)
                ) {
                    return;
                }
            }

            // #TODO: Fix ELO only being calculated when the player dies directly by another player
            if (e instanceof EntityDamageByEntityEvent) {
                if((((EntityDamageByEntityEvent) e).getDamager() instanceof Player killer)) {
                    // Do ELO calculations
                    calculateElo(victim, killer);
                }
            }

            e.setCancelled(true);
            victim.setHealth(victim.getHealthScale());

            arena.end(victim, false);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!isInArena(e.getEntity())) {
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(e.getEntity());

        Location loc = e.getEntity().getLocation();

        Player killer = e.getEntity().getKiller();

        if (killer == null) {
            killer = Bukkit.getPlayer(Objects.requireNonNull(Objects.requireNonNull(e.getEntity().getLastDamageCause()).getEntity().customName()).toString());
        }
        if (killer != null) {
            calculateElo(e.getEntity(), killer);
        }


        e.getEntity().spigot().respawn();
        new BukkitRunnable() {
            @Override
            public void run() {
                e.getEntity().teleport(loc);
                arena.end(e.getEntity(), false);
            }
        }.runTaskLater(plugin, 5L);
        updateScoreboard();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        updateScoreboard();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (playersByGroup.containsKey(player)) {
            leaveGang(player);
        }

        if (!isInArena(e.getPlayer())) {
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(e.getPlayer());

        // Check in which team the player is
        if (arena.getTeam1().contains(e.getPlayer())) {
            calculateElo(e.getPlayer(), arena.getTeam2().get(0));
        } else {
            calculateElo(e.getPlayer(), arena.getTeam1().get(0));
        }
        updateScoreboard();
    }

    private void calculateElo(Player loser, Player winner) {
        UUID winnerUUID = winner.getUniqueId();
        UUID loserUUID = loser.getUniqueId();

        // Get the win chance
        int winChance = plugin.getPlayerManager().CalculateWinChance(winnerUUID, loserUUID);

        Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<green>" + winner.getName() + " just killed " + loser.getName() + " in the " + plugin.getArenaManager().getArena(loser).getName() + " arena with a win chance of " + winChance + "%!"));

        // Handle ELO calculations
        plugin.getPlayerManager().PlayerKill(winnerUUID, loserUUID);

        updateScoreboard();
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
        if (plugin.getArenaManager().getArenas().stream().anyMatch(a -> a.getName().equalsIgnoreCase(name))) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Arena already exists")
            );
            return;
        }

        File file = new File(plugin.getDataFolder(), "data/arenas/" + name + ".yml");

        Arena arena = new Arena(plugin, name, sender.getName(), ((Player) sender).getLocation(), ((Player) sender).getLocation(), false, file);

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
                                < yellow ><bold > Arena Help
                                <green>/arena list -Show your arenas
                                <green >/arena delete <name > -Delete your arena
                                <green >/arena public <name > -Make arena public/private
                                <yellow > How to create a new arena:
                                <gold > 1. < green >/arena create <name > -Create a new arena
                                <gold> 2. < green >/arena sp1 <name > -Set spawn point for the first player
                                <gold> 3. < green >/arena sp2 <name > -Set spawn point for the second player
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

    private void pvpHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("""
                <gray> Usage: /pvp<player>
                <gray> Usage: /pvp accept
                <gray> Usage: /pvp reload
                """));
    }

    private void leaveGang(Player player) {
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

    public List<Player> getPlayersByGroup(Player player) {
        return groups.getOrDefault(player, null);
    }

    public void updateScoreboard() {
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

    public void leaderboard(CommandSender sender) {
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
