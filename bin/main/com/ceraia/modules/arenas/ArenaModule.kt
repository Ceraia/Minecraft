package com.ceraia.modules.arenas

import com.ceraia.Ceraia

class ArenaModule(private val plugin: Ceraia) {
    init {
        val arenaCommands = ArenaCommands(plugin)
        plugin.getCommand("arena")?.setExecutor(arenaCommands::onCommand)
        plugin.getCommand("arena")?.setTabCompleter(arenaCommands::onTabComplete)
    }
}