package com.axodouble.modules.arena

import org.bukkit.entity.Player

class ArenaInviteManager {
    @JvmField
    var invites: MutableMap<Player, Invite> = HashMap()
    var selectingInvites: MutableMap<Player, Invite> = HashMap()

    class Invite(@JvmField var inviter: Player, @JvmField var invited: Player) {
        @JvmField
        var arena: Arena? = null
        @JvmField
        var accepted: Boolean = false
        @JvmField
        var group: Boolean = false
    }
}