package com.ceraia.arena.listeners;

import com.ceraia.arena.Double;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ArenaExplodeListener implements Listener {

    private final Double plugin;

    public ArenaExplodeListener(Double plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isInArena(Player player) {
        return plugin.getArenaManager().getArena(player) != null;
    }

    // TNT
    @EventHandler
    public void onEntiyExplode(EntityExplodeEvent e) {
        Player source = null;
        if (e.getEntityType().equals(EntityType.TNT)) {
            TNTPrimed tnt = (TNTPrimed) e.getEntity();
            Entity entity = tnt.getSource();
            if (!(entity instanceof Player)) return;
            source = (Player) entity;
        }

        if (source == null || !isInArena(source)) {
            return;
        }
        e.blockList().clear();
    }

    // End crystal
    @EventHandler
    public void onHitCrystal(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();

        if (damager == null) return;

        Player source = null;

        if (!(entity instanceof EnderCrystal)) return;

        if (damager instanceof Player) {
            source = (Player) damager;
        } else if (damager instanceof Arrow) {
            Arrow arrow = (Arrow) damager;
            Entity entity2 = (Entity) arrow.getShooter();
            if (!(entity2 instanceof Player)) return;
            source = (Player) entity2;
        } else {
            return;
        }

        if (!isInArena(source)) {
            return;
        }

        e.setCancelled(true);
        if (e.getEntity().isValid())
            e.getEntity().remove();
        e.getEntity().getWorld().createExplosion(
                e.getEntity().getLocation(),
                6,
                false,
                false
        );
    }

    // Respawn Anchor
    @EventHandler
    public void onFillAnchor(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (!e.getClickedBlock().getType().equals(Material.RESPAWN_ANCHOR)) return;

        Block block = e.getClickedBlock();
        RespawnAnchor data = (RespawnAnchor) block.getBlockData();
        if (data.getCharges() < data.getMaximumCharges()) return;

        if (!isInArena(e.getPlayer())) return;

        e.setCancelled(true);
        block.setType(Material.AIR);
        block.getWorld().createExplosion(
                block.getLocation(),
                5,
                false,
                false
        );
    }
}
