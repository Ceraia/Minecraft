package com.axodouble.modules.arena

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.type.RespawnAnchor
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class ArenaEvents(private val plugin: com.axodouble.Double) : Listener {
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    private fun isInArena(player: Player): Boolean {
        return plugin.arenaModule.arenaManager.isInArena(player)
    }

    // If a block gets placed in the arena, add it to the list of placed blocks
    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        val arena = plugin.arenaModule.arenaManager.getArena(e.player) ?: return

        arena.placeBlock(e.blockPlaced.location)
    }

    // If a block gets broken in the arena, remove it from the list of placed blocks
    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        val arena = plugin.arenaModule.arenaManager.getArena(e.player) ?: return

        if (arena.getPlacedBlocks().contains(e.block.location)) {
            arena.removeBlock(e.block.location)
            return
        }

        e.isCancelled = true
    }

    // TNT
    @EventHandler
    fun onEntityExplode(e: EntityExplodeEvent) {
        var source: Player? = null
        if (e.entityType == EntityType.TNT) {
            val tnt = e.entity as TNTPrimed
            val entity = tnt.source as? Player ?: return
            source = entity
        }

        if (source == null || !isInArena(source)) {
            return
        }
        e.blockList().clear()
    }

    // End crystal
    @EventHandler
    fun onHitCrystal(e: EntityDamageByEntityEvent) {
        val entity = e.entity
        val damager = e.damager

        val source: Player

        if (entity !is EnderCrystal) {
            return
        }

        when (damager) {
            is Player -> {
                source = damager
            }

            is Arrow -> {
                val entity2 = damager.shooter as Entity? as? Player ?: return
                source = entity2
            }

            else -> {
                return
            }
        }

        if (!isInArena(source)) {
            return
        }

        e.isCancelled = true
        if (e.entity.isValid) {
            e.entity.remove()
        }
        e.entity.world.createExplosion(
            e.entity.location,
            6f,
            false,
            false
        )
    }

    // Respawn Anchor
    @EventHandler
    fun onFillAnchor(e: PlayerInteractEvent) {
        if (e.clickedBlock == null) {
            return
        }
        if (e.clickedBlock!!.type != Material.RESPAWN_ANCHOR) {
            return
        }

        val block = e.clickedBlock
        val data = block!!.blockData as RespawnAnchor
        if (data.charges < data.maximumCharges) {
            return
        }

        if (!isInArena(e.player)) {
            return
        }

        e.isCancelled = true
        block.type = Material.AIR
        block.world.createExplosion(
            block.location,
            5f,
            false,
            false
        )
    }

    @EventHandler
    fun onHit(e: EntityDamageByEntityEvent) {
        if (e.damager !is Player || e.entity !is Player) {
            return
        }
        val damager = e.damager as Player
        val player = e.entity as Player

        // If neither the damager nor the player are in an arena, return
        if (!isInArena(damager) && !isInArena(player)) {
            return
        }

        // If the damager and the player are in different arenas, return
        if(plugin.arenaModule.arenaManager.getArena(damager) != plugin.arenaModule.arenaManager.getArena(player))
        {
            e.isCancelled = true
            return
        }

        val arena: Arena = plugin.arenaModule.arenaManager.getArena(damager) ?: return

        // If the arena is null, return

        // If the arena is not running, return
        if (arena.getState() != Arena.ArenaState.RUNNING) {
            e.isCancelled = true
            return
        }

        // If the damager and the player are in the same team, return
        if (arena.getTeam1().contains(damager) && arena.getTeam1().contains(player)) {
            e.isCancelled = true
            return
        }

        // If the damager and the player are in the same team, return
        if (arena.getTeam2().contains(damager) && arena.getTeam2().contains(player)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        if (e.entity !is Player) {
            return
        }

        val victim = e.entity as Player
        val killer = victim.killer

        if (!isInArena(victim)) {
            return
        }

        val arena: Arena? = plugin.arenaModule.arenaManager.getArena(victim)

        if (arena == null) {
            plugin.logger.warning("Player is in arena but arena is null")
            return
        }

        val healthAfter: Double = victim.health - e.finalDamage
        if (healthAfter <= 0) {
            // Check if during the fight totems are allowed
            if (arena.totems) {
                if ((victim.inventory.itemInMainHand.type == Material.TOTEM_OF_UNDYING) ||
                    (victim.inventory.itemInOffHand.type == Material.TOTEM_OF_UNDYING)
                ) {
                    return
                }
            }

            // #TODO: Fix ELO only being calculated when the player dies directly by another player
            if (e is EntityDamageByEntityEvent) {
                if ((e.damager is Player)) {
                    // Do ELO calculations
                    ArenaActions.calculateElo(victim, killer)
                }
            }

            e.isCancelled = true
            victim.health = victim.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0

            arena.end(victim, false)
        }
    }

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        if (!isInArena(e.entity)) {
            return
        }
        val arena = plugin.arenaModule.arenaManager.getArena(e.entity)

        val loc = e.entity.location

        var killer = e.entity.killer

        if (killer == null) {
            killer = Bukkit.getPlayer(
                Objects.requireNonNull(Objects.requireNonNull(e.entity.lastDamageCause)?.entity?.customName()).toString()
            )
        }
        if (killer != null) {
            ArenaActions.calculateElo(e.entity, killer)
        }


        e.entity.spigot().respawn()
        object : BukkitRunnable() {
            override fun run() {
                e.entity.teleport(loc)
                arena!!.end(e.entity, false)
            }
        }.runTaskLater(plugin, 5L)
        ArenaActions.updateScoreboard()
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent?) {
        ArenaActions.updateScoreboard()
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        val player = e.player
        if (ArenaActions.playersByGroup.containsKey(player)) {
            ArenaActions.leaveGang(player)
        }

        if (!isInArena(e.player)) {
            return
        }

        val arena = plugin.arenaModule.arenaManager.getArena(e.player)

        // Check in which team the player is
        if (arena!!.getTeam1().contains(e.player)) {
            ArenaActions.calculateElo(e.player, arena.getTeam2()[0])
        } else {
            ArenaActions.calculateElo(e.player, arena.getTeam1()[0])
        }
        ArenaActions.updateScoreboard()
    }

    @EventHandler
    fun onClickInventory(e: InventoryClickEvent) {
        val p = e.whoClicked as Player

        if (!isInArena(p)) {
            return
        }

        if (e.view.topInventory.type == InventoryType.PLAYER
            || e.view.topInventory.type ==
            InventoryType.CRAFTING
        ) {
            return
        }

        e.isCancelled = true
    }

    @EventHandler
    fun onDropItem(e: PlayerDropItemEvent) {
        if (isInArena(e.player)) {
            e.isCancelled = true
        }
    }
}
