package dev.xdbl;

import dev.xdbl.commands.*;
import dev.xdbl.listeners.*;
import dev.xdbl.managers.*;
import dev.xdbl.misc.Metrics;
import dev.xdbl.managers.*;
import dev.xdbl.commands.*;
import dev.xdbl.listeners.*;
import dev.xdbl.types.ArenaSelectGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class Double extends JavaPlugin {

    private ArenaManager arenaManager;
    private InviteManager inviteManager;
    private MarketItemManager marketItemManager;
    private EloScoreboardManager eloScoreboardManager;
    private ArenaSelectGUI arenaSelectGUI;
    private CommandGVG commandGVG;
    private CommandPVP commandPVP;
    private CommandArena commandArena;
    private PlayerManager playerManager;
    private CommandMod commandMod;
    private CommandTop commandTop;
    private CommandProfile commandProfile;
    private CommandBidenBlast commandBidenBlast;
    Metrics metrics;

    public void onEnable() {
        metrics = new Metrics(this, 20303);

        saveDefaultConfig();
        new File(getDataFolder(), "data/arenas").mkdirs();
        new File(getDataFolder(), "data/items").mkdirs();
        new File(getDataFolder(), "data/players").mkdirs();

        this.arenaManager = new ArenaManager(this);
        this.playerManager = new PlayerManager(this);
        this.marketItemManager = new MarketItemManager(this);
        this.eloScoreboardManager = new EloScoreboardManager(this);
        this.inviteManager = new InviteManager();

        this.arenaSelectGUI = new ArenaSelectGUI(this);

        this.commandGVG = new CommandGVG(this);
        this.commandPVP = new CommandPVP(this);
        this.commandArena = new CommandArena(this);
        this.commandMod = new CommandMod(this);
        this.commandTop = new CommandTop(this);
        this.commandProfile = new CommandProfile(this);
        this.commandBidenBlast = new CommandBidenBlast(this);

        new PlayerEloChangeListener(this);
        new ArenaFightListener(this);
        new PlayerInventoryListener(this);
        new ArenaBlockListener(this);
        new ArenaExplodeListener(this);
        new SpellsListener(this);

        Objects.requireNonNull(getCommand("pvp")).setExecutor(commandPVP);

        Objects.requireNonNull(getCommand("arena")).setExecutor(commandArena);

        Objects.requireNonNull(getCommand("gvg")).setExecutor(commandGVG);

        Objects.requireNonNull(getCommand("mod")).setExecutor(commandMod);

        Objects.requireNonNull(getCommand("top")).setExecutor(commandTop);
        Objects.requireNonNull(getCommand("leaderboard")).setExecutor(commandTop);

        Objects.requireNonNull(getCommand("profile")).setExecutor(commandProfile);
        Objects.requireNonNull(getCommand("stats")).setExecutor(commandProfile);

        Objects.requireNonNull(getCommand("bidenblast")).setExecutor(commandBidenBlast);
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

    public MarketItemManager getMarketItemManager() {
        return marketItemManager;
    }

    public ArenaSelectGUI getArenaSelectGUI() {
        return arenaSelectGUI;
    }

    public CommandGVG getGroupManager() {
        return commandGVG;
    }

}
