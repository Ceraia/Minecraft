package com.axodouble.modules;

import com.axodouble.Double;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ModuleSeating implements CommandExecutor, TabCompleter, Listener {

    private final Double plugin;
    public List<Chair> chairs;

    public ModuleSeating(Double plugin) {
        this.plugin = plugin;
        this.chairs = new ArrayList<>();

        Objects.requireNonNull(plugin.getCommand("sit")).setExecutor(this);

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Check every tick if the player is still sitting on the chair
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Iterate through the chairs
            Iterator<Chair> iterator = chairs.iterator();
            while (iterator.hasNext()) {
                Chair chair = iterator.next();
                if (chair.getEntity().getPassengers().isEmpty()) {
                    iterator.remove(); // Safely remove the current chair
                    chair.getEntity().remove();

                    Player player = chair.getPlayer();

                    // If the player is not sitting on anything, teleport them 0.5 blocks up
                    if(chair.isBlock()) {
                        player.teleport(player.getLocation().add(0, .5, 0));
                    }
                } else {
                    // Rotate the chair to the player's direction
                    Player player = chair.getPlayer();
                    Entity entity = chair.getEntity();
                    entity.setRotation(player.getLocation().getYaw(), 0);
                }
            }
        }, 0, 5);

        // Check every 5 seconds if there are any chairs that are not in the list
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Get all armorstands close to the player and check if they are in the chairs list
            // If they are not, remove them
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
                    if (entity instanceof ArmorStand) {
                        boolean found = false;
                        if (entity.getPassengers().size() > 0) {
                            Player passenger = (Player) entity.getPassengers().get(0);
                            this.chairs.add(new Chair(passenger, entity, false));
                            continue;
                        }
                        for (Chair chair : chairs) {
                            if (chair.getEntity().equals(entity)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            if (entity.getScoreboardTags().contains("chair")) {
                                entity.remove();
                            }
                        }
                    }
                }
            }
        }, 0, 5 * 20);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (!sender.hasPermission("double.sit")) {
            this.plugin.noPermission((Player) sender);
            return true;
        }

        sit(player, player.getLocation().add(0, -0.3, 0), false);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return new ArrayList<>();
    }

    public void sit(Player player, Location location, Boolean block) {
        // Check if the player is midair
        if (player.getLocation().add(0, -1, 0).getBlock().getType().isAir()) {
            return;
        }

        // Seat the player
        ArmorStand entity = player.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setSilent(true);
            armorStand.setAI(false);
            armorStand.setInvulnerable(true);
            armorStand.setSilent(true);
            armorStand.setHealth(20);
            armorStand.setAbsorptionAmount(1000);
            armorStand.addScoreboardTag("chair");
            armorStand.teleport(location.add(0, -1.6, 0));
            armorStand.addPassenger(player);
        });

        chairs.add(new Chair(player, entity, block));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        if (e.getPlayer().isSneaking()) {
            return;
        }
        if (!e.getPlayer().getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }

        if(!chairs.isEmpty()) {
            for (Chair chair : chairs) {
                if (chair.getPlayer().equals(e.getPlayer())) {
                   return;
                }
            }
        }

        // Only sit if right click was clicked
        if (e.getAction().isLeftClick()) {
            return;
        }

        Player player = e.getPlayer();

        if (e.getClickedBlock().getType().toString().contains("STAIRS") || (e.getClickedBlock().getType().toString().contains("SLAB"))) {
            e.setCancelled(true);
            sit(player, e.getClickedBlock().getLocation().add(0.5, 0.1, 0.5), true);
        }
    }

    public class Chair {
        private final Player player;
        private final Boolean block;
        private final Entity entity;

        public Chair(Player player, Entity entity, Boolean block) {
            this.player = player;
            this.entity = entity;
            this.block = block;
        }

        public Player getPlayer() {
            return player;
        }

        public Entity getEntity() {
            return entity;
        }

        public Boolean isBlock() {
            return block;
        }

        public void remove() {
            entity.remove();
            chairs.remove(this);
        }
    }
}
