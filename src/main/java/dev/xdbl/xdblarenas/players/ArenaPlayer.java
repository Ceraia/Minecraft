package dev.xdbl.xdblarenas.players;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.events.PlayerEloChangeEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaPlayer {

    private final XDBLArena plugin;
    private final UUID uuid;
    private boolean pvpbanned;
    private int wins;
    private int losses;
    private boolean arenabanned;
    private File configFile;
    private List<String> logs = new ArrayList<>();
    private int elo;
    private boolean scoreboard;

    // after ready
    private Location spawnPoint1, spawnPoint2;
    private boolean isReady = false;


    public ArenaPlayer(XDBLArena plugin, UUID uuid, int elo, boolean scoreboard, boolean arenabanned, boolean pvpbanned, int wins, int losses, List<String> logs, File configFile) {
        this.plugin = plugin;

        this.uuid = uuid;
        this.elo = elo;
        this.arenabanned = arenabanned;
        this.pvpbanned = pvpbanned;
        this.scoreboard = scoreboard;
        this.wins = wins;
        this.losses = losses;
        this.logs = logs;

        this.configFile = configFile;
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("elo", elo);
        try {
            // Trigger the custom event when Elo changes
            PlayerEloChangeEvent eloChangeEvent = new PlayerEloChangeEvent(Bukkit.getPlayer(uuid), this);
            Bukkit.getServer().getPluginManager().callEvent(eloChangeEvent);

            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean scoreboard() {
        return scoreboard;
    }

    public boolean toggleScoreboard() {
        scoreboard = !scoreboard;
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("scoreboard", scoreboard);
        try {
            // Trigger the custom event when Elo changes
            PlayerEloChangeEvent eloChangeEvent = new PlayerEloChangeEvent(Bukkit.getPlayer(uuid), this);
            Bukkit.getServer().getPluginManager().callEvent(eloChangeEvent);

            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scoreboard;
    }

    public boolean pvpBan(){
        pvpbanned = !pvpbanned;
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("pvpbanned", pvpbanned);
        try {
            // Trigger the custom event when Elo changes
            PlayerEloChangeEvent eloChangeEvent = new PlayerEloChangeEvent(Bukkit.getPlayer(uuid), this);
            Bukkit.getServer().getPluginManager().callEvent(eloChangeEvent);

            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pvpbanned;
    }

    public boolean arenaBan(){
        arenabanned = !arenabanned;
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("arenabanned", arenabanned);
        try {
            // Trigger the custom event when Elo changes
            PlayerEloChangeEvent eloChangeEvent = new PlayerEloChangeEvent(Bukkit.getPlayer(uuid), this);
            Bukkit.getServer().getPluginManager().callEvent(eloChangeEvent);

            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return arenabanned;
    }

    public boolean pvpBanned() {
        return pvpbanned;
    }

    public boolean arenaBanned() {
        return arenabanned;
    }

    public int wins() {
        return wins;
    }

    public int losses() {
        return losses;
    }

    public void addWin() {
        wins++;
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("wins", wins);
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addLoss() {
        losses++;
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("losses", losses);
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addLog(String string){
        logs.add(string);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("logs", logs);
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
