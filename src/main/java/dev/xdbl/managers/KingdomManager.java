package dev.xdbl.managers;

import dev.xdbl.Double;
import dev.xdbl.types.Kingdom;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class KingdomManager {
    private final List<Kingdom> kingdoms = new ArrayList<>();
    private final Double plugin;

    private Map<Player, Kingdom> invites;

    public KingdomManager(Double plugin) {
        this.plugin = plugin;

        // Initialize the invites map
        this.invites = new HashMap<>();

        // Load kingdoms
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

    public List<Kingdom> getInvites(Player player) {
        List<Kingdom> invites = new ArrayList<>();

        for (Map.Entry<Player, Kingdom> entry : this.invites.entrySet()) {
            if (entry.getKey().equals(player)) {
                invites.add(entry.getValue());
            }
        }

        return invites;
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

    public Kingdom getKingdom(Player player) {
        return plugin.getPlayerManager().getDoublePlayer(player.getName()).getKingdom();
    }

    public Kingdom newKingdom(String name) {
        List<String> members = null;
        File file = new File(plugin.getDataFolder(), "data/kingdoms/" + name + ".yml");

        String id = name.toLowerCase().replace(" ", "_");

        Kingdom kingdom = new Kingdom(plugin, id, name, members, file);
        kingdoms.add(kingdom);
        return kingdom;
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

    public boolean invitePlayer(Player player, Kingdom kingdom) {
        if (invites.containsKey(player)) {
            return false;
        }
        invites.put(player, kingdom);
        return true;
    }

    public boolean acceptInvite(Player player, Kingdom kingdom) {
        if (!invites.containsKey(player)) {
            return false;
        }
        kingdom.addMember(player.getName());
        invites.remove(player);
        return true;
    }

    public Map<Player, Kingdom> getInvites() {
        return invites;
    }
}
