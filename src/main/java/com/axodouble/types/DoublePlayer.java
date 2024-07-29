package com.axodouble.types;

import com.axodouble.Double;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class DoublePlayer {
    private final Double plugin;
    private final UUID uuid;
    private final String name;
    private final int draws;
    private boolean pvpbanned;
    private int wins;
    private int losses;
    private boolean arenabanned;
    private final File configFile;
    private final List<String> logs;
    private int elo;
    private String marriedname;
    private String race;
    private String faction;


    public DoublePlayer(
            Double plugin,
            String name,
            String race,
            String faction,
            String marriedname,
            UUID uuid,
            int elo,
            boolean arenabanned,
            boolean pvpbanned,
            int wins,
            int losses,
            int draws,
            List<String> logs,
            File configFile

    ) {
        this.plugin = plugin;

        this.name = name;
        this.race = race;
        this.faction = faction;
        this.marriedname = marriedname;
        this.uuid = uuid;
        this.elo = elo;
        this.arenabanned = arenabanned;
        this.pvpbanned = pvpbanned;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.logs = logs;
        this.configFile = configFile;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
        this.savePlayer();
    }

    public boolean pvpBan() {
        pvpbanned = !pvpbanned;
        this.savePlayer();
        return pvpbanned;
    }

    public boolean arenaBan() {
        arenabanned = !arenabanned;
        this.savePlayer();
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
        this.savePlayer();
    }

    public void addLoss() {
        losses++;
        this.savePlayer();
    }

    public void addLog(String string) {
        logs.add(string);
        this.savePlayer();
    }

    public void savePlayer() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("name", name);
        config.set("race", race);
        config.set("faction", faction);
        config.set("married", marriedname);
        config.set("uuid", uuid.toString());
        config.set("elo", elo);
        config.set("arenabanned", arenabanned);
        config.set("pvpbanned", pvpbanned);
        config.set("wins", wins);
        config.set("losses", losses);
        config.set("logs", logs);

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void divorce() {
        marriedname = null;
        this.savePlayer();
    }

    public void marry(String name) {
        marriedname = name;
        this.savePlayer();
    }

    public String getMarriedName() {
        return marriedname;
    }

    public boolean isMarried() {
        return marriedname != null;
    }

    public String getPartner() {
        return marriedname;
    }

    public String getRace() {
        return race;
    }

    public String getFaction() {
        return faction;
    }
}
