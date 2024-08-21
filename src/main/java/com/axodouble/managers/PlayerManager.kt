package com.axodouble.managers

import com.axodouble.Double
import com.axodouble.types.DoublePlayer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.ScoreboardManager
import java.io.File
import java.io.IOException
import java.util.*

class PlayerManager(private val plugin: Double) {

    private val doublePlayers: MutableList<DoublePlayer> = mutableListOf()

    init {
        // Load arenaPlayers
        val dataFolder = File(plugin.dataFolder, "data/users")
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        dataFolder.listFiles()?.forEach { file ->
                val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)
            val doublePlayer = DoublePlayer(
                    plugin,
                    config.getString("name") ?: throw IllegalArgumentException("Name cannot be null"),
                    config.getString("race", "human").toString(),
                    config.getString("faction"),
                    config.getString("married"),
                    UUID.fromString(config.getString("uuid") ?: throw IllegalArgumentException("UUID cannot be null")),
                    config.getInt("elo", 1500),
                    config.getBoolean("arenabanned", false),
                    config.getBoolean("pvpbanned", false),
                    config.getInt("wins", 0),
                    config.getInt("losses", 0),
                    file
            )
            doublePlayers.add(doublePlayer)
        }
    }

    fun getDoublePlayers(): List<DoublePlayer> = doublePlayers

    fun getDoublePlayer(playerUUID: UUID): DoublePlayer {
        return doublePlayers.find { it.uuid == playerUUID } ?: createNewDoublePlayer(playerUUID).also {
            doublePlayers.add(it)
        }
    }

    fun getDoublePlayer(playerName: String): DoublePlayer {
        return doublePlayers.find { it.name == playerName }
            ?: createNewDoublePlayer(Bukkit.getPlayer(playerName)?.uniqueId ?: throw IllegalArgumentException("Player not found"))
                .also { doublePlayers.add(it) }
    }

    fun getDoublePlayer(player: Player): DoublePlayer {
        return doublePlayers.find { it.uuid == player.uniqueId } ?: createNewDoublePlayer(player.uniqueId).also {
            doublePlayers.add(it)
        }
    }

    fun playerKill(playerKiller: UUID, playerVictim: UUID) {
        val killer = getDoublePlayer(playerKiller)
        val victim = getDoublePlayer(playerVictim)

        // Constants for the Elo calculation
        val kFactor = 32.0
        val expectedScoreKiller = 1.0 / (1.0 + Math.pow(10.0, (victim.elo - killer.elo) / 400.0))
        val expectedScoreVictim = 1.0 / (1.0 + Math.pow(10.0, (killer.elo - victim.elo) / 400.0))

        // Update Elo ratings
        val newEloKiller = (killer.elo + kFactor * (1.0 - expectedScoreKiller)).toInt()
        val newEloVictim = (victim.elo + kFactor * (0.0 - expectedScoreVictim)).toInt()

        // Set new Elo ratings
        killer.elo = newEloKiller
        victim.elo = newEloVictim

        // Update scoreboard
        val scoreboardManager: ScoreboardManager = Bukkit.getScoreboardManager()
        val scoreboardDefault: Scoreboard = scoreboardManager.newScoreboard
        val objectivePlayerList: Objective = scoreboardDefault.registerNewObjective("eloObjectivePlayerList", "dummy", MiniMessage.miniMessage().deserialize("Top Arena Players"))
        val objectiveBelowName: Objective = scoreboardDefault.registerNewObjective("eloObjectiveBelowName", "dummy", MiniMessage.miniMessage().deserialize("<green>ELO"))

        // Get all online players and set their score to their Elo rating
        Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
                val doublePlayer = plugin.playerManager.getDoublePlayer(onlinePlayer.uniqueId)
            objectivePlayerList.getScore(onlinePlayer.name).score = doublePlayer.elo
            objectiveBelowName.getScore(onlinePlayer.name).score = doublePlayer.elo
            objectivePlayerList.displaySlot = DisplaySlot.PLAYER_LIST
            objectiveBelowName.displaySlot = DisplaySlot.BELOW_NAME
            onlinePlayer.scoreboard = scoreboardDefault
        }

        // Update player stats
        killer.savePlayer()
        victim.savePlayer()
    }

    fun calculateWinChance(playerKiller: UUID, playerVictim: UUID): Int {
        val killer = getDoublePlayer(playerKiller)
        val victim = getDoublePlayer(playerVictim)

        val expectedScoreKiller = 1.0 / (1.0 + Math.pow(10.0, (victim.elo - killer.elo) / 400.0))
        return (expectedScoreKiller * 100).toInt()
    }

    fun calculateLossChance(playerKiller: UUID, playerVictim: UUID): Int {
        val killer = getDoublePlayer(playerKiller)
        val victim = getDoublePlayer(playerVictim)

        val expectedScoreVictim = 1.0 / (1.0 + Math.pow(10.0, (killer.elo - victim.elo) / 400.0))
        return (expectedScoreVictim * 100).toInt()
    }

    private fun createNewDoublePlayer(playerUUID: UUID): DoublePlayer {
        val playerName = Bukkit.getPlayer(playerUUID)?.name ?: throw IllegalArgumentException("Player not found")
        val configFile = File(plugin.dataFolder, "data/users/$playerName.yml")
        try {
            configFile.createNewFile()
            val config = YamlConfiguration.loadConfiguration(configFile)

            val defaultElo = 1500

            config.apply {
                set("name", playerName)
                set("race", "human")
                set("faction", null)
                set("married", null)
                set("uuid", playerUUID.toString())
                set("elo", defaultElo)
                set("arenabanned", false)
                set("pvpbanned", false)
                set("wins", 0)
                set("losses", 0)
                set("logs", mutableListOf<String>())
            }
            config.save(configFile)

            return DoublePlayer(
                    plugin,
                    playerName,
                    "human",
                    null,
                    null,
                    playerUUID,
                    defaultElo,
                    false,
                    false,
                    0,
                    0,
                    configFile
            )
        } catch (e: IOException) {
            e.printStackTrace()
            return DoublePlayer(
                    plugin,
                    playerName,
                    "human",
                    null,
                    null,
                    playerUUID,
                    1500,
                    false,
                    false,
                    0,
                    0,
                    configFile
            )
        }
    }

    fun getPlayer(uniqueId: UUID): DoublePlayer? {
        return doublePlayers.find { it.uuid == uniqueId }
    }

    fun getPlayer(name: String): DoublePlayer? {
        return doublePlayers.find { it.name.equals(name, ignoreCase = true) }
    }

    fun savePlayers() {
        plugin.logger.info("Saving players...")
        doublePlayers.forEach { it.savePlayer() }
    }
}
