package com.axodouble.types

import com.axodouble.Double
import com.axodouble.util.ArenaHelper
import com.axodouble.managers.InviteManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class Arena(
    private val plugin: Double,
    val name: String,
    val owner: String,
    private var spawnPoint1: Location,
    private var spawnPoint2: Location,
    var isPublic: Boolean,
    private var configFile: File
) {
    private val startPlayers = mutableListOf<Player>()
    private val placedBlocks = mutableListOf<Location>()
    private val priorLocations = mutableMapOf<Player, Location>()
    private var state: ArenaState = ArenaState.WAITING
    var totems = false
    private val team1 = mutableListOf<Player>()
    private val team2 = mutableListOf<Player>()

    fun saveArena() {
        try {
            configFile = File(plugin.dataFolder, "data/arenas/$name.yml")

            if (!configFile.exists()) {
                configFile.createNewFile()
            }

            val config = YamlConfiguration.loadConfiguration(configFile)
            config.set("name", name)
            config.set("owner", owner)
            config.set("spawnPoint1", spawnPoint1)
            config.set("spawnPoint2", spawnPoint2)
            config.set("public", isPublic)

            config.save(configFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setTotems(totems: Boolean, save: Boolean = false) {
        this.totems = totems
        if (save) saveArena()
    }

    fun getState(): ArenaState = state

    fun setState(state: ArenaState) {
        this.state = state
    }

    fun delete() {
        configFile.delete()
    }

    fun getSpawnPoint1(): Location = spawnPoint1

    fun setSpawnPoint1(loc: Location) {
        spawnPoint1 = loc
        saveArena()
    }

    fun getSpawnPoint2(): Location = spawnPoint2

    fun setSpawnPoint2(loc: Location) {
        spawnPoint2 = loc
        saveArena()
    }

    fun getTeam1(): List<Player> = team1

    fun setTeam1(team1: List<Player>) {
        this.team1.clear()
        this.team1.addAll(team1)
    }

    fun getTeam2(): List<Player> = team2

    fun setTeam2(team2: List<Player>) {
        this.team2.clear()
        this.team2.addAll(team2)
    }

    fun addPlayer(player: Player, team: Int) {
        plugin.arenaManager.addPlayerToArena(player, this)
        if (team == 1) {
            team1.add(player)
        } else {
            team2.add(player)
        }
        startPlayers.add(player)
    }

    fun getStartPlayers(): List<Player> = startPlayers

    fun getOnlinePlayers(): List<Player> = team1 + team2

    fun reset() {
        team1.clear()
        team2.clear()
        startPlayers.clear()
        placedBlocks.clear()
        priorLocations.clear()
    }

    fun end(player: Player, quit: Boolean) {
        var end = false
        val winners = mutableListOf<Player>()
        val losers = mutableListOf<Player>()

        if (team1.contains(player)) {
            if (team1.size <= 1) {
                end = true
                winners.addAll(team2)
                losers.addAll(team1)
            } else {
                team1.remove(player)
                setTeam1(team1)
            }
        } else {
            if (team2.size <= 1) {
                end = true
                winners.addAll(team1)
                losers.addAll(team2)
            } else {
                team2.remove(player)
                setTeam2(team2)
            }
        }

        if (!end || quit) {
            ArenaHelper.teleportPlayerToSpawn(player, this)
            plugin.arenaManager.removePlayerFromArena(player)
            player.inventory.clear()
            ArenaHelper.revertInventory(plugin, player, this)
            if (!end) return
        }

        for (pl in getOnlinePlayers()) {
            if (pl == player && quit) continue

            pl.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<green>${winners.joinToString(", ") { it.name }} just killed ${losers.joinToString(", ") { it.name }} in the ${name} arena with a win chance of ${plugin.playerManager.calculateWinChance(winners.first().uniqueId, losers.first().uniqueId)}%!"
                    )
            )

            pl.inventory.clear()
            pl.health = 20.0
            pl.fireTicks = 0
            pl.foodLevel = 20
            pl.saturation = 20.0F
        }

        setState(ArenaState.ENDING)

        val thisArena = this
        object : BukkitRunnable() {
            override fun run() {
                for (loc in placedBlocks) {
                    loc.block.type = Material.AIR
                }

                for (pl in getOnlinePlayers()) {
                    if (pl == player && quit) continue
                    ArenaHelper.teleportPlayerToSpawn(pl, thisArena)
                    plugin.arenaManager.removePlayerFromArena(pl)
                    ArenaHelper.revertInventory(plugin, pl, thisArena)
                }

                // Reward
                for (pl in winners) {
                    plugin.playerManager.getDoublePlayer(pl.uniqueId).addWin()
                }

                // Reward losers
                for (pl in losers) {
                    plugin.playerManager.getDoublePlayer(pl.uniqueId).addLoss()
                }

                setState(ArenaState.WAITING)
                reset()
            }
        }.runTaskLater(plugin, 5 * 20L)
    }

    fun start(invite: InviteManager.Invite, players: List<Player>) {
        setState(ArenaState.STARTING)

        try {
            for (pl in players) {
                priorLocations[pl] = pl.location

                val content = pl.inventory.contents
                val file = File(plugin.dataFolder, "data/pinventory_${name}_${pl.name}.yml")
                file.createNewFile()

                val yaml = YamlConfiguration.loadConfiguration(file)
                content.forEachIndexed { i, itemStack ->
                        yaml.set("items.$i", itemStack ?: "null")
                }
                yaml.save(file)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            println("Problem saving inventories, nothing was deleted!")
            players.forEach { pl ->
                    pl.sendMessage(MiniMessage.miniMessage().deserialize("<red>Problem saving inventories, nothing was deleted!"))
            }
            return
        }

        team1.forEach { it.teleport(spawnPoint1) }
        team2.forEach { it.teleport(spawnPoint2) }

        players.forEach {
            it.apply {
                health = it.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                foodLevel = 20
                saturation = 20.0F
                gameMode = GameMode.SURVIVAL
            }
        }

        val i = AtomicInteger(6)
        val thisArena = this

        object : BukkitRunnable() {
            override fun run() {
                for (pl in players) {
                    val count = i.get()
                    if (count == 0) {
                        pl.showTitle(Title.title(Component.empty(), Component.empty()))
                    } else if (count == 1) {
                        val title = Title.title(
                                MiniMessage.miniMessage().deserialize("<green>Starting in ${count - 1}"),
                                Component.empty()
                        )
                        pl.showTitle(title)
                    } else {
                        pl.showTitle(Title.title(
                                MiniMessage.miniMessage().deserialize("<green>${count - 1}"),
                                MiniMessage.miniMessage().deserialize(
                        if (plugin.arenaManager.getArena(pl)?.totems == true)
                        "<red>Totems have been enabled for the fight."
                                else
                        "<green>Totems have been disabled for the fight."
                            )
                        ))
                    }
                }

                if (i.get() == 0) {
                    setState(ArenaState.RUNNING)
                    cancel()
                }

                i.decrementAndGet()
            }
        }.runTaskTimer(plugin, 0, 20)
    }

    fun placeBlock(loc: Location) {
        placedBlocks.add(loc)
    }

    fun removeBlock(loc: Location) {
        placedBlocks.remove(loc)
    }

    fun getPlacedBlocks(): List<Location> = placedBlocks

    fun getPlayerPriorLocation(pl: Player): Location? = priorLocations[pl]

    enum class ArenaState {
        WAITING, STARTING, RUNNING, ENDING
    }
}
