package dev.xdbl.managers;

import dev.xdbl.Double;
import dev.xdbl.types.Kingdom;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KingdomManager {
    private final List<Kingdom> kingdoms = new ArrayList<>();
    private final Double plugin;

    public KingdomManager(Double plugin) {
        this.plugin = plugin;

        // Load arenas
        File f = new File(plugin.getDataFolder(), "data/kingdoms");
        if (!f.exists()) {
            f.mkdirs();
            return;
        }

        File[] files = f.listFiles();
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);


            List<String> members = new ArrayList<String>(config.getStringList("members"));

            Kingdom kingdom = new Kingdom(plugin, config.getString("id"), config.getString("name"), members, file);
            kingdoms.add(kingdom);
        }

    }

    public List<Kingdom> getKingdoms() {
        return kingdoms;
    }

    public Kingdom getKingdom(String id) {
        for (Kingdom kingdom : kingdoms) {
            if (Objects.equals(kingdom.getId(), id)) {
                return kingdom;
            }
        }
        return null;
    }

    public Kingdom newKingdom(String name) {
        List<String> members = null;
        File file = new File(plugin.getDataFolder(), "data/kingdoms/" + name + ".yml");

        return new Kingdom(plugin, name, name, members, file);
    }

    public void removeKingdom(Kingdom kingdom) {
        kingdoms.remove(kingdom);
    }

    public void saveKingdoms() {
        plugin.getLogger().info("Saving kingdoms...");
        for (Kingdom kingdom : kingdoms) {
            kingdom.saveKingdom();
        }
    }
}
