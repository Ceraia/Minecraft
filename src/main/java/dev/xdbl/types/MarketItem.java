package dev.xdbl.types;

import dev.xdbl.Double;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MarketItem {
    private ItemStack itemstack;
    private Double plugin;
    public UUID uuid;
    public UUID owneruuid;
    private int price;
    public final File configFile;

    public MarketItem(Double plugin, UUID uuid, UUID owneruuid, int price, ItemStack itemStack, File configFile) {
        this.plugin = plugin;

        this.uuid = uuid;
        this.owneruuid = owneruuid;
        this.itemstack = itemStack;

        this.price = price;

        this.configFile = configFile;
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getPrice() {
        return price;
    }

    public ItemStack getItemStack() {
        return itemstack;
    }

    public UUID getOwnerUUID() {
        return owneruuid;
    }

    public void setPrice(int price) {
        this.price = price;

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("price", price);
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("price", price);
        config.set("owner", owneruuid.toString());


        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete() {
        configFile.delete();
        plugin.getMarketItemManager().getMarketItems().remove(this);
    }
}
