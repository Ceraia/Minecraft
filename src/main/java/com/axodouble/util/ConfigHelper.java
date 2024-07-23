package com.axodouble.util;

import com.axodouble.Double;

import java.io.File;

public abstract class ConfigHelper {
    private Double plugin;

    public ConfigHelper(Double plugin) {
        this.plugin = plugin;
    }

    public void saveDefaultConfigs(){
        plugin.getResource("config.yml");
        plugin.getResource("races.yml");

        // Check if any of the files exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        plugin.saveResource("config.yml", false);

        plugin.saveResource("races.yml", false);
    }

}
