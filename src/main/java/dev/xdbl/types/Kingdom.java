package dev.xdbl.types;

import dev.xdbl.Double;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Kingdom {

    private final Double plugin;
    private final String id;
    private String name;
    private File configFile;

    private List<String> members;

    public Kingdom(
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

    public String getName() {
        return this.name;
    }

    public List<String> getMembers() {
        return this.members;
    }

    public void addMember(String name) {
        // If members is null, create a new list
        if (this.members == null) {
            this.members = new ArrayList<String>();
        }
        this.members.add(name);
        this.plugin.getPlayerManager().getDoublePlayer(name).setKingdom(id);

        this.saveKingdom();
    }

    public boolean removeMember(String name) {
        if (!this.members.contains(name)) {
            return false;
        }
        DoublePlayer doublePlayer = this.plugin.getPlayerManager().getDoublePlayer(name);
        doublePlayer.setKingdom("none");
        doublePlayer.setRank(0);
        doublePlayer.savePlayer();
        this.members.remove(name);
        this.saveKingdom();
        return true;
    }

    public int promoteMember(String name) {
        if (!this.members.contains(name)) {
            return 5; // 5 is the code for "player not found"
        }

        DoublePlayer doublePlayer = this.plugin.getPlayerManager().getDoublePlayer(name);
        doublePlayer.setRank(doublePlayer.getRank() + 1);
        doublePlayer.savePlayer();
        return doublePlayer.getRank();
    }

    public int demoteMember(String name) {
        if (!this.members.contains(name)) {
            return 5; // 5 is the code for "player not found"
        }

        DoublePlayer doublePlayer = this.plugin.getPlayerManager().getDoublePlayer(name);
        doublePlayer.setRank(doublePlayer.getRank() - 1);
        doublePlayer.savePlayer();
        return doublePlayer.getRank();
    }

    public void setMemberRank(String name, int rank) {
        if (!this.members.contains(name)) {
            return;
        }

        DoublePlayer doublePlayer = this.plugin.getPlayerManager().getDoublePlayer(name);
        doublePlayer.setRank(rank);
        doublePlayer.savePlayer();
    }

    public boolean kickMember(String name) {
        if (!this.members.contains(name)) {
            return false;
        }

        this.removeMember(name);
        return true;
    }

    public boolean disband() {
        for (String member : this.members) {
            this.removeMember(member);
        }
        this.plugin.getKingdomManager().removeKingdom(this);
        this.configFile.delete();
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean invitePlayer(String name) {
        if (this.members.contains(name)) {
            return false;
        }

        this.plugin.getKingdomManager().invitePlayer(this.plugin.getServer().getPlayer(name), this);
        return true;
    }

    public boolean acceptInvite(Player player) {
        if (!this.plugin.getKingdomManager().getInvites().containsKey(player)) {
            return false;
        }

        this.addMember(player.getName());
        this.plugin.getKingdomManager().getInvites().remove(player);
        return true;
    }

    public int getMemberRank(String name) {
        if (!this.members.contains(name)) {
            return 5; // 5 is the code for "player not found"
        }

        return this.plugin.getPlayerManager().getDoublePlayer(name).getRank();
    }

    public void saveKingdom() {
        try {
            configFile = new File(plugin.getDataFolder(), "data/kingdoms/" + name + ".yml");

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
        }
    }

    public File getConfigFile() {
        return this.configFile;
    }
}
