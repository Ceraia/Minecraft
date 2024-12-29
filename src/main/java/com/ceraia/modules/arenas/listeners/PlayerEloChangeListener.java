package com.ceraia.modules.arenas.listeners;

import com.ceraia.Ceraia;
import com.ceraia.modules.arenas.managers.EloScoreboardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerEloChangeListener implements Listener {

    private final Ceraia plugin;

    public PlayerEloChangeListener(Ceraia plugin) {
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
