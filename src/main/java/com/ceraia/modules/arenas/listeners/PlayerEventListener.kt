package com.ceraia.modules.arenas.listeners

import com.ceraia.modules.ceraia.types.CeraiaPlayer
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerEventListener(val player: Player, val arenaPlayer: CeraiaPlayer) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        val handlerList: HandlerList = HandlerList()
    }
}
