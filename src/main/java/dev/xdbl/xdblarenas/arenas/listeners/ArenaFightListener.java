package dev.xdbl.xdblarenas.arenas.listeners;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.arenas.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ArenaFightListener implements Listener {

    private final XDBLArena plugin;

    public ArenaFightListener(XDBLArena plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isInArena(Player player) {
        return plugin.getArenaManager().getArena(player) != null;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() == null || e.getEntity() == null) {
            return;
        }

        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getEntity();
        Player damager = (Player) e.getDamager();

        Arena arena = plugin.getArenaManager().getArena(damager);

        if (arena == null) {
            return;
        }

        if (arena.getState() != Arena.ArenaState.RUNNING) {
            e.setCancelled(true);
            return;
        }

        if (arena.getTeam1().contains(damager) && arena.getTeam1().contains(player)) {
            e.setCancelled(true);
            return;
        }

        if (arena.getTeam2().contains(damager) && arena.getTeam2().contains(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getEntity();

        if (!isInArena(player)) {
            return;
        }

        double healthAfter = player.getHealth() - e.getFinalDamage();
        if (healthAfter <= 0) {
            if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                    player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                return;
            }
            
            e.setCancelled(true);
            player.setHealth(player.getHealthScale());

            // END OF FIGHT

            Arena arena = plugin.getArenaManager().getArena(player);

            arena.end(player, false);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!isInArena(e.getEntity())) {
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(e.getEntity());

        Location loc = e.getEntity().getLocation();

        e.getEntity().spigot().respawn();
        new BukkitRunnable() {
            @Override
            public void run() {
                e.getEntity().teleport(loc);
                arena.end(e.getEntity(), false);
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!isInArena(e.getPlayer())) {
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(e.getPlayer());
        arena.end(e.getPlayer(), true);
    }
}
