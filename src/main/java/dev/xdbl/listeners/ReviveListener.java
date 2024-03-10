package dev.xdbl.listeners;

import dev.xdbl.Double;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class ReviveListener implements Listener {

    private final Double plugin;
    private final List<PlayerSheepPair> playerSheepPairs;

    public ReviveListener(Double plugin) {
        this.plugin = plugin;
        this.playerSheepPairs = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Every tick for 20 seconds, set the player back on the sheep if they are not on the arrow
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (PlayerSheepPair pair : playerSheepPairs) {
                Player player = pair.getPlayer();
                Sheep sheep = pair.getSheep();
                if (!player.getPassengers().contains(sheep)) {
                    sheep.addPassenger(player);
                }
            }
        }, 0, 1);
    }

//    @EventHandler
//    public void onPlayerDeath(PlayerDeathEvent e) {
//
//        Player player = e.getEntity().getPlayer();
//        if (player == null) {
//            return;
//        }
//
////        Player killer = player.getKiller();
////        if (killer == null) {
////            return;
////        }
//
//
//        Location sheepLocation = player.getLocation().clone();
//        sheepLocation.setY(sheepLocation.getY() - 1.2); // Set the sheep slightly below ground level
//        Sheep sheep = player.getWorld().spawn(sheepLocation, Sheep.class);
//
//        // Cancel the death event
//        e.setCancelled(true);
//
//        // Add the player and sheep to the list
//        PlayerSheepPair pair = new PlayerSheepPair(player, sheep);
//        playerSheepPairs.add(pair);
//
//        // Make the player sit on the sheep
//        sheep.addPassenger(player);
//        player.setInvulnerable(true);
//
//        // Add a scoreboard tag to the arrow to identify it as a revive sheep
//        sheep.addScoreboardTag("revive");
//        sheep.setInvulnerable(true);
//        sheep.setAI(false);
//        sheep.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 999999, 1, false, false));
//
//        // After 20 seconds, remove the sheep and teleport the player up by 1.2
//        Bukkit.getScheduler().runTaskLater(plugin, () -> {
//            if (!(sheep.isDead())) {
//                sheep.remove();
//                player.setInvulnerable(false);
//
//                // Kill the player and have the killer be the last damage cause
//                //player.damage(player.getHealthScale() + 10, killer);
//                player.damage(player.getHealthScale() + 10);
//                player.setHealth(0);
//
//                playerSheepPairs.remove(pair); // Remove the pair from the list
//            }
//        }, 20 * 20);
//    }

    // Define PlayerSheepPair class
    private class PlayerSheepPair {
        private final Player player;
        private final Sheep sheep;

        public PlayerSheepPair(Player player, Sheep sheep) {
            this.player = player;
            this.sheep = sheep;
        }

        public void delete() {
            playerSheepPairs.remove(this);
        }

        public Player getPlayer() {
            return player;
        }

        public Sheep getSheep() {
            return sheep;
        }
    }
}
