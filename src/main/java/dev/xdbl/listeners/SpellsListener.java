package dev.xdbl.listeners;

import dev.xdbl.Double;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Objects;

public class SpellsListener implements Listener {

    private final Double plugin;

    public SpellsListener(Double plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Check if the player is casting a spell by right clicking with a book
    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (event.getItem().getType() == Material.BOOK) {
            Player p = event.getPlayer();
            if (p.getInventory().getItemInMainHand().getType() == Material.BOOK) {
                String name = Objects.requireNonNull(p.getInventory().getItemInMainHand().getItemMeta()).getDisplayName();
                switch (name) {
                    case "Fireflies" -> {
                        // Launch snowballs that light the person it hits on fire
                        event.setCancelled(true);
                        p.sendMessage("You casted Fireflies!");
                        p.launchProjectile(SmallFireball.class);
                    }
                    case "Getauttahere" -> {
                        event.setCancelled(true);
                        Trident trident = p.launchProjectile(Trident.class);
                        trident.setPickupStatus(Trident.PickupStatus.DISALLOWED); // Prevents Trident from being picked up
                        trident.addScoreboardTag("getauttahere"); // Add a tag to the Trident to identify it later
                        trident.addPassenger(p);
                        p.sendMessage("You casted Getauttahere!");
                    }
                    case "Bidenblast" -> {
                        event.setCancelled(true);
                        for (int i = 0; i < 1; i++) {
                            Arrow arrow = p.launchProjectile(Arrow.class);
                            arrow.setVelocity(p.getLocation().getDirection().multiply(10));
                            arrow.addScoreboardTag("explosive");
                        }
                    }
                    case "Kamikazesheep" -> {
                        event.setCancelled(true);
                        p.launchProjectile(Egg.class).addScoreboardTag("kamikazesheep");
                        p.sendMessage("You casted Kamikazesheep!");
                    }
                    case "Earfquake" -> {
                        event.setCancelled(true);
                        p.launchProjectile(Egg.class).addScoreboardTag("earfquake");
                    }
                    case "Earfffquake" -> {
                        event.setCancelled(true);
                        p.launchProjectile(Egg.class).addScoreboardTag("earfffquake");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            if (arrow.getScoreboardTags().contains("explosive")) {
                arrow.getWorld().createExplosion(arrow.getLocation(), 100, true, true);
                arrow.remove();
            }
        } else if (event.getEntity() instanceof Trident trident) {
            if (trident.getScoreboardTags().contains("getauttahere")) {
                trident.remove();
            }
        } else if (event.getEntity() instanceof Egg egg) {
            if (egg.getScoreboardTags().contains("kamikazesheep")) {
                // Get the closest player to the egg
                Player p = null;
                double distance = 100;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getLocation().distance(egg.getLocation()) < distance) {
                        p = player;
                        distance = player.getLocation().distance(egg.getLocation());
                    }
                }

                Sheep sheep = egg.getWorld().spawn(egg.getLocation(), Sheep.class);

                TNTPrimed tnt = egg.getWorld().spawn(egg.getLocation(), TNTPrimed.class);
                tnt.setFuseTicks(300);


                sheep.addPassenger(tnt);
                sheep.addScoreboardTag("kamikaze");
                sheep.setCustomNameVisible(true);
                sheep.customName(MiniMessage.miniMessage().deserialize("jeb_"));
                sheep.setInvulnerable(true);

                sheep.addPotionEffect(
                        new org.bukkit.potion.PotionEffect(
                                PotionEffectType.SPEED, 1000000, 2, false, false
                        )
                );

                egg.remove();

                Player finalP = p;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Make the sheep walk towards the player
                        assert finalP != null;
                        sheep.getPathfinder().moveTo(finalP.getLocation());
                        if (sheep.getPassengers().size() == 0) {
                            sheep.remove();
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            } else if (egg.getScoreboardTags().contains("earfquake")) {
                egg.getWorld().createExplosion(egg.getLocation(), 4, false, false); // Adjust power as needed

                // Get the blocks in the affected area
                int radius = 10; // Adjust as needed
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (Math.sqrt(x * x + y * y + z * z) <= radius) {
                                Block block = egg.getLocation().clone().add(x, y, z).getBlock();
                                if (block.getType() != Material.AIR) {
                                    // Spawn falling block entities at the location of each block
                                    FallingBlock fallingBlock = egg.getWorld().spawnFallingBlock(block.getLocation(), block.getBlockData());
                                    // Apply velocity to simulate launch effect
                                    fallingBlock.setVelocity(new Vector(
                                            (Math.random() - 0.5) * 2,
                                            Math.random() + .2,
                                            (Math.random() - 0.5) * 2
                                    ));
                                    // Remove the original block
                                    block.setType(Material.AIR);
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        this.plugin.getLogger().info("Player joined");
        this.plugin.getLogger().info(event.getPlayer().getUniqueId().toString());
    }
}
