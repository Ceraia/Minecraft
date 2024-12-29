package com.ceraia.modules.arenas.listeners;

import com.ceraia.modules.ceraia.types.CeraiaPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEventListener extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final CeraiaPlayer ceraiaPlayer;

    public PlayerEventListener(Player player, CeraiaPlayer ceraiaPlayer) {
        this.player = player;
        this.ceraiaPlayer = ceraiaPlayer;
    }

    public Player getPlayer() {
        return player;
    }

    public CeraiaPlayer getArenaPlayer() {
        return ceraiaPlayer;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
