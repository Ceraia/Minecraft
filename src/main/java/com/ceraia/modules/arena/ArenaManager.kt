package com.ceraia.modules.arena

import com.axodouble.Double
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

class ArenaManager(private val plugin: Double) {

    val arenas: MutableList<Arena> = mutableListOf()
    private val playersInArena: MutableMap<Player, Arena> = mutableMapOf()

    init {
        // Load arenas
        val dataFolder = File(plugin.dataFolder, "data/arenas")
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        dataFolder.listFiles()?.forEach { file ->
            val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)

            val name = config.getString("name") ?: return@forEach
            val owner = config.getString("owner") ?: return@forEach
            val spawnPoint1 = config.getLocation("spawnPoint1")
            val spawnPoint2 = config.getLocation("spawnPoint2")
            val isPublic = config.getBoolean("public", false)

            if(spawnPoint1 == null || spawnPoint2 == null) {
                plugin.logger.warning("Arena $name has invalid spawn points, skipping...")
                return@forEach
            }

            val arena = Arena(plugin, name, owner, spawnPoint1, spawnPoint2, isPublic, file)
            arenas.add(arena)
        }
    }

    fun getArena(player: Player): Arena? = playersInArena[player]

    fun isInArena(player: Player): Boolean {
        return plugin.arenaModule.arenaManager.getArena(player) != null
    }

    fun getArena(name: String): Arena? {
        return arenas.find { it.name.equals(name, ignoreCase = true) }
    }

    fun addArena(arena: Arena) {
        arenas.add(arena)
        arena.saveArena()
    }

    fun removeArena(arena: Arena) {
        arenas.remove(arena)
    }

    fun addPlayerToArena(player: Player, arena: Arena) {
        playersInArena[player] = arena
    }

    fun removePlayerFromArena(player: Player) {
        playersInArena.remove(player)
    }
}
