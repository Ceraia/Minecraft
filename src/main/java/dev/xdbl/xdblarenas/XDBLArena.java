package dev.xdbl.xdblarenas;

import dev.xdbl.xdblarenas.arenas.ArenaManager;
import dev.xdbl.xdblarenas.arenas.listeners.ArenaBlockListener;
import dev.xdbl.xdblarenas.commands.ArenaCommand;
import dev.xdbl.xdblarenas.arenas.listeners.ArenaExplodeListener;
import dev.xdbl.xdblarenas.arenas.listeners.ArenaFightListener;
import dev.xdbl.xdblarenas.arenas.listeners.ArenaItemListener;
import dev.xdbl.xdblarenas.commands.PVPCommand;
import dev.xdbl.xdblarenas.groups.GroupManager;
import dev.xdbl.xdblarenas.gui.ArenaSelectGUI;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class XDBLArena extends JavaPlugin {

    private ArenaManager arenaManager;
    private InviteManager inviteManager;
    private ArenaSelectGUI arenaSelectGUI;
    private GroupManager groupManager;

    public void onEnable() {
        saveDefaultConfig();
        new File(getDataFolder(), "data/arenas").mkdirs();
        this.arenaManager = new ArenaManager(this);
        this.inviteManager = new InviteManager();
        this.arenaSelectGUI = new ArenaSelectGUI(this);
        this.groupManager = new GroupManager(this);
        new ArenaFightListener(this);
        new ArenaItemListener(this);
        new ArenaBlockListener(this);
        new ArenaExplodeListener(this);

        getCommand("pvp").setExecutor(new PVPCommand(this));
        getCommand("arena").setExecutor(new ArenaCommand(this));
        getCommand("gvg").setExecutor(groupManager);
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public ArenaSelectGUI getArenaSelectGUI() {
        return arenaSelectGUI;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }
}
