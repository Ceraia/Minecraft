package dev.xdbl.managers;

import dev.xdbl.Double;
import dev.xdbl.types.MarketItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class MarketItemManager {

    private final Double plugin;

    private final List<MarketItem> marketItems = new ArrayList<>();

    public MarketItemManager(Double plugin) {
        this.plugin = plugin;

        // Load arenas
        File f = new File(plugin.getDataFolder(), "data/items");
        if (!f.exists()) {
            f.mkdirs();
            return;
        }

        File[] files = f.listFiles();
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            String owner = config.getString("owner");
            int price = config.getInt("price", 1000);
            ItemStack itemStack = config.getItemStack("item");

            MarketItem marketItem = new MarketItem(plugin, UUID.fromString(file.getName().split("\\.")[0]), UUID.fromString(owner), price, itemStack, file);
            marketItems.add(marketItem);
        }

    }

    public List<MarketItem> getMarketItems() {
        return marketItems;
    }

    public MarketItem getItem(UUID itemUUID) {
        for (MarketItem marketItem : marketItems) {
            if (marketItem.getUUID().equals(itemUUID)) {
                return marketItem;
            }
        }
        return null;
    }

    public boolean createItem(Player player, ItemStack itemStack, int price) {
        File f = new File(plugin.getDataFolder(), "data/items");
        if (!f.exists()) {
            f.mkdirs();
            return false;
        }

        File file = new File(f, UUID.randomUUID() + ".yml");
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("owner", player.getUniqueId().toString());
        config.set("price", price);
        config.set("item", itemStack);
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        MarketItem marketItem = new MarketItem(plugin, UUID.fromString(file.getName().split("\\.")[0]), player.getUniqueId(), price, itemStack, file);
        marketItems.add(marketItem);
        return true;
    }

    public void removeItem(UUID uuid) {
        MarketItem marketItem = getItem(uuid);
        marketItem.delete();
    }
}
