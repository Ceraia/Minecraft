package com.ceraia

import com.ceraia.arena.commands.arena.*
import com.ceraia.arena.commands.factions.CommandFaction
import com.ceraia.arena.commands.system.CommandMod
import com.ceraia.arena.commands.system.CommandVersion
import com.ceraia.arena.listeners.*
import com.ceraia.arena.managers.*
import com.ceraia.metrics.Metrics
import com.ceraia.arena.types.ArenaSelectGUI
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class Double : JavaPlugin() {
    var arenaManager: ArenaManager? = null
        private set
    var inviteManager: InviteManager? = null
        private set
    var arenaSelectGUI: ArenaSelectGUI? = null
        private set
    var groupManager: CommandGVG? = null
        private set
    var playerManager: PlayerManager? = null
        private set
    var metrics: Metrics? = null
    private var eloScoreBoardManager: EloScoreboardManager? = null
    var factionManager: FactionManager? = null
        private set

    override fun onEnable() {
        metrics = Metrics(this, 20303)

        saveDefaultConfig()
        File(dataFolder, "data/arenas").mkdirs()
        File(dataFolder, "data/items").mkdirs()
        File(dataFolder, "data/users").mkdirs()
        File(dataFolder, "data/factions").mkdirs()

        // Managers
        this.factionManager = FactionManager(this)
        this.arenaManager = ArenaManager(this)
        this.playerManager = PlayerManager(this)
        this.eloScoreBoardManager = EloScoreboardManager(this)
        this.inviteManager = InviteManager()

        this.arenaSelectGUI = ArenaSelectGUI(this)
        this.groupManager = CommandGVG(this)

        // Command
        val commandPVP = CommandPVP(this)
        val commandArena = CommandArena(this)
        val commandMod = CommandMod(this)
        val commandTop = CommandTop(this)
        val commandProfile = CommandProfile(this)
        val commandVersion = CommandVersion(this)
        val commandFaction = CommandFaction(this)

        // Listeners
        PlayerEloChangeListener(this)
        ArenaFightListener(this)
        PlayerInventoryListener(this)
        ArenaBlockListener(this)
        ArenaExplodeListener(this)
        SpellsListener(this)

        // PvP Commands
        Objects.requireNonNull(getCommand("pvp"))?.setExecutor(commandPVP)
        Objects.requireNonNull(getCommand("arena"))?.setExecutor(commandArena)
        Objects.requireNonNull(getCommand("gvg"))?.setExecutor(groupManager)
        Objects.requireNonNull(getCommand("top"))?.setExecutor(commandTop)
        Objects.requireNonNull(getCommand("leaderboard"))?.setExecutor(commandTop)
        Objects.requireNonNull(getCommand("profile"))?.setExecutor(commandProfile)
        Objects.requireNonNull(getCommand("stats"))?.setExecutor(commandProfile)

        // System Misc
        Objects.requireNonNull(getCommand("mod"))?.setExecutor(commandMod)
        Objects.requireNonNull(getCommand("version"))?.setExecutor(commandVersion)

        // Faction Commands
        Objects.requireNonNull(getCommand("faction"))?.setExecutor(commandFaction)
    }

    override fun onDisable() {
        metrics!!.shutdown()
        playerManager!!.savePlayers()
        factionManager!!.saveFactions()
    }
}
