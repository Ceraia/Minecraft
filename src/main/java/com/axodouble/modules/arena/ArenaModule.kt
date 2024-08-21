package com.axodouble.modules.arena

import com.axodouble.Double

class ArenaModule(private val plugin: Double) {
    @JvmField
    var arenaManager: ArenaManager

    init {
        ArenaCommandHandler(plugin)
        ArenaEvents(plugin)
        ArenaActions(plugin)
        arenaManager = ArenaManager(plugin)
    }
}
