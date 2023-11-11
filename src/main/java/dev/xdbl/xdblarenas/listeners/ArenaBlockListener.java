package dev.xdbl.xdblarenas.listeners;

import dev.xdbl.xdblarenas.arenas.Arena;
import dev.xdbl.xdblarenas.XDBLArena;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ArenaBlockListener implements Listener {

    private XDBLArena plugin;

    public ArenaBlockListener(XDBLArena plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Arena arena = plugin.getArenaManager().getArena(e.getPlayer());
        if (arena == null) {
            return;
        }

        arena.placeBlock(e.getBlockPlaced().getLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Arena arena = plugin.getArenaManager().getArena(e.getPlayer());
        if (arena == null) {
            return;
        }

        if (arena.getPlacedBlocks().contains(e.getBlock().getLocation())) {
            arena.removeBlock(e.getBlock().getLocation());
            return;
        }

        e.setCancelled(true);
    }
}
