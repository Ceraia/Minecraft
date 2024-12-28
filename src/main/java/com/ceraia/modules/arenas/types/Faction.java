package com.ceraia.modules.arenas.types;

import com.ceraia.modules.arenas.Double;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Faction {

    private final Double plugin;
    private final UUID uuid;
    private final String name;
    private File configFile;

    private List<String> members;

    public Faction(
            Double plugin,
            UUID uuid,
            String name,
            List<String> members,
            File configFile
    ) {
        this.plugin = plugin;
        this.name = name;
        this.uuid = uuid;
        this.members = members;
        this.configFile = configFile;
    }

    public UUID getUUID() {
        return uuid;
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
        this.plugin.getPlayerManager().getDoublePlayer(name).setFaction(this.uuid);

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
            config.set("id", uuid.toString());

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
