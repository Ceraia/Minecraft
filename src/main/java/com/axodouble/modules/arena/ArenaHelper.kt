package com.axodouble.modules.arena

import com.axodouble.Double
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import java.io.File

object ArenaHelper {

    @JvmStatic
    fun revertInventory( plugin: Double, player: Player,  arena: Arena) {
        try {
            val file = File(plugin.dataFolder, "data/pinventory_" + arena.name + "_" + player.name + ".yml")

            val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)

            val content = arrayOfNulls<ItemStack>(player.inventory.contents.size)

            try {
                config.getConfigurationSection("items")?.getKeys(false)?.forEach { s ->
                    val i = s.toInt()
                    content[i] = config.getItemStack("items.$s")
                }
            } catch (e: Exception) {
                println("Problem loading player inventories.")
            }

            player.inventory.contents = content

            file.delete()
        } catch ( e: Exception) {
            e.printStackTrace()
            println("Problem loading player inventory")
        }
    }

    @JvmStatic
    fun teleportPlayerToSpawn(  player: Player,  arena: Arena) {
        val l: Location? = arena.getPlayerPriorLocation(player)
        l?.let { player.teleport(it) }
    }
}
