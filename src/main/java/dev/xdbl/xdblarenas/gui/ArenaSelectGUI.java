package dev.xdbl.xdblarenas.gui;

import dev.xdbl.xdblarenas.InviteManager;
import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.arenas.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ArenaSelectGUI implements Listener {

    private static Component INVENTORY_NAME;
    private final XDBLArena plugin;
    private final Map<Player, Map<Integer, Arena>> selectingArenaCache = new HashMap<>();

    public ArenaSelectGUI(XDBLArena plugin) {
        this.plugin = plugin;

        INVENTORY_NAME = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.arena_select_gui.inventory_name")));

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfig() {

        INVENTORY_NAME = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.arena_select_gui.inventory_name")));
    }

    public void openGUI(Player inviter) {
        List<Arena> arenas = plugin.getArenaManager().getArenas()
                .stream().filter(a -> a.isPublic() || a.getOwner().equals(inviter.getName())).toList();

        // size is from arenas.size() and must be devidable by 9
        int size = Math.max(9, (arenas.size() + 8) / 9 * 9);

        Inventory inv = Bukkit.createInventory(null, size, MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.arena_select_gui.inventory_name"))));

        Map<Integer, Arena> arenasSelectSlots = new HashMap<>();

        int i = 0;
        for (Arena a : arenas.stream().filter(a -> a.getState() == Arena.ArenaState.WAITING).toList()) {
            ItemStack is = new ItemStack(Objects.requireNonNull(Material.getMaterial(
                    Objects.requireNonNull(plugin.getConfig().getString("messages.arena_select_gui.arena_item.item"))
            )));
            ItemMeta meta = is.getItemMeta();
            meta.displayName(
                    MiniMessage.miniMessage().deserialize(
                    Objects.requireNonNull(plugin.getConfig().getString("messages.arena_select_gui.arena_item.name"))
                    .replace("%arena_name%", a.getName())
                    .replace("%arena_owner%", a.getOwner())
                    .replace("%totemsenabled%", a.hasTotems() ? "<green>enabled" : "<red>disabled")
                    )
            );

            List<Component> lore = new ArrayList<>();
            for (String s : plugin.getConfig().getStringList("messages.arena_select_gui.arena_item.lore")) {
                lore.add(MiniMessage.miniMessage().deserialize(s.replace("%arena_name%", a.getName())
                        .replace("%arena_owner%", a.getOwner())
                ));
            }

            meta.lore(lore);

            is.setItemMeta(meta);

            inv.setItem(i, is);
            arenasSelectSlots.put(i, a);

            i++;
        }

        selectingArenaCache.put(inviter, arenasSelectSlots);

        inviter.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!Objects.equals(e.getView().title().toString(), INVENTORY_NAME.toString())) {
            return;
        }

        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        if (Objects.requireNonNull(e.getClickedInventory()).getType() == InventoryType.PLAYER) {
            return;
        }

        Player inviter = (Player) e.getWhoClicked();

        int slot = e.getSlot();
        Arena arena = selectingArenaCache.get(inviter).get(slot);

        if (arena == null || arena.getState() != Arena.ArenaState.WAITING) {
            inviter.sendMessage(
                    MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.arena_select_gui.arena_not_ready")))
            );
            return;
        }

        InviteManager.Invite invite = plugin.getInviteManager().selectingInvites.get(inviter);

        inviter.sendMessage(
                MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.pvp.invite.invite_sent"))
                        .replace("%player%", invite.invited.getName()))
        );

        String invite_message = Objects.requireNonNull(plugin.getConfig().getString("messages.arena_select_gui.invite_message"))
                .replace("%inviter%", inviter.getName())
                .replace("%totemsenabled%", arena.hasTotems() ? "<green>enabled" : "<red>disabled")
                .replace("%arena_name%", arena.getName())
                .replace("%winchance%", plugin.getPlayerManager().CalculateWinChance(invite.invited.getUniqueId(), inviter.getUniqueId()) + "%");

        Objects.requireNonNull(plugin.getServer().getPlayer(invite.invited.getUniqueId())).sendMessage(MiniMessage.miniMessage().deserialize(invite_message));

        invite.arena = arena;
        plugin.getInviteManager().selectingInvites.remove(inviter);
        plugin.getInviteManager().invites.put(invite.invited, invite);

        inviter.closeInventory();
    }

}
