package dev.xdbl.xdblarenas.events;

import dev.xdbl.xdblarenas.players.ArenaPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerEvents extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ArenaPlayer arenaPlayer;

    public PlayerEvents(Player player, ArenaPlayer arenaPlayer) {
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
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
