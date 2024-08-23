package com.axodouble.modules.arena

import com.axodouble.Ceraia

class ArenaModule(private val plugin: Ceraia) {
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
