package com.axodouble.modules.arena

import com.axodouble.Ceraia
import com.axodouble.types.CeraiaPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil
import org.jetbrains.annotations.NotNull
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.streams.toList

class ArenaCommandHandler(private val plugin: Ceraia) : CommandExecutor, TabCompleter {

    init {
        plugin.getCommand("arena")?.setExecutor(this)
        plugin.getCommand("pvp")?.setExecutor(this)
        plugin.getCommand("gvg")?.setExecutor(this)
        plugin.getCommand("leaderboard")?.setExecutor(this)
        plugin.getCommand("profile")?.setExecutor(this)

        plugin.getCommand("arena")?.tabCompleter = this
        plugin.getCommand("pvp")?.tabCompleter = this
        plugin.getCommand("gvg")?.tabCompleter = this
        plugin.getCommand("leaderboard")?.tabCompleter = this
        plugin.getCommand("profile")?.tabCompleter = this
    }

    override fun onCommand(sender: @NotNull CommandSender, cmd: @NotNull Command, label: String, args: Array<String>): Boolean {
        when (cmd.name) {
            "arena" -> {
                if (!sender.hasPermission("double.arena")) {
                    plugin.noPermission(sender as Player)
                    return true
                }

                if (args.isEmpty()) {
                    ArenaDefaultMessages.arenaHelp(sender)
                    return true
                }

                when (args[0].lowercase()) {
                    "list" -> {
                        plugin.arenaModule.arenaActions.arenaList(sender)
                        plugin.arenaModule.arenaActions.arenaList(sender)
                        return true
                    }
                    "scoreboard", "top" -> {
                        val p = sender as Player
                        val top = mutableListOf<Component>()
                        val i = AtomicInteger(1)

                        top.add(MiniMessage.miniMessage().deserialize("<yellow><bold>Top 10 players with the highest ELO:"))

                        plugin.playerManager.doublePlayers
                            .sortedByDescending { it.elo }
                            .take(10)
                            .forEach { ap ->
                                val playerName = Bukkit.getOfflinePlayer(ap.uuid).name
                                val elo = ap.elo

                                val medal = when (i.get()) {
                                    1 -> "<gold>"
                                    2 -> "<#C0C0C0>"
                                    3 -> "<#cd7f32>"
                                    else -> "<white>"
                                }

                                top.add(
                                    MiniMessage.miniMessage().deserialize(
                                        "$medal${i.getAndIncrement()} $playerName <dark_gray>- <gray>$elo ELO (${ap.wins + ap.losses} games)"
                                    )
                                )
                            }

                        top.forEach(p::sendMessage)
                        return true
                    }
                    "delete" -> {
                        plugin.arenaModule.arenaActions.arenaDelete(sender, args)
                        return true
                    }
                    "create" -> {
                        plugin.arenaModule.arenaActions.arenaCreate(sender, args)
                        return true
                    }
                    "sp1" -> {
                        plugin.arenaModule.arenaActions.arenaSP1(sender, args)
                        return true
                    }
                    "sp2" -> {
                        plugin.arenaModule.arenaActions.arenaSP2(sender, args)
                        return true
                    }
                    "public" -> {
                        plugin.arenaModule.arenaActions.arenaPublic(sender, args)
                        return true
                    }
                    else -> {
                        plugin.badUsage(sender as Player)
                        ArenaDefaultMessages.arenaHelp(sender)
                        return true
                    }
                }
            }
            "pvp" -> {
                if (!sender.hasPermission("double.pvp")) {
                    plugin.noPermission(sender as Player)
                    return true
                }

                if (args.isEmpty()) {
                    ArenaDefaultMessages.pvpHelp(sender)
                    return true
                }

                val doublePlayer = plugin.playerManager.getCeraiaPlayer((sender as Player).uniqueId)
                if (doublePlayer.isPvpBanned()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are banned from PvP!"))
                    return true
                }

                when (args[0].lowercase()) {
                    "accept" -> {
                        val p = sender
                        if (plugin.playerManager.getCeraiaPlayer(p.uniqueId).isPvpBanned()) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>This player is banned from PvP!"))
                            return true
                        }

                        val invite = plugin.arenaInviteManager.invites[p]

                        if (invite == null) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have any invites!"))
                            plugin.arenaInviteManager.invites.remove(p)
                            return true
                        }

                        if (!invite.invited.isOnline || !invite.inviter.isOnline) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found!"))
                            return true
                        }

                        if (invite.accepted) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You already accepted this invite!"))
                            return true
                        }

                        if (invite.arena?.state != Arena.ArenaState.WAITING ||
                            plugin.arenaModule.arenaManager.arenas.none { it.name.equals(invite.arena?.name, ignoreCase = true) }
                        ) {
                            listOf(invite.invited, invite.inviter).forEach { pl ->
                                pl.sendMessage(MiniMessage.miniMessage().deserialize("<red>Arena not found!"))
                            }
                            plugin.arenaInviteManager.invites.remove(p)
                            return true
                        }

                        val playersToFight = mutableListOf<Player>()

                        if (invite.group) {
                            val group1 = plugin.arenaModule.arenaActions.getPlayersByGroup(invite.inviter)
                            val group2 = plugin.arenaModule.arenaActions.getPlayersByGroup(invite.invited)

                            if (group1 == null || group2 == null) {
                                listOf(invite.invited, invite.inviter).forEach { pl ->
                                    pl.sendMessage(MiniMessage.miniMessage().deserialize("<red>Group not found!"))
                                }
                                plugin.arenaInviteManager.invites.remove(p)
                                return true
                            }

                            if (group1.size < 2 || group2.size < 2) {
                                listOf(invite.invited, invite.inviter).forEach { pl ->
                                    pl.sendMessage(MiniMessage.miniMessage().deserialize("<red>Group must have at least 2 players!"))
                                }
                                plugin.arenaInviteManager.invites.remove(p)
                                return true
                            }

                            val allPlayersAreReady = group1.all { plugin.arenaModule.arenaManager.getArena(it) == null } &&
                                    group2.all { plugin.arenaModule.arenaManager.getArena(it) == null }

                            if (!allPlayersAreReady) {
                                listOf(invite.invited, invite.inviter).forEach { pl ->
                                    pl.sendMessage(MiniMessage.miniMessage().deserialize("<red>One or more players are already in an arena!"))
                                }
                                plugin.arenaInviteManager.invites.remove(p)
                                return true
                            }

                            playersToFight.addAll(group1)
                            playersToFight.addAll(group2)

                            group1.forEach { invite.arena?.addPlayer(it, 1) }
                            group2.forEach { invite.arena?.addPlayer(it, 2) }
                        } else {
                            playersToFight.add(invite.invited)
                            playersToFight.add(invite.inviter)
                            invite.arena?.addPlayer(invite.invited, 1)
                            invite.arena?.addPlayer(invite.inviter, 2)
                        }

                        invite.accepted = true
                        invite.arena?.start(invite, playersToFight)

                        return true
                    }
                    else -> {
                        val playerName = args[0]

                        if (playerName.equals("reload", ignoreCase = true) && sender.isOp) {
                            plugin.reloadConfig()
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloaded!"))
                            return true
                        }

                        val invited = Bukkit.getPlayer(playerName)

                        if (invited == null) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found!"))
                            return true
                        }

                        val inviter = sender

                        if (inviter == invited) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You can't invite yourself!"))
                            return true
                        }

                        plugin.arenaSelectGUI.openArenaList(inviter, invited)
                        return true
                    }
                }
            }
            else -> return false
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        return when (cmd.name) {
            "arena" -> {
                when (args.size) {
                    1 -> StringUtil.copyPartialMatches(args[0], listOf("list", "scoreboard", "top", "delete", "create", "sp1", "sp2", "public"), ArrayList())
                    else -> emptyList()
                }
            }
            "pvp" -> {
                when (args.size) {
                    1 -> StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().map { it.name }, ArrayList())
                    else -> emptyList()
                }
            }
            "gvg" -> {
                when (args.size) {
                    1 -> StringUtil.copyPartialMatches(args[0], listOf("invite", "accept", "leave", "kick", "fight"), ArrayList())
                    2 -> {
                        if (args[0].equals("kick", ignoreCase = true) || args[0].equals("fight", ignoreCase = true) || args[0].equals("invite", ignoreCase = true)) {
                            StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().map { it.name }, ArrayList())
                        } else {
                            emptyList()
                        }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}