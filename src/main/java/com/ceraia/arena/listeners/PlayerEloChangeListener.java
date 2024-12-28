package com.ceraia.arena.listeners;

import com.ceraia.arena.Double;
import com.ceraia.arena.managers.EloScoreboardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerEloChangeListener implements Listener {

    private final Double plugin;

    public PlayerEloChangeListener(Double plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler // Custom on player Elo change event to update the scoreboard
    public void onPlayerEloChange(PlayerEventListener event) {
        EloScoreboardManager eloScoreboardManager = new EloScoreboardManager(plugin);
        eloScoreboardManager.updateScoreboard();
    }

    @EventHandler // On player join event to update the scoreboard
    public void onPlayerJoin(PlayerJoinEvent event) {
        EloScoreboardManager eloScoreboardManager = new EloScoreboardManager(plugin);
        eloScoreboardManager.updateScoreboard();
    }

}
