package dev.xdbl.types;

import dev.xdbl.Double;
import dev.xdbl.listeners.PlayerEventListener;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DoublePlayer {

    private final Double plugin;
    private final UUID uuid;
    private final String name;
    private final UUID faction;
    private int draws;
    private boolean pvpbanned;
    private int wins;
    private int losses;
    private boolean arenabanned;
    private final File configFile;
    private final List<String> logs;
    private int elo;
    private long lastFought;


    public DoublePlayer(
            Double plugin,
            String name,
            UUID uuid,
            int elo,
            boolean arenabanned,
            boolean pvpbanned,
            int wins,
            int losses,
            int draws,
            List<String> logs,
            long lastFought,
            UUID faction,
            File configFile

    ) {
        this.plugin = plugin;

        this.name = name;
        this.uuid = uuid;
        this.elo = elo;
        this.arenabanned = arenabanned;
        this.pvpbanned = pvpbanned;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.logs = logs;
        this.lastFought = lastFought;
        this.faction = faction;
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
            PlayerEventListener eloChangeEvent = new PlayerEventListener(Bukkit.getPlayer(uuid), this);
            Bukkit.getServer().getPluginManager().callEvent(eloChangeEvent);

            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean pvpBan(){
        pvpbanned = !pvpbanned;
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("pvpbanned", pvpbanned);
        try {
            // Trigger the custom event when Elo changes
            PlayerEventListener eloChangeEvent = new PlayerEventListener(Bukkit.getPlayer(uuid), this);
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
            PlayerEventListener eloChangeEvent = new PlayerEventListener(Bukkit.getPlayer(uuid), this);
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

    public int draws() {
        return draws;
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

    public void setLastFought(long lastFought) {
        this.lastFought = lastFought;
    }

    public Faction getFaction() {
        return plugin.getFactionManager().getFaction(faction);
    }
}
