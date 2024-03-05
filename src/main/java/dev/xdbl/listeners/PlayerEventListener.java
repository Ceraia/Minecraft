package dev.xdbl.listeners;

import dev.xdbl.types.DoublePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEventListener extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final DoublePlayer doublePlayer;

    public PlayerEventListener(Player player, DoublePlayer doublePlayer) {
        this.player = player;
        this.doublePlayer = doublePlayer;
    }

    public Player getPlayer() {
        return player;
    }

    public DoublePlayer getArenaPlayer() {
        return doublePlayer;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
