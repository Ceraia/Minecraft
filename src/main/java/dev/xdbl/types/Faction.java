package dev.xdbl.types;

import dev.xdbl.Double;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Faction {

    private final Double plugin;
    private final String id;
    private final String name;
    private File configFile;

    private List<String> members;

    public Faction(
            Double plugin,
            String id,
            String name,
            List<String> members,
            File configFile
    ) {
        this.plugin = plugin;
        this.name = name;
        this.id = id;
        this.members = members;
        this.configFile = configFile;
    }

    public String getId() {
        return id;
    }

    public String getName(){
        return this.name;
    }

    public List<String> getMembers(){
        return this.members;
    }

    public void addMember(String name){
        // If members is null, create a new list
        if (this.members == null){
            this.members = new ArrayList<String>();
        }
        this.members.add(name);
        this.plugin.getPlayerManager().getDoublePlayer(name).setFaction(id);

        this.saveFaction();
    }

    public void removeMember(String name){
        this.members.remove(name);
    }

    public boolean saveFaction(){
        try {
            configFile = new File(plugin.getDataFolder(), "data/factions/" + name + ".yml");

            // Check if the file already exists
            if (!configFile.exists()) {
                configFile.createNewFile();
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Update or set the values
            config.set("name", name);
            config.set("id", id);

            config.set("members", members);

            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public File getConfigFile(){
        return this.configFile;
    }
}
