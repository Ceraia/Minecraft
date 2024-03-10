package dev.xdbl.managers;

import dev.xdbl.Double;
import dev.xdbl.types.DoublePlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PlayerManager {

    private final Double plugin;
    private final List<DoublePlayer> doublePlayers = new ArrayList<>();

    public PlayerManager(Double plugin) {
        this.plugin = plugin;

        // Load arenaPlayers
        File f = new File(plugin.getDataFolder(), "data/users");
        if (!f.exists()) {
            f.mkdirs();
            return;
        }

        File[] files = f.listFiles();
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            DoublePlayer doublePlayer = new DoublePlayer(
                    plugin,
                    config.getString("name"),
                    config.getString("marriedname", null),
                    UUID.fromString(Objects.requireNonNull(config.getString("uuid"))),//UUID.fromString(file.getName().split("\\.")[0]),
                    config.getInt("elo", 1500),
                    config.getBoolean("arenabanned", false),
                    config.getBoolean("pvpbanned", false),
                    config.getInt("wins", 0),
                    config.getInt("losses", 0),
                    config.getInt("draws", 0),
                    config.getStringList("logs"),
                    config.getInt("lastSeen", (int) (System.currentTimeMillis() / 1000L)),
                    config.getString("kingdom", "none"),
                    config.getInt("rank", 0),
                    file
            );
            doublePlayers.add(doublePlayer);
        }
    }

    public List<DoublePlayer> getDoublePlayers() {
        return doublePlayers;
    }

    public DoublePlayer getDoublePlayer(UUID playerUUID) {
        // Check if the player is already in the list
        for (DoublePlayer doublePlayer : doublePlayers) {
            if (doublePlayer.getUUID().equals(playerUUID)) {
                return doublePlayer;
            }
        }

        DoublePlayer newPlayer = createNewDoublePlayer(playerUUID);
        doublePlayers.add(newPlayer);

        return newPlayer;
    }

    public DoublePlayer getDoublePlayer(String playerName) {
        // Check if the player is already in the list
        for (DoublePlayer doublePlayer : doublePlayers) {
            if (doublePlayer.getName().equals(playerName)) {
                return doublePlayer;
            }
        }

        DoublePlayer newPlayer = createNewDoublePlayer(Objects.requireNonNull(Bukkit.getPlayer(playerName)).getUniqueId());
        doublePlayers.add(newPlayer);
        return newPlayer;
    }


    public void PlayerKill(UUID playerKiller, UUID playerVictim) {
        DoublePlayer killer = getDoublePlayer(playerKiller);
        DoublePlayer victim = getDoublePlayer(playerVictim);

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


    public int CalculateWinChance(UUID playerKiller, UUID playerVictim) {
        DoublePlayer killer = getDoublePlayer(playerKiller);
        DoublePlayer victim = getDoublePlayer(playerVictim);

        double expectedScoreKiller = 1.0 / (1.0 + Math.pow(10, (victim.getElo() - killer.getElo()) / 400.0));
        int winChance = (int) (expectedScoreKiller * 100);

        return winChance;
    }

    public int CalculateLossChance(UUID playerKiller, UUID playerVictim) {
        DoublePlayer killer = getDoublePlayer(playerKiller);
        DoublePlayer victim = getDoublePlayer(playerVictim);

        double expectedScoreVictim = 1.0 / (1.0 + Math.pow(10, (killer.getElo() - victim.getElo()) / 400.0));
        int lossChance = (int) (expectedScoreVictim * 100);

        return lossChance;
    }

    private DoublePlayer createNewDoublePlayer(UUID playerUUID) {
        String playerName = Objects.requireNonNull(Bukkit.getPlayer(playerUUID)).getName();
        File configFile = new File(plugin.getDataFolder(), "data/users/" + playerName + ".yml");
        try {
            configFile.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Set default values or load from other sources as needed

            int defaultElo = 1500;

            config.set("name", playerName);
            config.set("marriedname", null);
            config.set("uuid", playerUUID.toString());
            config.set("elo", defaultElo);
            config.set("arenabanned", false);
            config.set("pvpbanned", false);
            config.set("wins", 0);
            config.set("losses", 0);
            config.set("logs", new ArrayList<String>());
            config.set("kingdom", "none");
            config.set("rank", 0);

            config.save(configFile);

            return new DoublePlayer(
                    plugin,
                    Objects.requireNonNull(Bukkit.getPlayer(playerUUID)).getName(),
                    null,
                    playerUUID,
                    defaultElo,
                    false,
                    false,
                    0,
                    0,
                    0,
                    new ArrayList<String>(),
                    (int) (System.currentTimeMillis() / 1000L),
                    "none",
                    0,
                    configFile
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Handle the exception based on your needs
        }
    }

    public DoublePlayer getPlayer(UUID uniqueId) {
        for (DoublePlayer doublePlayer : doublePlayers) {
            if (doublePlayer.getUUID().equals(uniqueId)) {
                return doublePlayer;
            }
        }
        return null;
    }

    public DoublePlayer getPlayer(String name) {
        for (DoublePlayer doublePlayer : doublePlayers) {
            if (doublePlayer.getName().equalsIgnoreCase(name)) {
                return doublePlayer;
            }
        }
        return null;
    }

    public void savePlayers() {
        plugin.getLogger().info("Saving players...");
        for (DoublePlayer doublePlayer : doublePlayers) {
            doublePlayer.savePlayer();
        }
    }
}
