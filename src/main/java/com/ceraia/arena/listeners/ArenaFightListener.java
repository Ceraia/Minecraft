package com.ceraia.arena.listeners;

import com.ceraia.arena.Double;
import com.ceraia.arena.managers.InviteManager;
import com.ceraia.arena.types.Arena;
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

    private final Double plugin;

    public ArenaFightListener(Double plugin) {
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
            // If the damage is caused by a player
            if (event.getDamager() instanceof Player) {
                plugin.getLogger().info("Player");
                killer = (Player) event.getDamager();
            }
            // If the damage is caused by a projectile
            else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
                plugin.getLogger().info("Arrow");
                if (projectile.getShooter() instanceof Player) {
                    killer = (Player) projectile.getShooter();
                }
            }

            // If the damage is caused by a tnt
            else if (event.getDamager().getType() == org.bukkit.entity.EntityType.PRIMED_TNT) {
                plugin.getLogger().info("TNT");
                if (event.getDamager().customName() != null) {
                    killer = Bukkit.getPlayer(Objects.requireNonNull(event.getDamager().customName()).toString());
                }
            }
        }

        Arena arena = plugin.getArenaManager().getArena(player);

        double healthAfter = player.getHealth() - e.getFinalDamage();
        if (healthAfter <= 0) {

            // Check if during the fight totems are allowed

            InviteManager.Invite invite = plugin.getInviteManager().invites.get(player);

            if (invite == null) invite = plugin.getInviteManager().selectingInvites.get(killer);

            if (invite != null) {
                if (arena.totems) {
                    if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING || player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                        return;
                    }
                }
            }


            e.setCancelled(true);
            player.setHealth(player.getHealthScale());

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

        Player killer = e.getEntity().getKiller();

        if (killer == null) {
            killer = Bukkit.getPlayer(Objects.requireNonNull(Objects.requireNonNull(e.getEntity().getLastDamageCause()).getEntity().customName()).toString());
        }
        if (killer != null) {
            matchEnd(e.getEntity(), killer);
        }



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

        // Check in which team the player is
        if (arena.getTeam1().contains(e.getPlayer())) {
            matchEnd(e.getPlayer(), arena.getTeam2().get(0));
        } else {
            matchEnd(e.getPlayer(), arena.getTeam1().get(0));
        }

        arena.end(e.getPlayer(), true);
    }

    private void matchEnd(Player loser, Player winner) {
        UUID winnerUUID = winner.getUniqueId();
        UUID loserUUID = loser.getUniqueId();

        // Get the win chance
        int winChance = plugin.getPlayerManager().CalculateWinChance(winnerUUID, loserUUID);

        // Announce the winner and the win chance in chat
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.fight.end_global").replace("%winner%", winner.getName()).replace("%loser%", loser.getName()).replace("%elo%", String.valueOf(plugin.getPlayerManager().getDoublePlayer(loserUUID).getElo())).replace("%winchance%", String.valueOf(winChance)).replace("%arena%", plugin.getArenaManager().getArena(loser).getName()))

        );

        // Handle ELO calculations
        plugin.getPlayerManager().PlayerKill(winnerUUID, loserUUID);
    }
}
