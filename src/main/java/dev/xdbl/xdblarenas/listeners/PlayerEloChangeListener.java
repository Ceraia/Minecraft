package dev.xdbl.xdblarenas.listeners;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.events.PlayerEvents;
import dev.xdbl.xdblarenas.scoreboards.EloScoreboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerEloChangeListener implements Listener {

    private final XDBLArena plugin;

    public PlayerEloChangeListener(XDBLArena plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler // Custom on player Elo change event to update the scoreboard
    public void onPlayerEloChange(PlayerEvents event) {
        EloScoreboard eloScoreboard = new EloScoreboard(plugin);
        eloScoreboard.updateScoreboard();
    }

    @EventHandler // On player join event to update the scoreboard
    public void onPlayerJoin(PlayerJoinEvent event) {
        EloScoreboard eloScoreboard = new EloScoreboard(plugin);
        eloScoreboard.updateScoreboard();
    }

}
