package com.axodouble.modules.arena

import com.axodouble.Double
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

class ArenaSelectGUI(private val plugin: Double) : Listener {

    companion object {
        private val INVENTORY_NAME_ARENAS: Component = MiniMessage.miniMessage().deserialize("Select an arena")
        private val INVENTORY_NAME_TOTEMS: Component = MiniMessage.miniMessage().deserialize("Select totems")
        private val INVENTORY_NAME_RANKED: Component = MiniMessage.miniMessage().deserialize("Select whether to play ranked or not")
    }

    private val selectingArenaCache: MutableMap<Player, MutableMap<Int, Arena>> = mutableMapOf()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun openArenaList(inviter: Player, invited: Player) {
        val invite = ArenaInviteManager.Invite(inviter, invited)
        plugin.arenaInviteManager.selectingInvites[inviter] = invite

        val arenas = plugin.arenaModule.arenaManager.arenas
            .filter { it.isPublic || it.owner == inviter.name }

        val size = Math.max(9, (arenas.size + 8) / 9 * 9)
        val inv = Bukkit.createInventory(null, size, INVENTORY_NAME_ARENAS)

        val arenasSelectSlots = mutableMapOf<Int, Arena>()
        var i = 0

        for (arena in arenas.filter { it.state == Arena.ArenaState.WAITING }) {
            var itemStack = ItemStack(Material.ENDER_EYE)

            itemStack.editMeta { meta ->
                meta.displayName(MiniMessage.miniMessage().deserialize("<green>${arena.name}</green>"))
                meta.lore(listOf(MiniMessage.miniMessage().deserialize("<gray>Owner: ${arena.owner}")))
            }

            inv.setItem(i, itemStack)
            arenasSelectSlots[i] = arena
            i++
        }

        selectingArenaCache[inviter] = arenasSelectSlots
        inviter.openInventory(inv)
    }

    fun openTotemEnabled(inviter: Player, arena: Arena) {
        val size = 9
        val inv = Bukkit.createInventory(null, size, INVENTORY_NAME_TOTEMS)

        val itemStackEnable = ItemStack(Material.GREEN_STAINED_GLASS_PANE)
        itemStackEnable.editMeta { meta ->
            meta.displayName(MiniMessage.miniMessage().deserialize("<green>Enable totems</green>"))
            meta.lore(listOf(MiniMessage.miniMessage().deserialize("<gray>Click to enable totems in the fight")))
        }
        inv.setItem(1, itemStackEnable)

        val itemStackDisable = ItemStack(Material.RED_STAINED_GLASS_PANE)
        itemStackDisable.editMeta { meta ->
            meta.displayName(MiniMessage.miniMessage().deserialize("<red>Disable totems</red>"))
            meta.lore(listOf(MiniMessage.miniMessage().deserialize("<gray>Click to disable totems in the fight")))
        }
        inv.setItem(7, itemStackDisable)

        val itemStackArena = ItemStack(Material.ENDER_EYE)
        itemStackArena.editMeta { meta ->
            meta.displayName(MiniMessage.miniMessage().deserialize("<green>${arena.name}</green>"))
            meta.lore(listOf(MiniMessage.miniMessage().deserialize("<gray>Owner: ${arena.owner}")))
        }
        inv.setItem(4, itemStackArena)

        inviter.openInventory(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val currentItem = event.currentItem ?: return
        if (currentItem.type == Material.AIR) return
        if (event.inventory.type == InventoryType.PLAYER) return

        val inviter = event.whoClicked as Player
        when (event.view.title().toString()) {
            INVENTORY_NAME_ARENAS.toString() -> {
                event.isCancelled = true
                val slot = event.slot
                val arena = selectingArenaCache[inviter]?.get(slot)
                val invite = plugin.arenaInviteManager.selectingInvites[inviter]
                invite?.arena = arena

                if (arena == null || arena.state != Arena.ArenaState.WAITING) {
                    inviter.sendMessage(
                        MiniMessage.miniMessage().deserialize("<red>That arena is not available at the moment. Please try again later.")
                    )
                    return
                }

                openTotemEnabled(inviter, arena)
            }
            INVENTORY_NAME_TOTEMS.toString() -> {
                event.isCancelled = true
                val slot = event.slot
                val invite = plugin.arenaInviteManager.selectingInvites[inviter]
                val arena = invite?.arena
                arena?.totems = slot == 1

                inviter.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<green>Totems have been ${if (slot == 1) "enabled" else "disabled"} for the fight."
                    )
                )

                val totemsEnabled = if (arena?.totems == true) "<green>enabled</green>" else "<red>disabled</red>"
                val invitedPlayer = invite?.invited?.let { plugin.server.getPlayer(it.uniqueId) }
                invitedPlayer?.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<green>Click <white><hover:show_text:\"<green>Click to accept the pvp match!</green>\"><click:run_command:/pvp accept>[here]</click></hover> <green>to join PVP arena with ${inviter.name} in ${arena?.name}, totems are $totemsEnabled you have a ${plugin.playerManager.calculateWinChance(invite.invited.uniqueId, inviter.uniqueId)}% chance of winning!</green>"
                    )
                )

                if (invite != null) {
                    invite.arena = arena
                }
                plugin.arenaInviteManager.selectingInvites.remove(inviter)
                if (invite != null) {
                    plugin.arenaInviteManager.invites.put(invite.invited, invite)
                }
                inviter.closeInventory()
            }
            INVENTORY_NAME_RANKED.toString() -> {
                event.isCancelled = true
                val slot = event.slot
                val invite = plugin.arenaInviteManager.selectingInvites[inviter]
                val arena = invite?.arena
                arena?.totems = slot == 1

                inviter.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<green>Totems have been ${if (slot == 1) "enabled" else "disabled"} for the fight."
                    )
                )
            }
        }
    }
}