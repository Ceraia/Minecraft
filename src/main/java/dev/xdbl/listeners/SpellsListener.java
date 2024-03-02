package dev.xdbl.listeners;

import dev.xdbl.Double;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
                if(name.equals("Bidenblast")){
                    event.setCancelled(true);
                    p.sendMessage("You casted Bidenblast!");

                    for (int i = 0; i < 10; i++) {
                        p.launchProjectile(org.bukkit.entity.Arrow.class);
                    }
                } else if (name.equals("Fireflies")) {
                    // Launch snowballs that light the person it hits on fire
                    event.setCancelled(true);
                    p.sendMessage("You casted Fireflies!");
                    p.launchProjectile(org.bukkit.entity.SmallFireball.class);
                } else if (name.equals("Getauttahere")){
                    event.setCancelled(true);
                    Trident trident = p.launchProjectile(org.bukkit.entity.Trident.class);
                    trident.setPickupStatus(Trident.PickupStatus.DISALLOWED); // Prevents Trident from being picked up
                    trident.addScoreboardTag("getauttahere"); // Add a tag to the Trident to identify it later
                    trident.addPassenger(p);
                    p.sendMessage("You casted Getauttahere!");
                } else if (name.equals("Shitstorm")){
                    event.setCancelled(true);
                    for(int i = 0; i < 1; i++) {
                        Arrow arrow = p.launchProjectile(org.bukkit.entity.Arrow.class);
                        arrow.setVelocity(p.getLocation().getDirection().multiply(10));
                        arrow.addScoreboardTag("explosive");
                    }
                } else if (name.equals("Kamikazesheep")){
                    event.setCancelled(true);

                    Sheep sheep = p.getWorld().spawn(p.getLocation(), Sheep.class);

                    TNTPrimed tnt = p.getWorld().spawn(p.getLocation(), TNTPrimed.class);
                    tnt.setFuseTicks(100);


                    sheep.addPassenger(tnt);
                    sheep.addScoreboardTag("kamikaze");
                    sheep.setCustomNameVisible(true);
                    sheep.customName(MiniMessage.miniMessage().deserialize("jeb_"));

                    sheep.addPotionEffect(
                            new org.bukkit.potion.PotionEffect(
                                    PotionEffectType.SPEED, 1000000, 2, false, false
                            )
                    );

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Make the sheep walk towards the player
                            sheep.getPathfinder().moveTo(p.getLocation());
                        }
                    }.runTaskTimer(plugin, 0L, 2L);
                }  else if (name.equals("Kamikazebat")){
                    event.setCancelled(true);

                    Bat bat = p.getWorld().spawn(p.getLocation(), Bat.class);

                    TNTPrimed tnt = p.getWorld().spawn(p.getLocation(), TNTPrimed.class);
                    tnt.setFuseTicks(100);


                    bat.addPassenger(tnt);
                    bat.addScoreboardTag("kamikaze");
                    bat.setCustomNameVisible(true);

                    bat.addPotionEffect(
                            new org.bukkit.potion.PotionEffect(
                                    PotionEffectType.SPEED, 1000000, 2, false, false
                            )
                    );

                    bat.lookAt(p);

                    Sheep sheep = p.getWorld().spawn(p.getLocation(), Sheep.class);

                    sheep.addPassenger(bat);
                    sheep.addPotionEffect(
                            new org.bukkit.potion.PotionEffect(
                                    PotionEffectType.SPEED, 1000000, 2, false, false
                            )
                    );
                    sheep.addPotionEffect(                            new org.bukkit.potion.PotionEffect(
                            PotionEffectType.INVISIBILITY, 1000000, 2, false, false
                    ));

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Make the sheep walk towards the player
                            sheep.getPathfinder().moveTo(p.getLocation());
                        }
                    }.runTaskTimer(plugin, 0L, 2L);
                }
            }
        }
    }

    @EventHandler
    public void onTridentHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Trident trident) {
            if (trident.getScoreboardTags().contains("getauttahere")) {
                trident.remove();
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            if (arrow.getScoreboardTags().contains("explosive")) {
                arrow.getWorld().createExplosion(arrow.getLocation(), 50);
                arrow.remove();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        this.plugin.getLogger().info("Player joined");
    }
}
