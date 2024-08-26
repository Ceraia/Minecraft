package com.ceraia.util

import com.ceraia.Ceraia
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigHelper(private val plugin: Ceraia) {

    // Get the FileConfiguration from a file
    fun get(file: File): FileConfiguration {
        return YamlConfiguration.loadConfiguration(file)
    }

    // Get the FileConfiguration from a file path
    fun get(path: String): FileConfiguration {
        return get(File(plugin.dataFolder, path))
    }
}
