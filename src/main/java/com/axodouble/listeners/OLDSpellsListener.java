package com.axodouble.listeners;

import com.axodouble.Double;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class OLDSpellsListener implements Listener {

    private final Double plugin;

    public OLDSpellsListener(Double plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Check if the player is casting a spell by right-clicking with a book
//    @EventHandler
//    public void onPlayerRightClick(PlayerInteractEvent event) {
//        return;
//        if (event.getItem() == null) return;
//        if (event.getItem().getType() == Material.BOOK) {
//            Player p = event.getPlayer();
//            if (p.getInventory().getItemInMainHand().getType() == Material.BOOK) {
//                String name = Objects.requireNonNull(p.getInventory().getItemInMainHand().getItemMeta()).getDisplayName();
//                switch (name) {
//                    case "Fireflies" -> {
//                        // Launch snowballs that light the person it hits on fire
//                        event.setCancelled(true);
//                        p.sendMessage("You casted Fireflies!");
//                        p.launchProjectile(SmallFireball.class);
//                    }
//                    case "Getauttahere" -> {
//                        event.setCancelled(true);
//                        Trident trident = p.launchProjectile(Trident.class);
//                        trident.setPickupStatus(Trident.PickupStatus.DISALLOWED); // Prevents Trident from being picked up
//                        trident.addScoreboardTag("getauttahere"); // Add a tag to the Trident to identify it later
//                        trident.addPassenger(p);
//                        p.sendMessage("You casted Getauttahere!");
//                    }
//                    case "Bidenblast" -> {
//                        event.setCancelled(true);
//                        for (int i = 0; i < 1; i++) {
//                            Arrow arrow = p.launchProjectile(Arrow.class);
//                            arrow.setVelocity(p.getLocation().getDirection().multiply(10));
//                            arrow.addScoreboardTag("explosive");
//                        }
//                    }
//                    case "Kamikazesheep" -> {
//                        event.setCancelled(true);
//                        p.launchProjectile(Snowball.class).addScoreboardTag("kamikazesheep");
//                        p.sendMessage("You casted Kamikazesheep!");
//                    }
//                    case "ThermonuclearDetonation" -> {
//                        event.setCancelled(true);
//                        p.launchProjectile(Snowball.class).addScoreboardTag("earfquake");
//                    }
//                }
//            }
//        }
//    }

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
        } else if (event.getEntity() instanceof Snowball snowball) {
            if (snowball.getScoreboardTags().contains("kamikazesheep")) {
                // Get the closest player to the snowball
                Player p = null;
                double distance = 100;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getLocation().distance(snowball.getLocation()) < distance) {
                        p = player;
                        distance = player.getLocation().distance(snowball.getLocation());
                    }
                }

                Sheep sheep = snowball.getWorld().spawn(snowball.getLocation(), Sheep.class);

                TNTPrimed tnt = snowball.getWorld().spawn(snowball.getLocation(), TNTPrimed.class);
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

                snowball.remove();

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
            } else if (snowball.getScoreboardTags().contains("earfquake")) {
                snowball.getWorld().createExplosion(snowball.getLocation(), 80, true, false); // Adjust power as needed

                // Get the blocks in the affected area
                float radius = 30; // Adjust as needed however no bigger than 40 has been tested which is already very large


                int remTree = (int) (radius * 5);

                // Remove all trees and leaves in the affected area
                for (int x = -remTree; x <= remTree; x++) {
                    for (int y = -remTree; y <= remTree; y++) {
                        for (int z = -remTree; z <= remTree; z++) {
                            if (Math.sqrt(x * x + (y) * (y) + z * z) <= remTree) {
                                Block block = snowball.getLocation().clone().add(x, y, z).getBlock();
                                if (block.getType() == Material.OAK_LOG || block.getType() == Material.BIRCH_LOG || block.getType() == Material.SPRUCE_LOG || block.getType() == Material.JUNGLE_LOG || block.getType() == Material.ACACIA_LOG || block.getType() == Material.DARK_OAK_LOG) {
                                    block.setType(Material.FIRE);
                                }
                                if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.BIRCH_LEAVES || block.getType() == Material.SPRUCE_LEAVES || block.getType() == Material.JUNGLE_LEAVES || block.getType() == Material.ACACIA_LEAVES || block.getType() == Material.DARK_OAK_LEAVES) {
                                    block.setType(Material.FIRE);
                                }
                                if (block.getType() == Material.BAMBOO || block.getType() == Material.SHORT_GRASS || block.getType() == Material.TALL_GRASS || block.getType() == Material.VINE) {
                                    block.setType(Material.FIRE);
                                }
                                if (block.getType() == Material.SNOW || block.getType() == Material.POWDER_SNOW || block.getType() == Material.SNOW_BLOCK || block.getType() == Material.ICE || block.getType() == Material.PACKED_ICE || block.getType() == Material.BLUE_ICE || block.getType() == Material.FROSTED_ICE) {
                                    block.setType(Material.AIR);
                                }
                                if (block.getType() == Material.GRASS_BLOCK || block.getType() == Material.DIRT || block.getType() == Material.COARSE_DIRT || block.getType() == Material.PODZOL || block.getType() == Material.DIRT_PATH || block.getType() == Material.MYCELIUM || block.getType() == Material.SNOW_BLOCK) {
                                    // Choose random number if the block should be converted to course dirt or other "ruined" blocks
                                    int random = (int) (Math.random() * 100);
                                    if (random < 30) {
                                        block.setType(Material.COARSE_DIRT);
                                    } else if (random < 40) {
                                        block.setType(Material.MUD);
                                    } else if (random < 50) {
                                        block.setType(Material.NETHERRACK);
                                        Block blockFire = snowball.getLocation().clone().add(x, (y + 1), z).getBlock();
                                        if (blockFire.getType() == Material.AIR) {
                                            blockFire.setType(Material.FIRE);
                                        }
                                    } else if (random < 60) {
                                        block.setType(Material.DIRT_PATH);
                                    } else if (random < 70) {
                                        block.setType(Material.GRAVEL);
                                    } else if (random < 80) {
                                        block.setType(Material.PODZOL);
                                    } else if (random < 90) {
                                        block.setType(Material.MANGROVE_ROOTS);
                                    } else if (random < 100) {
                                        block.setType(Material.MUDDY_MANGROVE_ROOTS);
                                    }
                                }
                            }
                        }
                    }
                }

                // Do the explosion
                for (int x = (int) -radius; x <= radius; x++) {
                    for (int y = (int) -radius; y <= radius; y++) {
                        for (int z = (int) -radius; z <= radius; z++) {
                            if (Math.sqrt(x * x + y * y + z * z) <= radius) {
                                Block block = snowball.getLocation().clone().add(x, y, z).getBlock();
                                if ((block.getType() != Material.AIR)
                                        && (block.getType() != Material.BEDROCK)
                                        && (block.getType() != Material.AIR)
                                ) {
                                    if ((block.getType() == Material.WATER)) {
                                        block.setType(Material.AIR);
                                    }


                                    // Spawn falling block entities at the location of each block
                                    FallingBlock fallingBlock = snowball.getWorld().spawnFallingBlock(block.getLocation(), block.getBlockData());
                                    // Apply velocity to simulate launch effect
                                    fallingBlock.setVelocity(new Vector(
                                            (Math.random() - 0.5) * (radius / 20) * 2, // Random x velocity between -radius/20 and radius/20
                                            Math.random() * ((radius / 100) * 3) + 1.2, // Random y velocity between 1.2 and (1.2 + radius/100 * 3)
                                            (Math.random() - 0.5) * (radius / 20) * 2 // Random z velocity between -radius/20 and radius/20
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
}
