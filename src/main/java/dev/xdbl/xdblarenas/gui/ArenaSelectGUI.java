package dev.xdbl.xdblarenas.gui;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.arenas.Arena;
import dev.xdbl.xdblarenas.InviteManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArenaSelectGUI implements Listener {

    private static String INVENTORY_NAME;
    private final XDBLArena plugin;
    private Map<Player, Map<Integer, Arena>> selectingArenaCache = new HashMap<>();

    public ArenaSelectGUI(XDBLArena plugin) {
        this.plugin = plugin;

        INVENTORY_NAME = plugin.getConfig().getString("messages.arena_select_gui.inventory_name").replace("&", "§");

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfig() {
        INVENTORY_NAME = plugin.getConfig().getString("messages.arena_select_gui.inventory_name").replace("&", "§");
    }

    public void openGUI(Player inviter) {
        List<Arena> arenas = plugin.getArenaManager().getArenas()
                .stream().filter(a -> a.isPublic() || a.getOwner().equals(inviter.getName())).collect(Collectors.toList());

        // size is from arenas.size() and must be devidable by 9
        int size = Math.max(9, (arenas.size() + 8) / 9 * 9);

        Inventory inv = Bukkit.createInventory(null, size, INVENTORY_NAME);

        Map<Integer, Arena> arenasSelectSlots = new HashMap<>();

        int i = 0;
        for (Arena a : arenas.stream().filter(a -> a.isReady() && a.getState() == Arena.ArenaState.WAITING).collect(Collectors.toList())) {
            ItemStack is = new ItemStack(Material.getMaterial(
                    plugin.getConfig().getString("messages.arena_select_gui.arena_item.item")
            ));
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName(plugin.getConfig().getString("messages.arena_select_gui.arena_item.name")
                    .replace("%arena_name", a.getName())
                    .replace("%arena_owner", a.getOwner())
                    .replace("&", "§")
            );

            List<String> lore = new ArrayList<>();
            for (String s : plugin.getConfig().getStringList("messages.arena_select_gui.arena_item.lore")) {
                lore.add(s.replace("%arena_name", a.getName())
                        .replace("%arena_owner", a.getOwner())
                        .replace("&", "§")
                );
            }

            meta.setLore(lore);

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
        if (!e.getView().getTitle().startsWith(INVENTORY_NAME)) {
            return;
        }

        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        Player inviter = (Player) e.getWhoClicked();

        int slot = e.getSlot();
        Arena arena = selectingArenaCache.get(inviter).get(slot);

        if (arena == null || !arena.isReady() || arena.getState() != Arena.ArenaState.WAITING) {
            inviter.sendMessage(
                    plugin.getConfig().getString("messages.arena_select_gui.arena_not_ready").replace("&", "§")
            );
            return;
        }

        InviteManager.Invite invite = plugin.getInviteManager().selectingInvites.get(inviter);

        inviter.sendMessage(
                plugin.getConfig().getString("messages.pvp.invite.invite_sent")
                        .replace("%player%", invite.invited.getName())
                        .replace("&", "§")
        );

        String invite_message = plugin.getConfig().getString("messages.arena_select_gui.invite_message")
                .replace("%inviter%", inviter.getName())
                .replace("%arena_name%", arena.getName())
                .replace("&", "§");
        String[] split = invite_message.split("@");
        // get between two @
        TextComponent message = new TextComponent(split[0]);

        TextComponent clickableMessage = new TextComponent(split[1]);
        clickableMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pvp accept"));
        clickableMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClick to accept").create()));
        message.addExtra(clickableMessage);
        message.addExtra(split[2]);
        invite.invited.spigot().sendMessage(message);

        invite.arena = arena;
        plugin.getInviteManager().selectingInvites.remove(inviter);
        plugin.getInviteManager().invites.put(invite.invited, invite);

        inviter.closeInventory();
    }

}
