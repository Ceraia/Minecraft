package dev.xdbl;

import dev.xdbl.types.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Objects;

public class Utils {

    public static void revertInventory(Double plugin, Player pl, Arena arena) {
        try {
            File file = new File(plugin.getDataFolder(), "data/pinventory_" + arena.getName() + "_" + pl.getName() + ".yml");

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            ItemStack[] content = new ItemStack[pl.getInventory().getContents().length];
            try {
                for (String s : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
                    int i = Integer.parseInt(s);
                    content[i] = config.getItemStack("items." + s);
                }
            } catch (Exception e) {
                System.out.println("Problem loading player inventories.");
            }

            pl.getInventory().setContents(content);

            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Problem loading player inventory");
        }
    }

    public static void teleportPlayerToSpawn(Double plugin, Player player, Arena arena) {
        Location l = arena.getPlayerPriorLocation(player);
        player.teleport(l);
    }
}
