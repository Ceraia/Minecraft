package com.axodouble.modules.arena

import com.axodouble.Double
import com.axodouble.types.DoublePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class ArenaActions(private val plugin: Double) {
    val invites: MutableMap<Player, Player> = HashMap()
    val playersByGroup: MutableMap<Player, Player> = HashMap()
    val groups: MutableMap<Player, MutableList<Player>> = HashMap()


    fun arenaSP1(sender: CommandSender, args: Array<String>) {
        if (args.size == 1) {
            plugin.badUsage(sender as Player)
            return
        }

        val name = args[1]
        val arena = plugin.arenaModule.arenaManager.getArena(name)
        if (arena == null) {
            ArenaDefaultMessages.notFound(sender)
            return
        }
        if (arena.owner != sender.name) {
            ArenaDefaultMessages.notYours(sender)
            return
        }
        arena.setSpawnPoint1((sender as Player).location)
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Spawn point 1 set"))
    }

    fun arenaSP2(sender: CommandSender, args: Array<String>) {
        if (args.size == 1) {
            plugin.badUsage(sender as Player)
            return
        }

        val name = args[1]
        val arena = plugin.arenaModule.arenaManager.getArena(name)
        if (arena == null) {
            ArenaDefaultMessages.notFound(sender)
            return
        }
        if (arena.owner != sender.name) {
            ArenaDefaultMessages.notYours(sender)
            return
        }
        arena.setSpawnPoint2((sender as Player).location)
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Spawn point 2 set"))
    }

    fun arenaCreate(sender: CommandSender, args: Array<String>) {
        if (args.size == 1) {
            plugin.badUsage(sender as Player)
            return
        }

        val name = args[1]

        // Check if the user is banned from creating arenas
        if (plugin.playerManager.getDoublePlayer((sender as Player).uniqueId).isArenaBanned()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are banned from creating arenas"))
            return
        }

        // Check if the string is the same as <name>, if so state the user should put a name
        if (name.equals("<name>", ignoreCase = true)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid arena name"))
            return
        }

        // Check if the string is alphanumeric
        if (!name.matches("[a-zA-Z0-9]*".toRegex())) {
            sender.sendMessage(
                MiniMessage.miniMessage()
                    .deserialize("<red>Invalid arena name, it may only contain letters and numbers")
            )
            return
        }

        // Check if the arena already exists
        if (plugin.arenaModule.arenaManager.arenas.any { it.name.equals(name, ignoreCase = true) }) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Arena already exists"))
            return
        }

        val file = File(plugin.dataFolder, "data/arenas/$name.yml")
        val arena = Arena(plugin, name, sender.name, sender.location, sender.location, false, file)

        arena.setSpawnPoint1(sender.location)
        arena.setSpawnPoint2(sender.location)

        plugin.arenaModule.arenaManager.addArena(arena)
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Arena created"))
    }

    fun arenaDelete(sender: CommandSender, args: Array<String>) {
        if (args.size == 1) {
            plugin.badUsage(sender as Player)
            return
        }

        val name = args[1]
        val arena = plugin.arenaModule.arenaManager.getArena(name)
        if (arena == null) {
            ArenaDefaultMessages.notFound(sender)
            return
        }
        if (arena.owner != sender.name) {
            ArenaDefaultMessages.notYours(sender)
            return
        }
        if (arena.state != Arena.ArenaState.WAITING) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Arena is not empty"))
            return
        }

        arena.delete()
        plugin.arenaModule.arenaManager.removeArena(arena)
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Arena deleted"))
    }

    fun arenaPublic(sender: CommandSender, args: Array<String>) {
        if (args.size == 1) {
            plugin.badUsage(sender as Player)
            return
        }

        val name = args[1]
        val arena = plugin.arenaModule.arenaManager.getArena(name)
        if (arena == null) {
            ArenaDefaultMessages.notFound(sender)
            return
        }
        if (arena.owner != sender.name) {
            ArenaDefaultMessages.notYours(sender)
            return
        }

        val isPublic = arena.isPublic
        sender.sendMessage(
            MiniMessage.miniMessage().deserialize(
                if (isPublic) "<green>Arena made private" else "<green>Arena made public"
            )
        )

        arena.isPublic = !isPublic
    }

    fun arenaList(sender: CommandSender) {
        val arenas = plugin.arenaModule.arenaManager.arenas.filter { it.owner == sender.name }
        sender.sendMessage(
            MiniMessage.miniMessage().deserialize(
                "<yellow><bold>Your arenas:" +
                        arenas.joinToString("\n<green>") { it.name }
            )
        )
    }

    fun getPlayersByGroup(player: Player): List<Player>? {
        return groups[player]
    }

    fun calculateElo(loser: Player, winner: Player) {
        val winnerUUID = winner.uniqueId
        val loserUUID = loser.uniqueId

        // Get the win chance
        val winChance = plugin.playerManager.calculateWinChance(winnerUUID, loserUUID)

        Bukkit.broadcast(
            MiniMessage.miniMessage().deserialize(
                "<green>${winner.name} just killed ${loser.name} in the " +
                        "${plugin.arenaModule.arenaManager.getArena(loser)?.name} arena with a win chance of $winChance%!"
            )
        )

        // Handle ELO calculations
        plugin.playerManager.playerKill(winnerUUID, loserUUID)

        updateScoreboard()
    }

    fun leaveGang(player: Player) {
        val owner = playersByGroup[player]
        val group = groups[owner]

        if (owner == null || group == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are not in a group"))
            return
        }

        if (group.size <= 2) {
            group.forEach { pl ->
                pl.sendMessage(MiniMessage.miniMessage().deserialize("<red>Group has been disbanded"))
                playersByGroup.remove(pl)
            }
            groups.remove(owner)
        } else {
            group.forEach { p ->
                p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player ${player.name} has left the group"))
            }
            group.remove(player)
            groups[owner] = group // Now `owner` and `group` are non-null
            playersByGroup.remove(player)
        }
    }

    fun updateScoreboard() {
        // Update scoreboard
        val scoreboardManager = Bukkit.getScoreboardManager()
        val scoreboardDefault = scoreboardManager.newScoreboard
        val objectivePlayerList = scoreboardDefault.registerNewObjective(
            "eloObjectivePlayerList",
            "dummy",
            MiniMessage.miniMessage().deserialize("Top Arena Players")
        )
        val objectiveBelowName = scoreboardDefault.registerNewObjective(
            "eloObjectiveBelowName",
            "dummy",
            MiniMessage.miniMessage().deserialize("<green>ELO")
        )

        // Get all online players and set their score to their Elo rating
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            val doublePlayer = plugin.playerManager.getDoublePlayer(onlinePlayer.uniqueId)

            objectivePlayerList.getScore(onlinePlayer.name).score = doublePlayer.elo
            objectiveBelowName.getScore(onlinePlayer.name).score = doublePlayer.elo
            objectivePlayerList.displaySlot = DisplaySlot.PLAYER_LIST
            objectiveBelowName.displaySlot = DisplaySlot.BELOW_NAME

            onlinePlayer.scoreboard = scoreboardDefault
        }
    }

    fun leaderboard(sender: CommandSender) {
        val p = sender as Player

        // Create and show a string list of the top 10 players with the highest elo
        val top: MutableList<Component> = ArrayList()
        val i = AtomicInteger(1)

        top.add(MiniMessage.miniMessage().deserialize("<yellow><bold>Top 10 players with the highest ELO:"))

        plugin.playerManager.doublePlayers.sortedByDescending(DoublePlayer::elo).take(10).forEach { ap ->
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

        // Send the top 10 players with the highest elo to the player
        top.forEach(p::sendMessage)
    }

}
