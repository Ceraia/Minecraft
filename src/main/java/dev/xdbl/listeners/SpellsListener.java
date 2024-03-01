package dev.xdbl.listeners;

import dev.xdbl.Double;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

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
                }
            }
        }
    }



    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        this.plugin.getLogger().info("Player joined");
    }
}
