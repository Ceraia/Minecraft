package dev.xdbl.xdblarenas.listeners;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.arenas.Arena;
import dev.xdbl.xdblarenas.managers.InviteManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

import java.util.Objects;
import java.util.UUID;

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

        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player player)) {
            return;
        }

        if (!isInArena(damager) && !isInArena(player)) {
            return;
        }

        if (((isInArena(damager) && !isInArena(player)) || (!isInArena(damager) && isInArena(player))) || !Objects.equals(plugin.getArenaManager().getArena(damager).getName(), plugin.getArenaManager().getArena(player).getName())) {
            e.setCancelled(true);
            return;
        }

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
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        if (!isInArena(player)) {
            return;
        }

        // Get player that hurt the player
        Player killer = null;
        if (e instanceof EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof Player) {
                killer = (Player) event.getDamager();
            }
        }

        Arena arena = plugin.getArenaManager().getArena(player);

        double healthAfter = player.getHealth() - e.getFinalDamage();
        if (healthAfter <= 0) {

            // Check if during the fight totems are allowed

            InviteManager.Invite invite = plugin.getInviteManager().invites.get(player);

            if(invite == null) invite = plugin.getInviteManager().selectingInvites.get(killer);

            if (invite != null) {
                if (arena.totems) {
                    if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING || player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                        return;
                    }
                }
            }


            e.setCancelled(true);
            player.setHealth(player.getHealthScale());

            // END OF FIGHT
            if (killer != null) {
                UUID killerUUID = killer.getUniqueId();
                UUID victimUUID = player.getUniqueId();

                // Get the win chance
                int winChance = plugin.getPlayerManager().CalculateWinChance(killerUUID, victimUUID);

                // Announce the winner and the win chance in chat
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                        plugin.getConfig().getString("messages.fight.end_global")
                                .replace("%winner%", killer.getName())
                                .replace("%loser%", player.getName())
                                .replace("%elo%", String.valueOf(plugin.getPlayerManager().getArenaPlayer(victimUUID).getElo()))
                                .replace("%winchance%", String.valueOf(winChance))
                                .replace("%arena%", plugin.getArenaManager().getArena(player).getName()))

                );

                // Handle ELO calculations
                plugin.getPlayerManager().PlayerKill(killerUUID, victimUUID);
            }
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
