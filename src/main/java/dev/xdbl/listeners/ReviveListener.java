package dev.xdbl.listeners;

import dev.xdbl.Double;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.util.Vector;

public class ReviveListener implements Listener {

    private final Double plugin;

    public ReviveListener(Double plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {

        Player player = e.getEntity().getPlayer();
        // Cancel the death event
        e.setCancelled(true);

        // Spawn an arrow that is clipped slightly into the ground and have the player sit on it
        Location arrowLocation = player.getLocation().clone();
        arrowLocation.setY(arrowLocation.getY() - 0.5); // Set the arrow slightly below ground level
        Arrow arrow = player.getWorld().spawnArrow(arrowLocation, new Vector(0, -1, 0), 0, 0); // Spawn the arrow

        // Make the player sit on the arrow
        arrow.addPassenger(player);

        // Add a scoreboard tag to the arrow to identify it as a revive arrow
        arrow.addScoreboardTag("revive");

        // Every tick for 20 seconds, set the player back on the arrow if they are not on the arrow
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!(arrow.isDead()) && !(arrow.getPassengers().contains(player))) {
                arrow.addPassenger(player);
            }
        }, 0, 1);

        // After 20 seconds, remove the arrow and teleport the player to the spawn location
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            arrow.remove();
        }, 20 * 20);
    }
}
