package com.axodouble.modules.arena;

import com.axodouble.Double;

public class ArenaModule {
    private Double plugin;
    public ArenaManager arenaManager;
    public ArenaModule(Double plugin) {
        this.plugin = plugin;
        new ArenaCommandHandler(plugin);
        new ArenaEvents(plugin);
        new ArenaActions(plugin);
        arenaManager = new ArenaManager(plugin);
    }
}
