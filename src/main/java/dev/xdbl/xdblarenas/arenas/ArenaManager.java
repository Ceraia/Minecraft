package dev.xdbl.xdblarenas.arenas;

import dev.xdbl.xdblarenas.XDBLArena;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaManager {

    private final XDBLArena plugin;

    private final List<Arena> arenas = new ArrayList<>();
    private final Map<Player, Arena> playersInArena = new HashMap<>();

    public ArenaManager(XDBLArena plugin) {
        this.plugin = plugin;

        // Load arenas
        File f = new File(plugin.getDataFolder(), "data/arenas");
        if (!f.exists()) {
            f.mkdirs();
            return;
        }

        File[] files = f.listFiles();
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            String name = config.getString("name");
            String owner = config.getString("owner");
            Location spawnPoint1 = config.getLocation("spawnPoint1");
            Location spawnPoint2 = config.getLocation("spawnPoint2");
            boolean totems = config.getBoolean("totems", true);
            boolean isPublic = config.getBoolean("public", false);

            Arena arena = new Arena(plugin, name, owner, spawnPoint1, spawnPoint2, isPublic, totems, file);
            arenas.add(arena);
        }

    }

    public List<Arena> getArenas() {
        return arenas;
    }

    public Arena getArena(Player player) {
        return playersInArena.get(player);
    }

    public Arena getArena(String name) {
        for (Arena arena : arenas) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        return null;
    }
    

    public void addArena(Arena arena) {
        arenas.add(arena);
        arena.saveArena();
    }

    public void removeArena(Arena arena) {
        arenas.remove(arena);
    }

    public void addPlayerToArena(Player player, Arena arena) {
        playersInArena.put(player, arena);
    }

    public void removePlayerFromArena(Player player) {
        playersInArena.remove(player);
    }
}
