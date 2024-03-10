package dev.xdbl.managers;

import dev.xdbl.Double;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChairManager implements Listener {

    private final Double plugin;
    public List<Chair> chairs;

    public ChairManager(Double plugin) {
        this.plugin = plugin;
        this.chairs = new ArrayList<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Check every tick if the player is still sitting on the chair
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Chair> iterator = chairs.iterator();
            while (iterator.hasNext()) {
                Chair chair = iterator.next();
                if (chair.getEntity().getPassengers().isEmpty()) {
                    iterator.remove(); // Safely remove the current chair
                    chair.getEntity().remove();

                    Player player = chair.getPlayer();
                    // If the player is not sitting on anything, teleport them 1 block up
                    player.teleport(player.getLocation().add(0, 1, 0));
                } else {
                    // Rotate the chair to the player's direction
                    Player player = chair.getPlayer();
                    Entity entity = chair.getEntity();
                    entity.setRotation(player.getLocation().getYaw(), 0);
                }
            }
        }, 0, 5);
    }

    public void sit(Player player, Location location) {
        ArmorStand entity = player.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setSilent(true);
            armorStand.setAI(false);
            armorStand.setInvulnerable(true);
            armorStand.setSilent(true);
            armorStand.setHealth(20);
            armorStand.setAbsorptionAmount(1000);
            armorStand.teleport(location.add(0, -1.6, 0));
            armorStand.addPassenger(player);
        });

        chairs.add(new Chair(player, entity));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        if (e.getPlayer().isSneaking()) {
            return;
        }
        if (e.getClickedBlock().getType().toString().contains("STAIRS") || e.getClickedBlock().getType().toString().contains("SLAB")) {
            sit(e.getPlayer(), e.getClickedBlock().getLocation().add(0.5, 0, 0.5));
        }
    }

    public class Chair {
        private final Player player;
        private final Entity entity;

        public Chair(Player player, Entity entity) {
            this.player = player;
            this.entity = entity;
        }

        public Player getPlayer() {
            return player;
        }

        public Entity getEntity() {
            return entity;
        }

        public void remove() {
            entity.remove();
            chairs.remove(this);
        }
    }
}
