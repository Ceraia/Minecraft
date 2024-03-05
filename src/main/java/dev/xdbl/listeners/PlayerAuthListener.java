package dev.xdbl.listeners;

import dev.xdbl.Double;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerAuthListener implements Listener {

    private final Double plugin;

    public PlayerAuthListener(Double plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        // Force the player to log in

    }
}
