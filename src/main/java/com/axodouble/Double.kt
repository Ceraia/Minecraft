package com.axodouble

import com.axodouble.listeners.OLDSpellsListener
import com.axodouble.modules.arena.ArenaInviteManager
import com.axodouble.managers.PlayerManager
import com.axodouble.modules.*
import com.axodouble.modules.arena.ArenaModule
import com.axodouble.modules.arena.ArenaSelectGUI
import com.axodouble.modules.races.ModuleRaces
import com.axodouble.modules.system.ModuleSystem
import com.axodouble.util.ConfigHelper
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Double : JavaPlugin() {
    private val plugin: Double = this
    lateinit var arenaInviteManager: ArenaInviteManager
        private set
    lateinit var arenaSelectGUI: ArenaSelectGUI
        private set
    lateinit var playerManager: PlayerManager
        private set
    lateinit var moduleSeating: ModuleSeating
        private set
    lateinit var moduleMarriage: ModuleMarriage
        private set
    lateinit var arenaModule: ArenaModule
        private set
    lateinit var moduleSystem: ModuleSystem
        private set
    lateinit var moduleRaces: ModuleRaces
        private set
    lateinit var configHelper: ConfigHelper
        private set

    override fun onEnable() {
        saveDefaultConfig()
        File(dataFolder, "data").mkdirs()
        File(dataFolder, "data/arenas").mkdirs()
        File(dataFolder, "data/items").mkdirs()
        File(dataFolder, "data/users").mkdirs()

        /*---------------------------------*/
        /*       Registering Managers      */
        /*---------------------------------*/
        playerManager = PlayerManager(plugin)
        arenaInviteManager = ArenaInviteManager()

        /*---------------------------------*/
        /*             Modules             */
        /*---------------------------------*/
        moduleSeating = ModuleSeating(plugin)
        moduleMarriage = ModuleMarriage(plugin)
        arenaModule = ArenaModule(plugin)
        moduleSystem = ModuleSystem(plugin)
        moduleRaces = ModuleRaces(plugin)

        /*---------------------------------*/
        /*               GUIs              */
        /*---------------------------------*/
        arenaSelectGUI = ArenaSelectGUI(plugin)

        /*---------------------------------*/
        /*            Listeners            */
        /*---------------------------------*/
        OLDSpellsListener(plugin)

        /*---------------------------------*/
        /*             Helpers             */
        /*---------------------------------*/
        configHelper = ConfigHelper(plugin)
    }

    override fun onDisable() {
        playerManager.savePlayers()
    }

    fun badUsage(player: Player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid usage."))
    }

    fun noPermission(player: Player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to execute this command."))
    }
}
