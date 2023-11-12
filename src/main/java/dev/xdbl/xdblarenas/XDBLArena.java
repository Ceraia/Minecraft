package dev.xdbl.xdblarenas;

import dev.xdbl.xdblarenas.arenas.ArenaManager;
import dev.xdbl.xdblarenas.commands.*;
import dev.xdbl.xdblarenas.listeners.*;
import dev.xdbl.xdblarenas.players.PlayerManager;
import dev.xdbl.xdblarenas.gui.ArenaSelectGUI;
import dev.xdbl.xdblarenas.scoreboards.EloScoreboard;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

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

    public void onEnable() {
        saveDefaultConfig();
        new File(getDataFolder(), "data/arenas").mkdirs();

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
        new ArenaItemListener(this);
        new ArenaBlockListener(this);
        new ArenaExplodeListener(this);

        getCommand("pvp").setExecutor(commandPVP);

        getCommand("arena").setExecutor(commandArena);

        getCommand("gvg").setExecutor(commandGVG);

        getCommand("mod").setExecutor(commandMod);

        getCommand("top").setExecutor(commandTop);
        getCommand("leaderboard").setExecutor(commandTop);

        getCommand("profile").setExecutor(commandProfile);
        getCommand("stats").setExecutor(commandProfile);
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
