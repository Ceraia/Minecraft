package com.axodouble.modules.arena

import com.axodouble.Double

class ArenaModule(private val plugin: Double) {
    @JvmField
    var arenaManager: ArenaManager
    var arenaEvents: ArenaEvents
    var arenaActions: ArenaActions

    init {
        ArenaCommandHandler(plugin)
        arenaEvents = ArenaEvents(plugin)
        arenaActions=ArenaActions(plugin)
        arenaManager = ArenaManager(plugin)
    }
}
