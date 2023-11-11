package dev.xdbl.xdblarenas.players;

import dev.xdbl.xdblarenas.XDBLArena;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerManager {

    private final XDBLArena plugin;
    private final List<ArenaPlayer> arenaPlayers = new ArrayList<>();

    public PlayerManager(XDBLArena plugin) {
        this.plugin = plugin;

        // Load arenaPlayers
        File f = new File(plugin.getDataFolder(), "data/arenaPlayers");
        if (!f.exists()) {
            f.mkdirs();
            return;
        }

        File[] files = f.listFiles();
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            String uuid = config.getString("uuid");
            String elo = config.getString("elo");

            ArenaPlayer arenaPlayer = new ArenaPlayer(plugin, UUID.fromString(uuid), Integer.parseInt(elo), false, file);
            arenaPlayers.add(arenaPlayer);
        }
    }

    public List<ArenaPlayer> getArenaPlayers() {
        return arenaPlayers;
    }

    public ArenaPlayer getArenaPlayer(UUID playerUUID) {
        // Check if the player is already in the list
        for (ArenaPlayer arenaPlayer : arenaPlayers) {
            if (arenaPlayer.getUUID().equals(playerUUID)) {
                return arenaPlayer;
            }
        }

        // If the player is not in the list, create a new ArenaPlayer
        ArenaPlayer newPlayer = createNewArenaPlayer(playerUUID);
        arenaPlayers.add(newPlayer);

        return newPlayer;
    }

    public void PlayerKill(UUID playerKiller, UUID playerVictim) {
        ArenaPlayer killer = getArenaPlayer(playerKiller);
        ArenaPlayer victim = getArenaPlayer(playerVictim);

        // Constants for the Elo calculation
        double kFactor = 32.0;
        double expectedScoreKiller = 1.0 / (1.0 + Math.pow(10, (victim.getElo() - killer.getElo()) / 400.0));
        double expectedScoreVictim = 1.0 / (1.0 + Math.pow(10, (killer.getElo() - victim.getElo()) / 400.0));

        // Update Elo ratings
        int newEloKiller = (int) (killer.getElo() + kFactor * (1.0 - expectedScoreKiller));
        int newEloVictim = (int) (victim.getElo() + kFactor * (0.0 - expectedScoreVictim));

        // Set new Elo ratings
        killer.setElo(newEloKiller);
        victim.setElo(newEloVictim);
    }

    public int CalculateWinChance(UUID playerKiller, UUID playerVictim){
        ArenaPlayer killer = getArenaPlayer(playerKiller);
        ArenaPlayer victim = getArenaPlayer(playerVictim);

        double expectedScoreKiller = 1.0 / (1.0 + Math.pow(10, (victim.getElo() - killer.getElo()) / 400.0));
        int winChance = (int) (expectedScoreKiller * 100);

        return winChance;
    }

    private ArenaPlayer createNewArenaPlayer(UUID playerUUID) {
        File configFile = new File(plugin.getDataFolder(), "data/arenaPlayers/" + playerUUID + ".yml");
        try {
            configFile.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Set default values or load from other sources as needed
            int defaultElo = 1500;

            config.set("uuid", playerUUID.toString());
            config.set("elo", defaultElo);
            config.set("scoreboard", true);

            config.save(configFile);

            return new ArenaPlayer(plugin, playerUUID, defaultElo, false, configFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Handle the exception based on your needs
        }
    }
}
