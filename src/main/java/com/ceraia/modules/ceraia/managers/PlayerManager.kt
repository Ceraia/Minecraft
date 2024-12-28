package com.ceraia.modules.ceraia.managers

import com.ceraia.Ceraia
import com.ceraia.modules.ceraia.types.CeraiaPlayer
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.IOException
import java.util.*

class PlayerManager(private val plugin: Ceraia) {
    val doublePlayers: MutableList<CeraiaPlayer> = mutableListOf()

    init {
        // Load arenaPlayers
        val dataFolder = File(plugin.dataFolder, "data/users")
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        dataFolder.listFiles()?.forEach { file ->
                val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)

            if(config.getString("uuid") == null) {

            }

            val doublePlayer = CeraiaPlayer(
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

    fun getCeraiaPlayer(playerUUID: UUID): CeraiaPlayer {
        return doublePlayers.find { it.uuid == playerUUID } ?: createNewCeraiaPlayer(playerUUID).also {
            doublePlayers.add(it)
        }
    }

    fun getCeraiaPlayer(playerName: String): CeraiaPlayer {
        return doublePlayers.find { it.name == playerName }
            ?: createNewCeraiaPlayer(Bukkit.getPlayer(playerName)?.uniqueId ?: throw IllegalArgumentException("Player not found"))
                .also { doublePlayers.add(it) }
    }

    fun getCeraiaPlayer(player: Player): CeraiaPlayer {
        return doublePlayers.find { it.uuid == player.uniqueId } ?: createNewCeraiaPlayer(player.uniqueId).also {
            doublePlayers.add(it)
        }
    }

    private fun createNewCeraiaPlayer(playerUUID: UUID): CeraiaPlayer {
        val playerName = Bukkit.getPlayer(playerUUID)?.name ?: throw IllegalArgumentException("Player not found")
        val configFile = File(plugin.dataFolder, "data/users/$playerUUID.yml")
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

            return CeraiaPlayer(
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
            return CeraiaPlayer(
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

    fun getPlayer(uniqueId: UUID): CeraiaPlayer? {
        return doublePlayers.find { it.uuid == uniqueId }
    }

    fun getPlayer(name: String): CeraiaPlayer? {
        return doublePlayers.find { it.name.equals(name, ignoreCase = true) }
    }

    fun savePlayers() {
        plugin.logger.info("Saving players...")
        doublePlayers.forEach { it.savePlayer() }
    }
}
