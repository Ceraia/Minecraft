package com.ceraia.modules.arenas.listeners;

import com.ceraia.Ceraia;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerInventoryListener implements Listener {

    private final Ceraia plugin;

    public PlayerInventoryListener(Ceraia plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isInArena(Player player) {
        return plugin.getArenaModule().getArenaManager().getArena(player) != null;
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        if (isInArena(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (!isInArena(p)) {
            return;
        }

        if (e.getView().getTopInventory().getType() == InventoryType.PLAYER
                || e.getView().getTopInventory().getType() ==
                InventoryType.CRAFTING) {
            return;
        }

        e.setCancelled(true);

    }
}
