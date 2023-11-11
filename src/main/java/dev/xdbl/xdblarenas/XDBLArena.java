package dev.xdbl.xdblarenas;

import dev.xdbl.xdblarenas.arenas.ArenaManager;
import dev.xdbl.xdblarenas.listeners.*;
import dev.xdbl.xdblarenas.players.PlayerManager;
import dev.xdbl.xdblarenas.commands.CommandArena;
import dev.xdbl.xdblarenas.commands.CommandPVP;
import dev.xdbl.xdblarenas.commands.CommandGVG;
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
    private CommandArena commmandArena;
    private PlayerManager playerManager;
    private EloScoreboard eloScoreboard;

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
        this.commmandArena = new CommandArena(this);

        new PlayerEloChangeListener(this);
        new ArenaFightListener(this);
        new ArenaItemListener(this);
        new ArenaBlockListener(this);
        new ArenaExplodeListener(this);

        getCommand("pvp").setExecutor(commandPVP);
        getCommand("arena").setExecutor(commmandArena);
        getCommand("gvg").setExecutor(commandGVG);
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
