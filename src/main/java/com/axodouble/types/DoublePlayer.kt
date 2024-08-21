package com.axodouble.types

import com.axodouble.Double
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.UUID

class DoublePlayer(
    private val plugin: Double,
    val name: String,
    private var race: String,
    private var faction: String?,
    private var marriedName: String?,
    val uuid: UUID,
    var elo: Int,
    private var arenaBanned: Boolean,
    private var pvpBanned: Boolean,
    private var wins: Int,
    private var losses: Int,
    private val configFile: File
) {

    fun getUUID(): UUID = uuid

    fun togglePvpBan(): Boolean {
        pvpBanned = !pvpBanned
        savePlayer()
        return pvpBanned
    }

    fun toggleArenaBan(): Boolean {
        arenaBanned = !arenaBanned
        savePlayer()
        return arenaBanned
    }

    fun isPvpBanned(): Boolean = pvpBanned

    fun isArenaBanned(): Boolean = arenaBanned

    fun getWins(): Int = wins

    fun getLosses(): Int = losses

    fun addWin() {
        wins++
        savePlayer()
    }

    fun addLoss() {
        losses++
        savePlayer()
    }

    fun savePlayer() {
        val config: FileConfiguration = YamlConfiguration.loadConfiguration(configFile)
        config.set("name", name)
        config.set("race", race)
        config.set("married", marriedName)
        config.set("uuid", uuid.toString())
        config.set("elo", elo)
        config.set("arenabanned", arenaBanned)
        config.set("pvpbanned", pvpBanned)
        config.set("wins", wins)
        config.set("losses", losses)

        try {
            config.save(configFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun divorce() {
        marriedName = null
        savePlayer()
    }

    fun marry(name: String) {
        marriedName = name
        savePlayer()
    }

    fun getMarriedName(): String? = marriedName

    fun isMarried(): Boolean = marriedName != null

    fun getPartner(): String? = marriedName

    fun setFaction(faction: String) {
        this.faction = faction
        savePlayer()
    }

    fun setRace(race: String) {
        this.race = race
        savePlayer()
    }
}
