package dev.xdbl.xdblarenas.listeners;

import dev.xdbl.xdblarenas.types.ArenaPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEventListener extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ArenaPlayer arenaPlayer;

    public PlayerEventListener(Player player, ArenaPlayer arenaPlayer) {
        this.player = player;
        this.arenaPlayer = arenaPlayer;
    }

    public Player getPlayer() {
        return player;
    }

    public ArenaPlayer getArenaPlayer() {
        return arenaPlayer;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
