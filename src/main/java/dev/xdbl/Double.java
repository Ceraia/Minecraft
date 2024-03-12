package dev.xdbl;

import dev.xdbl.commands.arena.*;
import dev.xdbl.commands.kingdoms.CommandKingdom;
import dev.xdbl.commands.marriage.CommandMarry;
import dev.xdbl.commands.misc.CommandMod;
import dev.xdbl.commands.misc.CommandSit;
import dev.xdbl.commands.misc.CommandVersion;
import dev.xdbl.listeners.*;
import dev.xdbl.managers.*;
import dev.xdbl.misc.Metrics;
import dev.xdbl.types.ArenaSelectGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Double extends JavaPlugin {

    private ArenaManager arenaManager;
    private InviteManager inviteManager;
    private ArenaSelectGUI arenaSelectGUI;
    private CommandGVG commandGVG;
    private PlayerManager playerManager;
    Metrics metrics;
    private KingdomManager kingdomManager;
    private ChairManager chairManager;
    private CommandMarry marriageManager;

    public void onEnable() {
        metrics = new Metrics(this, 20303);

        saveDefaultConfig();
        new File(getDataFolder(), "data/arenas").mkdirs();
        new File(getDataFolder(), "data/items").mkdirs();
        new File(getDataFolder(), "data/users").mkdirs();
        new File(getDataFolder(), "data/kingdoms").mkdirs();

        // Managers
        this.kingdomManager = new KingdomManager(this);
        this.arenaManager = new ArenaManager(this);
        this.playerManager = new PlayerManager(this);
        this.inviteManager = new InviteManager();
        this.chairManager = new ChairManager(this);
        this.marriageManager = new CommandMarry(this);
        new EloScoreboardManager(this);

        this.arenaSelectGUI = new ArenaSelectGUI(this);
        this.commandGVG = new CommandGVG(this);

        // Command
        new CommandPVP(this);
        new CommandArena(this);
        new CommandMod(this);
        new CommandTop(this);
        new CommandProfile(this);
        new CommandVersion(this);
        new CommandKingdom(this);
        new CommandSit(this);

        // Listeners
        new PlayerEloChangeListener(this);
        new ArenaFightListener(this);
        new PlayerInventoryListener(this);
        new ArenaBlockListener(this);
        new ArenaExplodeListener(this);
        new SpellsListener(this);
        new ReviveListener(this);
    }

    public void onDisable() {
        metrics.shutdown();
        playerManager.savePlayers();
        kingdomManager.saveKingdoms();
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

    public KingdomManager getKingdomManager() {
        return kingdomManager;
    }

    public ChairManager getChairManager() {
        return chairManager;
    }

    public CommandMarry getMarriageManager() {
        return marriageManager;
    }

    public void badUsage(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid usage"));
    }

    public void noPermission(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to execute this command"));
    }
}
