package com.axodouble.modules.arena;

import com.axodouble.Double;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class ArenaEvents implements Listener {
    private final Double plugin;
    public ArenaEvents(Double plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    // If a block gets placed in the arena, add it to the list of placed blocks
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Arena arena = plugin.getArenaModule().arenaManager.getArena(e.getPlayer());
        if (arena == null) {
            return;
        }

        arena.placeBlock(e.getBlockPlaced().getLocation());
    }

    // If a block gets broken in the arena, remove it from the list of placed blocks
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Arena arena = plugin.getArenaModule().arenaManager.getArena(e.getPlayer());
        if (arena == null) {
            return;
        }

        if (arena.getPlacedBlocks().contains(e.getBlock().getLocation())) {
            arena.removeBlock(e.getBlock().getLocation());
            return;
        }

        e.setCancelled(true);
    }

    private boolean isInArena(Player player) {
        return plugin.getArenaModule().arenaManager.getArena(player) != null;
    }

    // TNT
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        Player source = null;
        if (e.getEntityType().equals(EntityType.TNT)) {
            TNTPrimed tnt = (TNTPrimed) e.getEntity();
            Entity entity = tnt.getSource();
            if (!(entity instanceof Player)) {
                return;
            }
            source = (Player) entity;
        }

        if (source == null || !isInArena(source)) {
            return;
        }
        e.blockList().clear();
    }

    // End crystal
    @EventHandler
    public void onHitCrystal(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();

        Player source;

        if (!(entity instanceof EnderCrystal)) {
            return;
        }

        if (damager instanceof Player) {
            source = (Player) damager;
        } else if (damager instanceof Arrow arrow) {
            Entity entity2 = (Entity) arrow.getShooter();
            if (!(entity2 instanceof Player)) {
                return;
            }
            source = (Player) entity2;
        } else {
            return;
        }

        if (!isInArena(source)) {
            return;
        }

        e.setCancelled(true);
        if (e.getEntity().isValid()) {
            e.getEntity().remove();
        }
        e.getEntity().getWorld().createExplosion(
                e.getEntity().getLocation(),
                6,
                false,
                false
        );
    }

    // Respawn Anchor
    @EventHandler
    public void onFillAnchor(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        if (!e.getClickedBlock().getType().equals(Material.RESPAWN_ANCHOR)) {
            return;
        }

        Block block = e.getClickedBlock();
        RespawnAnchor data = (RespawnAnchor) block.getBlockData();
        if (data.getCharges() < data.getMaximumCharges()) {
            return;
        }

        if (!isInArena(e.getPlayer())) {
            return;
        }

        e.setCancelled(true);
        block.setType(Material.AIR);
        block.getWorld().createExplosion(
                block.getLocation(),
                5,
                false,
                false
        );
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player player)) {
            return;
        }

        // If neither the damager nor the player are in an arena, return
        if (!isInArena(damager) && !isInArena(player)) {
            return;
        }

        // If the damager and the player are in different arenas, return
        if (((isInArena(damager) && !isInArena(player)) && (!isInArena(damager) && isInArena(player))) && !Objects.equals(plugin.getArenaModule().arenaManager.getArena(damager).getName(), plugin.getArenaModule().arenaManager.getArena(player).getName())) {
            e.setCancelled(true);
            return;
        }

        Arena arena = plugin.getArenaModule().arenaManager.getArena(damager);

        // If the arena is null, return
        if (arena == null) {
            return;
        }

        // If the arena is not running, return
        if (arena.getState() != Arena.ArenaState.RUNNING) {
            e.setCancelled(true);
            return;
        }

        // If the damager and the player are in the same team, return
        if (arena.getTeam1().contains(damager) && arena.getTeam1().contains(player)) {
            e.setCancelled(true);
            return;
        }

        // If the damager and the player are in the same team, return
        if (arena.getTeam2().contains(damager) && arena.getTeam2().contains(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }

        if (!isInArena(victim)) {
            return;
        }

        Arena arena = plugin.getArenaModule().arenaManager.getArena(victim);

        double healthAfter = victim.getHealth() - e.getFinalDamage();
        if (healthAfter <= 0) {
            // Check if during the fight totems are allowed
            if (arena.getTotems()) {
                if (
                        (victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) ||
                                (victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING)
                ) {
                    return;
                }
            }

            // #TODO: Fix ELO only being calculated when the player dies directly by another player
            if (e instanceof EntityDamageByEntityEvent) {
                if((((EntityDamageByEntityEvent) e).getDamager() instanceof Player killer)) {
                    // Do ELO calculations
                    ArenaActions.calculateElo(victim, killer);
                }
            }

            e.setCancelled(true);
            victim.setHealth(victim.getHealthScale());

            arena.end(victim, false);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!isInArena(e.getEntity())) {
            return;
        }
        Arena arena = plugin.getArenaModule().arenaManager.getArena(e.getEntity());

        Location loc = e.getEntity().getLocation();

        Player killer = e.getEntity().getKiller();

        if (killer == null) {
            killer = Bukkit.getPlayer(Objects.requireNonNull(Objects.requireNonNull(e.getEntity().getLastDamageCause()).getEntity().customName()).toString());
        }
        if (killer != null) {
            ArenaActions.calculateElo(e.getEntity(), killer);
        }


        e.getEntity().spigot().respawn();
        new BukkitRunnable() {
            @Override
            public void run() {
                e.getEntity().teleport(loc);
                arena.end(e.getEntity(), false);
            }
        }.runTaskLater(plugin, 5L);
        ArenaActions.updateScoreboard();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        ArenaActions.updateScoreboard();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (ArenaActions.playersByGroup.containsKey(player)) {
            ArenaActions.leaveGang(player);
        }

        if (!isInArena(e.getPlayer())) {
            return;
        }

        Arena arena = plugin.getArenaModule().arenaManager.getArena(e.getPlayer());

        // Check in which team the player is
        if (arena.getTeam1().contains(e.getPlayer())) {
            ArenaActions.calculateElo(e.getPlayer(), arena.getTeam2().get(0));
        } else {
            ArenaActions.calculateElo(e.getPlayer(), arena.getTeam1().get(0));
        }
        ArenaActions.updateScoreboard();
    }
}
