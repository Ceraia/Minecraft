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
    private boolean arenabanned;
    private File configFile;
    private int elo;
    private boolean scoreboard;

    // after ready
    private Location spawnPoint1, spawnPoint2;
    private boolean isReady = false;


    public ArenaPlayer(XDBLArena plugin, UUID uuid, int elo, boolean scoreboard, boolean arenabanned, boolean pvpbanned, File configFile) {
        this.plugin = plugin;

        this.uuid = uuid;
        this.elo = elo;
        this.arenabanned = arenabanned;
        this.pvpbanned = pvpbanned;
        this.scoreboard = scoreboard;

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
}
