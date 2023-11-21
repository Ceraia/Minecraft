package dev.xdbl.xdblarenas;

import dev.xdbl.xdblarenas.arenas.ArenaManager;
import dev.xdbl.xdblarenas.commands.*;
import dev.xdbl.xdblarenas.listeners.*;
import dev.xdbl.xdblarenas.metrics.Metrics;
import dev.xdbl.xdblarenas.players.PlayerManager;
import dev.xdbl.xdblarenas.gui.ArenaSelectGUI;
import dev.xdbl.xdblarenas.scoreboards.EloScoreboard;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class XDBLArena extends JavaPlugin {

    private ArenaManager arenaManager;
    private InviteManager inviteManager;
    private ArenaSelectGUI arenaSelectGUI;
    private CommandGVG commandGVG;
    private CommandPVP commandPVP;
    private CommandArena commandArena;
    private PlayerManager playerManager;
    private EloScoreboard eloScoreboard;
    private CommandMod commandMod;
    private CommandTop commandTop;
    private CommandProfile commandProfile;

    int pluginId = 20303;

    Metrics metrics;

    public void onEnable() {
        metrics = new Metrics(this, pluginId);

        saveDefaultConfig();
        new File(getDataFolder(), "data/arenas").mkdirs();
        new File(getDataFolder(), "data/players").mkdirs();

        this.arenaManager = new ArenaManager(this);
        this.playerManager = new PlayerManager(this);
        this.inviteManager = new InviteManager();

        this.eloScoreboard = new EloScoreboard(this);
        this.arenaSelectGUI = new ArenaSelectGUI(this);

        this.commandGVG = new CommandGVG(this);
        this.commandPVP = new CommandPVP(this);
        this.commandArena = new CommandArena(this);
        this.commandMod = new CommandMod(this);
        this.commandTop = new CommandTop(this);
        this.commandProfile = new CommandProfile(this);

        new PlayerEloChangeListener(this);
        new ArenaFightListener(this);
        new PlayerInventoryListener(this);
        new ArenaBlockListener(this);
        new ArenaExplodeListener(this);

        Objects.requireNonNull(getCommand("pvp")).setExecutor(commandPVP);

        Objects.requireNonNull(getCommand("arena")).setExecutor(commandArena);

        Objects.requireNonNull(getCommand("gvg")).setExecutor(commandGVG);

        Objects.requireNonNull(getCommand("mod")).setExecutor(commandMod);

        Objects.requireNonNull(getCommand("top")).setExecutor(commandTop);
        Objects.requireNonNull(getCommand("leaderboard")).setExecutor(commandTop);

        Objects.requireNonNull(getCommand("profile")).setExecutor(commandProfile);
        Objects.requireNonNull(getCommand("stats")).setExecutor(commandProfile);
    }

    public void onDisable() {
        metrics.shutdown();
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public ArenaSelectGUI getArenaSelectGUI() {
        return arenaSelectGUI;
    }

    public CommandGVG getGroupManager() {
        return commandGVG;
    }
}
