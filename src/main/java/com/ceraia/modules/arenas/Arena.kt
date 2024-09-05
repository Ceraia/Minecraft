package com.ceraia.modules.arenas

import com.ceraia.Ceraia
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.UUID

class Arena private constructor(
    private val plugin: Ceraia,
    private val name: String,
    private val owner: UUID,
    private val breaking: Boolean,
    private val spawnpoints: List<Location>,
) {
    private val players: MutableMap<Player, Location> = mutableMapOf()
    private val placedBlocks: MutableList<Location> = mutableListOf()
    private val brokenBlocks: MutableList<Location> = mutableListOf()
    private val state: ArenaState = ArenaState.READY
    private val world: World = spawnpoints[0].world

    fun getPlugin(): Ceraia { return this.plugin }

    fun getName(): String { return this.name }

    fun getOwner(): UUID { return this.owner }

    fun getSpawnpoints(): List<Location> { return this.spawnpoints }

    fun breakingAllowed(): Boolean { return this.breaking }

    fun getPlayers(): MutableMap<Player, Location> { return this.players }

    fun getPlacedBlocks(): MutableList<Location> { return this.placedBlocks }

    fun getBrokenBlocks(): MutableList<Location> { return this.brokenBlocks }

    fun getState(): ArenaState { return this.state }

    fun addPlayer(player: Player, location: Location) {
        this.players[player] = location
    }

    fun removePlayer(player: Player) {
        this.players.remove(player)
    }

    fun addPlacedBlock(location: Location) {
        this.placedBlocks.add(location)
    }

    fun removePlacedBlocks() {
        this.placedBlocks.forEach {
                block -> block.block.type = Material.AIR
        }
        this.placedBlocks.clear()
    }

    class Builder {
        private lateinit var plugin: Ceraia
        private lateinit var name: String
        private lateinit var owner: UUID
        private var breaking: Boolean = false // Default to false
        private lateinit var spawnpoints: List<Location>

        fun plugin(plugin: Ceraia) = apply { this.plugin = plugin }
        fun name(name: String) = apply { this.name = name }
        fun owner(owner: UUID) = apply { this.owner = owner }
        fun breaking(breaking: Boolean) = apply { this.breaking = breaking }
        fun spawnpoints(spawnpoints: List<Location>) = apply { this.spawnpoints = spawnpoints }

        fun loadBuilder(): Arena {
            return Arena(plugin, name, owner, breaking, spawnpoints)
        }
    }

    companion object {
        fun builder() = Builder()
    }

    enum class ArenaState {
        READY,
        STARTING,
        INGAME,
        ENDING,
        ENDLESS
    }
}

class Test(){
    fun test(){
        Arena.builder()
    }
}