package dev.xdbl.types;

import dev.xdbl.Double;
import dev.xdbl.managers.InviteManager;
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
    private static Component INVENTORY_NAME_ARENAS = MiniMessage.miniMessage().deserialize("Select an arena");
    private static Component INVENTORY_NAME_TOTEMS = MiniMessage.miniMessage().deserialize("Select totems");
    private final Double plugin;
    private final Map<Player, Map<Integer, Arena>> selectingArenaCache = new HashMap<>();

    public ArenaSelectGUI(Double plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openArenaList(Player inviter, Player invited) { // Let the player select what arena to fight in
        // Add the player invitee and inviter
        InviteManager.Invite invite = new InviteManager.Invite(inviter, invited);

        plugin.getInviteManager().selectingInvites.put(inviter, invite);

        // Get a list of all arenas accessible to the player
        List<Arena> arenas = plugin.getArenaManager().getArenas()
                .stream().filter(a -> a.isPublic() || a.getOwner().equals(inviter.getName())).toList();

        // Size is from arenas.size() and must be devidable by 9
        int size = Math.max(9, (arenas.size() + 8) / 9 * 9);

        // Create the inventory
        Inventory inv = Bukkit.createInventory(null, size, INVENTORY_NAME_ARENAS);

        // Create a map of the slot and the arena
        Map<Integer, Arena> arenasSelectSlots = new HashMap<>();

        int i = 0; // Slot
        for (Arena a : arenas.stream().filter(a -> a.getState() == Arena.ArenaState.WAITING).toList()) { // Filter out arenas that are not ready
            ItemStack itemStack = new ItemStack(Material.ENDER_EYE); // Create the itemstack
            ItemMeta meta = itemStack.getItemMeta();
            meta.displayName(
                    MiniMessage.miniMessage().deserialize(
                            "<green>%arena_name%</green>"
                    )
            );

            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize(
                    "<gray>Owner: %arena_owner%"
            ));

            meta.lore(lore);

            itemStack.setItemMeta(meta);

            inv.setItem(i, itemStack);
            arenasSelectSlots.put(i, a);

            i++;
        }

        // Put the map in the cache
        selectingArenaCache.put(inviter, arenasSelectSlots);

        // Open the inventory
        inviter.openInventory(inv);
    }

    public void openTotemEnabled(Player inviter, Arena arena) { // Let the player select whether to enable or disable totems in the fight
        int size = 9;

        Inventory inv = Bukkit.createInventory(null, size, INVENTORY_NAME_TOTEMS);

        ItemStack itemStackEnable = new ItemStack(Material.GREEN_STAINED_GLASS_PANE); // Create the itemstack
        ItemMeta metaEnable = itemStackEnable.getItemMeta();

        metaEnable.displayName(
                MiniMessage.miniMessage().deserialize(
                        "<green>Enable totems</green>"
                )
        );

        List<Component> loreEnable = new ArrayList<>();
        loreEnable.add(MiniMessage.miniMessage().deserialize("<gray>Click to enable totems in the fight"));

        metaEnable.lore(loreEnable);

        itemStackEnable.setItemMeta(metaEnable);

        inv.setItem(1, itemStackEnable);

        ItemStack itemStackDisable = new ItemStack(Material.RED_STAINED_GLASS_PANE); // Create the itemstack
        ItemMeta metaDisable = itemStackDisable.getItemMeta();
        metaDisable.displayName(
                MiniMessage.miniMessage().deserialize(
                        "<red>Disable totems</red>"
                )
        );

        List<Component> loreDisable = new ArrayList<>();
        loreDisable.add(MiniMessage.miniMessage().deserialize("<gray>Click to disable totems in the fight"));


        metaDisable.lore(loreDisable);

        itemStackDisable.setItemMeta(metaDisable);

        inv.setItem(7, itemStackDisable);

        // Set the center slot to the arena that was selected
        ItemStack itemStackArena = new ItemStack(Material.ENDER_EYE); // Create the itemstack

        ItemMeta metaArena = itemStackArena.getItemMeta();
        metaArena.displayName(
                MiniMessage.miniMessage().deserialize(
                        "<green>%arena_name%</green>"
                )
        );

        List<Component> loreArena = new ArrayList<>();
        loreArena.add(MiniMessage.miniMessage().deserialize(
                "<gray>Owner: %arena_owner%"
        ));

        metaArena.lore(loreArena);

        inviter.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        } // If the item doesn't exist or is air, return

        if (Objects.requireNonNull(e.getInventory()).getType() == InventoryType.PLAYER) {
            return;
        } // If the inventory is the player's inventory, return

        Player inviter = (Player) e.getWhoClicked(); // Get the player who clicked

        if (Objects.equals(e.getView().title().toString(), INVENTORY_NAME_ARENAS.toString())) {
            e.setCancelled(true); // The inventory name is from the plugin, so cancel the event

            int slot = e.getSlot();
            Arena arena = selectingArenaCache.get(inviter).get(slot); // Get the specific player and then the arena from the cache

            InviteManager.Invite invite = plugin.getInviteManager().selectingInvites.get(inviter); // Get the invite from the selectingInvites map
            invite.arena = arena; // Set the arena in the invite


            if (arena == null || arena.getState() != Arena.ArenaState.WAITING) { // Somehow arena doesn't work, purely debug
                inviter.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                                "<red>That arena is not available at the moment. Please try again later."
                        )
                );
                return;
            }


            openTotemEnabled(inviter, arena);
        }
        if (Objects.equals(e.getView().title().toString(), INVENTORY_NAME_TOTEMS.toString())) {
            e.setCancelled(true); // The inventory name is from the plugin, so cancel the event

            int slot = e.getSlot();

            // Get the invite from the selectingInvites map
            InviteManager.Invite invite = plugin.getInviteManager().selectingInvites.get(inviter);

            // Get the arena from the invite
            Arena arena = invite.arena;

            // Set the totems in the invite
            arena.totems = slot == 1;

            inviter.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<green>Totems have been " + (slot == 1 ? "enabled" : "disabled") + " for the fight."
                    )
            ); // Send the invite confirmation message

            String totemsEnabled = arena.totems ? "<green>enabled</green>" : "<red>disabled</red>";

            Objects.requireNonNull(plugin.getServer().getPlayer(invite.invited.getUniqueId()))
                    .sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                    "<green>Click <white><hover:show_text:\"<green>Click to accept the pvp match!\"><click:run_command:/pvp accept>[here]</click></hover> <green>to join PVP arena with "
                                            + inviter.getName() + " in " + arena.getName() +
                                            ", totems are "
                                            + totemsEnabled + " you have a "
                                            + plugin.getPlayerManager().CalculateWinChance(inviter.getUniqueId(), invite.invited.getUniqueId())
                                            + "% chance of winning!"
                            ));// Send the actual message


            invite.arena = arena;

            plugin.getInviteManager().selectingInvites.remove(inviter); // Remove the invite from the selectingInvites map
            plugin.getInviteManager().invites.put(invite.invited, invite); // Put the invite in the invites map

            inviter.closeInventory(); // Close the inventory
        }
    }
}
