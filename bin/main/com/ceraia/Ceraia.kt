package com.ceraia

import com.ceraia.managers.PlayerManager
import com.ceraia.modules.*
import com.ceraia.modules.arenas.ArenaModule
import com.ceraia.modules.races.ModuleRaces
import com.ceraia.modules.system.ModuleSystem
import com.ceraia.util.ConfigHelper
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.paper.PaperCommandManager
import java.io.File

class Ceraia : JavaPlugin() {
    private val plugin: Ceraia = this
    lateinit var playerManager: PlayerManager
        private set
    lateinit var moduleSeating: ModuleSeating
        private set
    lateinit var moduleMarriage: ModuleMarriage
        private set
    lateinit var moduleSystem: ModuleSystem
        private set
    lateinit var moduleRaces: ModuleRaces
        private set
    lateinit var configHelper: ConfigHelper
        private set
    lateinit var arenaModule: ArenaModule
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

        /*---------------------------------*/
        /*             Modules             */
        /*---------------------------------*/
        moduleSeating = ModuleSeating(plugin)
        moduleMarriage = ModuleMarriage(plugin)
        moduleSystem = ModuleSystem(plugin)
        moduleRaces = ModuleRaces(plugin)
        arenaModule = ArenaModule(plugin)

        /*---------------------------------*/
        /*             Helpers             */
        /*---------------------------------*/
        configHelper = ConfigHelper(plugin)
    }

    override fun onDisable() {
        playerManager.savePlayers()
    }

    fun noPermission(player: Player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to execute this command."))
    }
}
