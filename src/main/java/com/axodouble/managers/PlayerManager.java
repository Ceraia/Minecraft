package com.axodouble.managers;

import com.axodouble.Double;
import com.axodouble.types.DoublePlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

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
                    config.getString("race", "human"),
                    config.getString("faction", null),
                    config.getString("married", null),
                    UUID.fromString(Objects.requireNonNull(config.getString("uuid"))),//UUID.fromString(file.getName().split("\\.")[0]),
                    config.getInt("elo", 1500),
                    config.getBoolean("arenabanned", false),
                    config.getBoolean("pvpbanned", false),
                    config.getInt("wins", 0),
                    config.getInt("losses", 0),
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

    public DoublePlayer getDoublePlayer(Player player) {
        // Check if the player is already in the list
        for (DoublePlayer doublePlayer : doublePlayers) {
            if (doublePlayer.getUUID().equals(player.getUniqueId())) {
                return doublePlayer;
            }
        }

        DoublePlayer newPlayer = createNewDoublePlayer(player.getUniqueId());
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

        // Update scoreboard

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboardDefault = scoreboardManager.getNewScoreboard();
        Objective objectivePlayerList = scoreboardDefault.registerNewObjective("eloObjectivePlayerList", "dummy", MiniMessage.miniMessage().deserialize("Top Arena Players"));
        Objective objectiveBelowName = scoreboardDefault.registerNewObjective("eloObjectiveBelowName", "dummy", MiniMessage.miniMessage().deserialize("<green>ELO"));

        // Get all online players and set their score to their Elo rating
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(onlinePlayer.getUniqueId());

            objectivePlayerList.getScore(onlinePlayer.getName()).setScore(doublePlayer.getElo());
            objectiveBelowName.getScore(onlinePlayer.getName()).setScore(doublePlayer.getElo());
            objectivePlayerList.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            objectiveBelowName.setDisplaySlot(DisplaySlot.BELOW_NAME);

            onlinePlayer.setScoreboard(scoreboardDefault);
        }

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
            config.set("race", "human");
            config.set("faction", null);
            config.set("married", null);
            config.set("uuid", playerUUID.toString());
            config.set("elo", defaultElo);
            config.set("arenabanned", false);
            config.set("pvpbanned", false);
            config.set("wins", 0);
            config.set("losses", 0);
            config.set("logs", new ArrayList<String>());

            config.save(configFile);

            return new DoublePlayer(
                    plugin,
                    Bukkit.getPlayer(playerUUID).getName(),
                    "human",
                    null,
                    null,
                    playerUUID,
                    defaultElo,
                    false,
                    false,
                    0,
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
