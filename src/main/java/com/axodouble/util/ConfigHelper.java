package com.axodouble.util;

import com.axodouble.Double;
import com.axodouble.modules.ModuleRaces;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;

public class ConfigHelper {
    private final Double plugin;
    public ConfigHelper(Double plugin) {
        this.plugin = plugin;
    }

    // Get the FileConfiguration from a file
    public FileConfiguration get(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }
    // Get the FileConfiguration from a file path
    public FileConfiguration get(String path) {
        return get(new File(plugin.getDataFolder(), path));
    }

}
