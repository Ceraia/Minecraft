package com.ceraia.modules;

import com.ceraia.Ceraia;
import com.ceraia.modules.arena.*
import com.ceraia.modules.arena.ArenaDefaultMessages.arenaHelp
import com.ceraia.modules.arena.ArenaDefaultMessages.pvpHelp
import com.ceraia.types.CeraiaPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.StringUtil;
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.stream.Collectors

class ModuleArena(private val plugin: Ceraia) : CommandExecutor, TabCompleter, Listener {

    private val invites: MutableMap<Player, Player> = mutableMapOf()
val arenaActions = ArenaActions(plugin)
val arenaManager = ArenaManager(plugin)
val arenaInviteManager = ArenaInviteManager()
    val arenaSelectGUI = ArenaSelectGUI(plugin)

    init {
        plugin.getCommand("arena")?.setExecutor(this)
        plugin.getCommand("pvp")?.setExecutor(this)
        plugin.getCommand("gvg")?.setExecutor(this)
        plugin.getCommand("leaderboard")?.setExecutor(this)
        plugin.getCommand("profile")?.setExecutor(this)

        plugin.getCommand("arena")?.tabCompleter =
            this
        plugin.getCommand("pvp")?.tabCompleter =
            this
        plugin.getCommand("gvg")?.tabCompleter =
            this
        plugin.getCommand("leaderboard")?.tabCompleter =
            this
        plugin.getCommand("profile")?.tabCompleter =
            this


        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun arenaHelp(sender: CommandSender) {
        sender.sendMessage(
            MiniMessage.miniMessage().deserialize(
                """
                < yellow ><bold > Arena Help
                <green>/arena list -Show your arenas
                <green >/arena delete <name > -Delete your arena
                <green >/arena public <name > -Make arena public/private
                <yellow > How to create a new arena:
                <gold > 1. < green >/arena create <name > -Create a new arena
                <gold> 2. < green >/arena sp1 <name > -Set spawn point for the first player
                <gold> 3. < green >/arena sp2 <name > -Set spawn point for the second player
                """.trimIndent()
            )
        )
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        when (cmd.name) {
            "arena" -> {
                if (!sender.hasPermission("ceraia.arena")) {
                    plugin.noPermission(sender as Player)
                    return true
                }

                if (args.size == 0) {
                    arenaHelp(sender)
                    return true
                }

                if (args[0].equals("list", ignoreCase = true)) {
                    plugin.moduleArena.arenaActions.arenaList(sender)
                    plugin.moduleArena.arenaActions.arenaList(sender)
                    return true
                }

                if (args[0].equals("scoreboard", ignoreCase = true) || args[0].equals("top", ignoreCase = true)) {
                    val p = sender as Player

                    // Create and show a string list of the top 10 players with the highest elo
                    val top: MutableList<Component> = ArrayList()
                    val i = AtomicInteger()
                    i.set(1)

                    top.add(MiniMessage.miniMessage().deserialize("<yellow><bold>Top 10 players with the highest ELO:"))

                    plugin.playerManager.ceraiaPlayers.stream()
                        .sorted(Comparator.comparingInt<CeraiaPlayer>(CeraiaPlayer::elo).reversed()).limit(10)
                        .forEach { ap: CeraiaPlayer ->
                            val playerName = Bukkit.getOfflinePlayer(ap.getUUID()).name
                            val elo = ap.elo

                            // Check for 1st, 2nd, and 3rd place
                            val medal = if (i.get() == 1) {
                                "<gold>" // Gold for 1st place
                            } else if (i.get() == 2) {
                                "<#C0C0C0>" // Silver for 2nd place
                            } else if (i.get() == 3) {
                                "<#cd7f32>" // Bronze for 3rd place
                            } else {
                                "<white>" // Default medal color for players outside the top 3
                            } // Default medal color for players outside the top 3

                            top.add(
                                MiniMessage.miniMessage()
                                    .deserialize(medal + i + " " + playerName + " <dark_gray>- <gray>" + elo + " ELO (" + (ap.wins + ap.losses) + " games)")
                            )
                            i.getAndIncrement()
                        }

                    // Send the top 10 players with the highest elo to the player
                    top.forEach(Consumer { message: Component? ->
                        p.sendMessage(
                            message!!
                        )
                    })

                    return true
                }

                if (args[0].equals("delete", ignoreCase = true)) {
                    plugin.moduleArena.arenaActions.arenaDelete(sender, args)
                    return true
                }
                if (args[0].equals("create", ignoreCase = true)) {
                    plugin.moduleArena.arenaActions.arenaCreate(sender, args)
                    return true
                }
                if (args[0].equals("sp1", ignoreCase = true)) {
                    plugin.moduleArena.arenaActions.arenaSP1(sender, args)
                    return true
                }
                if (args[0].equals("sp2", ignoreCase = true)) {
                    plugin.moduleArena.arenaActions.arenaSP2(sender, args)
                    return true
                }
                if (args[0].equals("public", ignoreCase = true)) {
                    plugin.moduleArena.arenaActions.arenaPublic(sender, args)
                } else {

                    arenaHelp(sender)
                }
                return true
            }

            "pvp" -> {
                if (!sender.hasPermission("ceraia.pvp")) {
                    plugin.noPermission(sender as Player)
                    return true
                }

                if (args.size == 0) {
                    pvpHelp(sender)
                    return true
                }

                if (args[0].equals("accept", ignoreCase = true)) {
                    val p = sender

                    val invite = plugin.moduleArena.arenaInviteManager.invites[p]

                    if (invite == null) {
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>You don't have any invites!")
                        )
                        plugin.moduleArena.arenaInviteManager.invites.remove(p)
                        return true
                    }

                    if (!invite.invited.isOnline || !invite.inviter.isOnline) {
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Player not found!")
                        )
                        return true
                    }

                    if (invite.accepted) {
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>You already accepted this invite!")
                        )
                        return true
                    }

                    if (invite.arena!!.getState() != Arena.ArenaState.WAITING || plugin.moduleArena.arenaManager.arenas.stream()
                            .noneMatch { a: Arena ->
                                a.name.equals(
                                    invite.arena!!.name, ignoreCase = true
                                )
                            }
                    ) {
                        for (pl in Arrays.asList(invite.invited, invite.inviter)) {
                            pl.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>Arena not found!")
                            )
                        }
                        plugin.moduleArena.arenaInviteManager.invites.remove(p)
                        return true
                    }

                    val playersToFight: MutableList<Player> = ArrayList()

                    if (invite.group) {
                        val group1 = plugin.moduleArena.arenaActions.getPlayersByGroup(invite.inviter)
                        val group2 = plugin.moduleArena.arenaActions.getPlayersByGroup(invite.invited)

                        if (group1 == null || group2 == null) {
                            for (pl in Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                    MiniMessage.miniMessage().deserialize("<red>Group not found!")
                                )
                            }
                            plugin.moduleArena.arenaInviteManager.invites.remove(p)
                            return true
                        }

                        if (group1.size < 2 || group2.size < 2) {
                            for (pl in Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                    MiniMessage.miniMessage().deserialize("<red>Group must have at least 2 players!")
                                )
                            }
                            plugin.moduleArena.arenaInviteManager.invites.remove(p)
                            return true
                        }

                        val allPlayersAreReady = true

                        for (pl in group1) {
                            if (plugin.moduleArena.arenaManager.getArena(pl) != null) {
                                return true
                            }
                        }

                        for (pl in group2) {
                            if (plugin.moduleArena.arenaManager.getArena(pl) != null) {
                                return true
                            }
                        }

                        if (!allPlayersAreReady) {
                            for (pl in Arrays.asList(invite.invited, invite.inviter)) {
                                pl.sendMessage(
                                    MiniMessage.miniMessage()
                                        .deserialize("<red>One or more players are already in an arena!")
                                )
                            }
                            plugin.moduleArena.arenaInviteManager.invites.remove(p)
                            return true
                        }

                        playersToFight.addAll(group1)
                        playersToFight.addAll(group2)

                        group1.forEach(Consumer { pl: Player? -> invite.arena!!.addPlayer(pl!!, 1) })
                        group2.forEach(Consumer { pl: Player? -> invite.arena!!.addPlayer(pl!!, 2) })
                    } else {
                        playersToFight.add(invite.invited)
                        playersToFight.add(invite.inviter)
                        invite.arena!!.addPlayer(invite.invited, 1)
                        invite.arena!!.addPlayer(invite.inviter, 2)
                    }

                    // Starting arena
                    invite.accepted = true
                    invite.arena!!.start(invite, playersToFight)

                    return true
                }

                // open gui for invite player
                val playerName = args[0]

                // reload for op
                if (playerName.equals("reload", ignoreCase = true) && sender.isOp()) {
                    plugin.reloadConfig()
                    sender.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            "<green>Reloaded!"
                        )
                    )
                    return true
                } // If the player is reload and the sender is op, reload the config


                val invited = Bukkit.getPlayer(playerName)

                if (invited == null) {
                    sender.sendMessage(
                        MiniMessage.miniMessage().deserialize("<red>Player not found!")
                    )
                    return true
                } // If the player is offline, return


                val inviter = sender

                if (inviter === invited) {
                    sender.sendMessage(
                        MiniMessage.miniMessage().deserialize("<red>You can't invite yourself!")
                    )
                    return true
                } // If the inviter is the same as the invited, return


                plugin.moduleArena.arenaSelectGUI.openArenaList(inviter as Player, invited)
                return true
            }

            "gvg" -> {
                if (!sender.hasPermission("ceraia.gvg")) {
                    plugin.noPermission(sender as Player)
                    return true
                }

                if (args.size == 0) {
                    sender.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            """
                                    <red>Usage: /gvg invite <player>
                                    /gvg accept
                                    /gvg leave
                                    /gvg kick <player>
                                    /gvg fight <player>
                            
                            """.trimIndent()
                        )
                    )
                    return true
                }

                val player = sender as Player

                if (args[0].equals("invite", ignoreCase = true)) {
                    if (plugin.moduleArena.arenaActions.playersByGroup.containsKey(player) && !plugin.moduleArena.arenaActions.groups.containsKey(
                            player
                        )
                    ) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already in a group"))
                        return true
                    }

                    if (args.size == 1) {
                        arenaHelp(sender)
                        return true
                    }

                    val targets: MutableList<Player> = ArrayList()
                    val notOnline: MutableList<String> = ArrayList()

                    for (i in 1 until args.size) {
                        val target = Bukkit.getPlayer(args[i])
                        if (target == null) {
                            notOnline.add(args[i])
                        } else {
                            targets.add(target)
                        }

                        if (plugin.moduleArena.arenaActions.playersByGroup.containsKey(target)) {
                            checkNotNull(target)
                            sender.sendMessage(
                                MiniMessage.miniMessage()
                                    .deserialize("<red>Player " + target.name + " is already in a group")
                            )
                            return true
                        }
                    }

                    if (targets.contains(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot invite yourself"))
                        return true
                    }

                    if (!notOnline.isEmpty()) {
                        println(notOnline)
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                "<red>Player(s) " + java.lang.String.join(
                                    ", ",
                                    notOnline
                                ) + " are not online"
                            )
                        )
                        return true
                    }

                    for (target in targets) {
                        target.sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                """
                                You have been invited to a group by ${player.name}
                                <green>/gvg accept</green> to accept the invite
                                """.trimIndent()
                            )
                        )

                        plugin.moduleArena.arenaActions.invites[target] = player
                    }

                    sender.sendMessage(
                        MiniMessage.miniMessage()
                            .deserialize("<green>Invites sent to " + targets.stream().map { obj: Player -> obj.name }
                                .collect(
                                    Collectors.joining(", ")
                                )))
                    return true
                } else if (args[0].equals("accept", ignoreCase = true)) {
                    val inviter = plugin.moduleArena.arenaActions.invites[player]

                    if (inviter == null || !inviter.isOnline) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No invites found"))
                        return true
                    }

                    if (plugin.moduleArena.arenaActions.playersByGroup.containsKey(inviter) && !plugin.moduleArena.arenaActions.groups.containsKey(
                            inviter
                        )
                    ) {
                        sender.sendMessage(
                            MiniMessage.miniMessage()
                                .deserialize("<red>Player " + inviter.name + " is already in a group")
                        )
                        return true
                    }

                    if (plugin.moduleArena.arenaActions.playersByGroup.containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already in a group"))
                        return true
                    }

                    var group = plugin.moduleArena.arenaActions.groups[inviter]
                    if (group == null) {
                        group = ArrayList()
                        group.add(inviter)
                        plugin.moduleArena.arenaActions.playersByGroup[inviter] = inviter
                    }
                    group.add(player)
                    plugin.moduleArena.arenaActions.groups[inviter] = group

                    plugin.moduleArena.arenaActions.playersByGroup[player] = inviter
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Invite accepted"))
                    for (pl in group) {
                        pl.sendMessage(
                            MiniMessage.miniMessage()
                                .deserialize("<green>Player " + player.name + " has joined the group")
                        )
                    }
                    return true
                } else if (args[0].equals("leave", ignoreCase = true)) {
                    plugin.moduleArena.arenaActions.leaveGang(player)
                } else if (args[0].equals("kick", ignoreCase = true)) {
                    if (!plugin.moduleArena.arenaActions.groups.containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are not in a group"))
                        return true
                    }

                    if (args.size == 1) {
                        arenaHelp(sender)
                        return true
                    }

                    val target = plugin.server.getPlayer(args[1])
                    if (target == null) {
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Player " + args[1] + " is not online")
                        )
                        return true
                    }

                    val group = plugin.moduleArena.arenaActions.groups[player]!!

                    if (!group.contains(target)) {
                        sender.sendMessage(
                            MiniMessage.miniMessage()
                                .deserialize("<red>Player " + target.name + " is not in your group")
                        )
                        return true
                    }

                    group.remove(target)
                    plugin.moduleArena.arenaActions.playersByGroup.remove(target)

                    target.sendMessage(
                        MiniMessage.miniMessage()
                            .deserialize("<red>You have been kicked from the group by " + player.name)
                    )

                    if (group.size <= 1) {
                        plugin.moduleArena.arenaActions.groups[player]!!.forEach(Consumer { p: Player? ->
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Group has been disbanded"))
                            plugin.moduleArena.arenaActions.playersByGroup.remove(p)
                            plugin.moduleArena.arenaActions.groups.remove(player)
                        })

                        plugin.moduleArena.arenaActions.groups.remove(player)
                    } else {
                        group.forEach(Consumer { p: Player ->
                            p.sendMessage(
                                MiniMessage.miniMessage()
                                    .deserialize("<red>Player " + target.name + " has been kicked from the group by " + player.name)
                            )
                        })
                    }
                    return true
                } else if (args[0].equals("fight", ignoreCase = true)) {
                    val playerName = args[1]
                    val invited = Bukkit.getPlayer(playerName)

                    if (invited == null) {
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Player $playerName is not online")
                        )
                        return true
                    }

                    if (invited === player) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot fight yourself"))
                        return true
                    }

                    if (!plugin.moduleArena.arenaActions.groups.containsKey(player)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are not in a group"))
                        return true
                    }

                    if (!plugin.moduleArena.arenaActions.groups.containsKey(invited) || (plugin.moduleArena.arenaActions.groups[player]!!
                            .contains(invited) || plugin.moduleArena.arenaActions.groups[invited]!!.contains(player))
                    ) {
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Player $playerName is not in a group")
                        )
                        return true
                    }

                    plugin.moduleArena.arenaSelectGUI.openArenaList(player, invited)
                }

                return true
            }

            "top", "leaderboard" -> plugin.moduleArena.arenaActions.leaderboard(sender)
            "profile" -> {
                if (!sender.hasPermission("ceraia.pvp")) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid usage"))
                    return true
                }

                val player: Player?
                if (args.size == 1) {
                    player = Bukkit.getPlayer(args[0])
                    if (player == null) {
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                "<red>Invalid usage"
                            )
                        )
                        return true
                    }
                } else {
                    player = sender as Player
                }

                // Return the player's profile
                val doublePlayer = plugin.playerManager.getCeraiaPlayer(player.uniqueId)
                sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        """
                        <yellow><bold>Profile of ${player.name}
                        <yellow><bold>ELO: <green>${doublePlayer.elo}
                        <yellow><bold>Games: <green>${doublePlayer.wins + doublePlayer.losses}
                        <yellow><bold>Wins: <green>${doublePlayer.wins}
                        <yellow><bold>Losses: <green>${doublePlayer.losses}
                        """.trimIndent()
                    )
                )

                return true
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<String>): List<String>? {
        when (cmd.name) {
            "arena" -> {
                if (args.size == 1) {
                    return mutableListOf("list", "delete", "public", "create", "sp1", "sp2", "top", "scoreboard")
                } else if (args.size == 2 && (args[0].equals("delete", ignoreCase = true) ||
                            args[0].equals("public", ignoreCase = true) ||
                            args[0].equals("sp1", ignoreCase = true) ||
                            args[0].equals("sp2", ignoreCase = true))
                ) {
                    val tabOptions: MutableList<String> = ArrayList()
                    plugin.moduleArena.arenaManager.arenas.forEach(Consumer { a: Arena ->
                        if (a.owner == sender.name) {
                            tabOptions.add(a.name)
                        }
                    })
                    val returnedOptions: MutableList<String> = ArrayList()
                    StringUtil.copyPartialMatches(args[args.size - 1], tabOptions, returnedOptions)

                    return returnedOptions
                } else if (args.size == 2 && args[0].equals("create", ignoreCase = true)) {
                    return listOf("<name>")
                } else if ((args.size == 3 && args[0].equals("public", ignoreCase = true))) {
                    return mutableListOf("true", "false")
                }
                return ArrayList()
            }

            "pvp" -> {
                if (args.size == 1) {
                    val tabOptions: MutableList<String> = ArrayList()
                    // If there is an argument, suggest online player names
                    for (player in Bukkit.getOnlinePlayers()) {
                        // Exclude the sender's name from the suggestions
                        if (player.name != sender.name) {
                            tabOptions.add(player.name)
                        }
                    }
                    val returnedOptions: MutableList<String> = ArrayList()
                    StringUtil.copyPartialMatches(args[args.size - 1], tabOptions, returnedOptions)

                    return returnedOptions
                }
                // If there is more than one argument, return an empty list
                return ArrayList()
            }

            "gvg" -> {
                if (args.size == 1) {
                    return mutableListOf("invite", "accept", "leave", "kick", "fight")
                } else if (args.size == 2 && args[0].equals("kick", ignoreCase = true)) {
                    val tabOptions = ArrayList<String>()
                    if (plugin.moduleArena.arenaActions.groups.containsKey(sender as Player)) {
                        plugin.moduleArena.arenaActions.groups[sender]!!
                            .forEach(Consumer { p: Player -> tabOptions.add(p.name) })
                    }
                    val returnedOptions: MutableList<String> = ArrayList()
                    StringUtil.copyPartialMatches(args[args.size - 1], tabOptions, returnedOptions)
                    return returnedOptions
                } else if (args.size == 2 && (args[0].equals("invite", ignoreCase = true) || args[0].equals(
                        "fight",
                        ignoreCase = true
                    ))
                ) {
                    val returnedOptions: MutableList<String> = ArrayList()
                    StringUtil.copyPartialMatches(
                        args[args.size - 1],
                        Bukkit.getOnlinePlayers().stream().map { obj: Player -> obj.name }.collect(
                            Collectors.toList()
                        ),
                        returnedOptions
                    )

                    return returnedOptions
                }

                return ArrayList()
            }

            "top" -> {
                return ArrayList()
            }

            "profile" -> {
                if (args.size == 1) {
                    // Return a list of all players
                    val tabOptions: MutableList<String> = ArrayList()
                    Bukkit.getOnlinePlayers().forEach { p: Player -> tabOptions.add(p.name) }
                    val returnedOptions: MutableList<String> = ArrayList()
                    StringUtil.copyPartialMatches(args[args.size - 1], tabOptions, returnedOptions)
                    return returnedOptions
                }
            }
        }
        return null
    }
}
