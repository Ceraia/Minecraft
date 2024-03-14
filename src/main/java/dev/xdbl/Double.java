package dev.xdbl;

import dev.xdbl.listeners.PlayerInventoryListener;
import dev.xdbl.listeners.ReviveListener;
import dev.xdbl.listeners.SpellsListener;
import dev.xdbl.managers.ArenaManager;
import dev.xdbl.managers.InviteManager;
import dev.xdbl.managers.KingdomManager;
import dev.xdbl.managers.PlayerManager;
import dev.xdbl.modules.*;
import dev.xdbl.types.ArenaSelectGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Double extends JavaPlugin {

    private ArenaManager arenaManager;
    private InviteManager inviteManager;
    private ArenaSelectGUI arenaSelectGUI;
    private PlayerManager playerManager;
    private KingdomManager kingdomManager;
    private ModuleSeating moduleSeating;
    private ModuleMarriage moduleMarriage;
    private ModuleArena moduleArena;
    private ModuleSystem moduleSystem;
    private ModuleKingdom moduleKingdom;

    public void onEnable() {
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

        // Modules
        this.moduleSeating = new ModuleSeating(this);
        this.moduleMarriage = new ModuleMarriage(this);
        this.moduleArena = new ModuleArena(this);
        this.moduleSystem = new ModuleSystem(this);
        this.moduleKingdom = new ModuleKingdom(this);

        this.arenaSelectGUI = new ArenaSelectGUI(this);


        new PlayerInventoryListener(this);
        new SpellsListener(this);
        new ReviveListener(this);
    }

    public void onDisable() {
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

    public KingdomManager getKingdomManager() {
        return kingdomManager;
    }

    public ModuleMarriage getMarriageModule() {
        return moduleMarriage;
    }

    public ModuleKingdom getModuleKingdom() {
        return moduleKingdom;
    }

    public ModuleSeating getSeatingModule() {
        return moduleSeating;
    }

    public ModuleArena getArenaModule() {
        return moduleArena;
    }

    public void badUsage(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid usage."));
    }

    public void noPermission(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to execute this command."));
    }
}
