package dev.xdbl.types;

import dev.xdbl.Double;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Faction {

    private final Double plugin;
    private final UUID uuid;
    private final String name;
    private File configFile;

    private List<UUID> members;

    public Faction(
            Double plugin,
            UUID uuid,
            String name,
            List<UUID> members,
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

    public List<UUID> getMembers(){
        return this.members;
    }

    public void addMember(UUID uuid){
        this.members.add(uuid);
    }

    public void removeMember(UUID uuid){
        this.members.remove(uuid);
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
