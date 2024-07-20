package com.axodouble;

import com.axodouble.listeners.PlayerInventoryListener;
import com.axodouble.listeners.SpellsListener;
import com.axodouble.managers.ArenaManager;
import com.axodouble.managers.InviteManager;
import com.axodouble.managers.PlayerManager;
import com.axodouble.modules.*;
import com.axodouble.types.ArenaSelectGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Double extends JavaPlugin {

    private ArenaManager arenaManager;
    private InviteManager inviteManager;
    private ArenaSelectGUI arenaSelectGUI;
    private PlayerManager playerManager;
    private ModuleSeating moduleSeating;
    private ModuleMarriage moduleMarriage;
    private ModuleArena moduleArena;
    private ModuleSystem moduleSystem;
    private ModuleRaces moduleRaces;

    public void onEnable() {
        saveDefaultConfig();
        new File(getDataFolder(), "data/arenas").mkdirs();
        new File(getDataFolder(), "data/items").mkdirs();
        new File(getDataFolder(), "data/users").mkdirs();

        /*---------------------------------*/
        /*       Registering Managers      */
        /*---------------------------------*/
        this.arenaManager = new ArenaManager(this);
        this.playerManager = new PlayerManager(this);
        this.inviteManager = new InviteManager();

        /*---------------------------------*/
        /*             Modules             */
        /*---------------------------------*/
        this.moduleSeating = new ModuleSeating(this);
        this.moduleMarriage = new ModuleMarriage(this);
        this.moduleArena = new ModuleArena(this);
        this.moduleSystem = new ModuleSystem(this);
        this.moduleRaces = new ModuleRaces(this);

        /*---------------------------------*/
        /*               GUIs              */
        /*---------------------------------*/
        this.arenaSelectGUI = new ArenaSelectGUI(this);

        /*---------------------------------*/
        /*            Listeners            */
        /*---------------------------------*/
        new PlayerInventoryListener(this);
        new SpellsListener(this);
    }

    public void onDisable() {
        playerManager.savePlayers();
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

    public ModuleMarriage getMarriageModule() {
        return moduleMarriage;
    }

    public ModuleSeating getSeatingModule() {
        return moduleSeating;
    }

    public ModuleArena getArenaModule() {
        return moduleArena;
    }

    public ModuleSystem getSystemModule() {
        return moduleSystem;
    }

    public ModuleRaces getRacesModule() {
        return moduleRaces;
    }

    public void badUsage(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid usage."));
    }

    public void noPermission(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to execute this command."));
    }
}
