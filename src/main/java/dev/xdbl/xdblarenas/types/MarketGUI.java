package dev.xdbl.xdblarenas.types;

import dev.xdbl.xdblarenas.XDBLArena;
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
import java.util.concurrent.atomic.AtomicInteger;

public class MarketGUI implements Listener {
    private final XDBLArena plugin;
    private final Map<Player, Map<Integer, MarketItem>> itemSelectionCache = new HashMap<>();
    public MarketGUI(XDBLArena plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMarketGUI(Player player) { // Let the player select what arena to fight in
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.market.inventory_name"))));

        Map<Integer, MarketItem> itemSelectSlots = new HashMap<>();

        int i = 0;

        for (MarketItem marketItem : plugin.getMarketItemManager().getMarketItems()) {
            ItemStack itemStack = marketItem.getItemStack();
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<Component> lore = new ArrayList<>();
            plugin.getConfig().getStringList("messages.market.item_lore").forEach(s -> lore.add(MiniMessage.miniMessage().deserialize(s.replace("%price%", String.valueOf(marketItem.getPrice())))));
            itemMeta.lore(lore);
            itemStack.setItemMeta(itemMeta);
            inv.setItem(i, itemStack);

            itemSelectSlots.put(i, marketItem);
            i++;
        }

        itemSelectionCache.put(player, itemSelectSlots);

        // Open the inventory
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        } // If the item doesn't exist or is air, return

        if (Objects.requireNonNull(e.getClickedInventory()).getType() == InventoryType.PLAYER) {
            return;
        } // If the inventory is the player's inventory, return

        Player player = (Player) e.getWhoClicked(); // Get the player who clicked

        if (Objects.equals(e.getView().title().toString(), MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.market.inventory_name"))).toString())) {
            e.setCancelled(true);

            if (itemSelectionCache.containsKey(player)) {
                Map<Integer, MarketItem> itemSelectSlots = itemSelectionCache.get(player);

                if (itemSelectSlots.containsKey(e.getSlot())) {
                    MarketItem marketItem = itemSelectSlots.get(e.getSlot());

                    if (marketItem == null) {
                        return;
                    }

                    // Create a new ItemStack without market-related meta
                    ItemStack purchasedItem = new ItemStack(marketItem.getItemStack());
                    ItemMeta purchasedMeta = purchasedItem.getItemMeta();
                    purchasedMeta.lore(null); // Remove any additional lore/meta
                    // Any other modifications if needed...

                    purchasedItem.setItemMeta(purchasedMeta);

                    player.getInventory().addItem(purchasedItem);

                    // Notify the player about the purchase

                    plugin.getMarketItemManager().removeItem(marketItem.getUUID());
                    plugin.getMarketGUI().openMarketGUI(player);
                }
            }
        }
    }
}
