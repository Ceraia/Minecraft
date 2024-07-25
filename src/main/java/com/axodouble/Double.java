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
    private final Double plugin = this;
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

        plugin.saveResource("races.yml", false);

        /*---------------------------------*/
        /*       Registering Managers      */
        /*---------------------------------*/
        plugin.arenaManager = new ArenaManager(this);
        plugin.playerManager = new PlayerManager(this);
        plugin.inviteManager = new InviteManager();

        /*---------------------------------*/
        /*             Modules             */
        /*---------------------------------*/
        plugin.moduleSeating = new ModuleSeating(this);
        plugin.moduleMarriage = new ModuleMarriage(this);
        plugin.moduleArena = new ModuleArena(this);
        plugin.moduleSystem = new ModuleSystem(this);
        plugin.moduleRaces = new ModuleRaces(this);

        /*---------------------------------*/
        /*               GUIs              */
        /*---------------------------------*/
        plugin.arenaSelectGUI = new ArenaSelectGUI(this);

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
