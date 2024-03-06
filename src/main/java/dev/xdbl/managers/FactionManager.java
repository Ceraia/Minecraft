package dev.xdbl.managers;

import dev.xdbl.Double;
import dev.xdbl.types.Faction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class FactionManager {
    private final List<Faction> factions = new ArrayList<>();
    private final Double plugin;

    public FactionManager(Double plugin) {
        this.plugin = plugin;

        // Load arenas
        File f = new File(plugin.getDataFolder(), "data/factions");
        if (!f.exists()) {
            f.mkdirs();
            return;
        }

        File[] files = f.listFiles();
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            UUID uuid = UUID.fromString(config.getString("id"));
            String name = config.getString("name");

            List<String> members = new ArrayList<String>(config.getStringList("members"));

            Faction faction = new Faction(plugin, uuid, name, members, file);
            factions.add(faction);
        }

    }

    public List<Faction> getFactions() {
        return factions;
    }

    public Faction getFaction(UUID uuid) {
        for (Faction faction : factions) {
            if (faction.getUUID() == uuid) {
                return faction;
            }
        }
        return null;
    }

    public Faction getFaction(String name) {
        for (Faction faction : factions) {
            if (faction.getName().equalsIgnoreCase(name)) {
                return faction;
            }
        }
        return null;
    }

    public Faction newFaction(String name){
        List<String> members = null;
        File file = new File(plugin.getDataFolder(), "data/factions/" + name + ".yml");

        return new Faction(plugin, UUID.randomUUID(), name, members, file);
    }

    public void removeFaction(Faction faction) {
        factions.remove(faction);
    }

    public void saveFactions(){
        plugin.getLogger().info("Saving factions...");
        for(Faction faction : factions){
            faction.saveFaction();
        }
    }
}
