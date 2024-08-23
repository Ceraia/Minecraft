package com.axodouble.modules.system

import com.axodouble.Ceraia
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.util.StringUtil

class ModuleSystem(private val plugin: Ceraia) : CommandExecutor, TabCompleter, Listener {

    init {
        // Register the commands
        plugin.getCommand("jump")?.setExecutor(this)
        plugin.getCommand("mod")?.setExecutor(this)
        plugin.getCommand("version")?.setExecutor(this)
        plugin.getCommand("day")?.setExecutor(this)
        plugin.getCommand("night")?.setExecutor(this)
        plugin.getCommand("discord")?.setExecutor(this)
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        when (command.name.lowercase()) {
            "jump", "j" -> {
                if (sender is Player) {
                    jump(sender)
                }
                return true
            }
            "mod" -> {
                if (!sender.hasPermission("double.mod")) {
                    plugin.noPermission(sender as Player)
                    return true
                }

                when (args.size) {
                    0 -> {
                        modHelp(sender)
                        return true
                    }
                    1 -> {
                        if (args[0].equals("ban", ignoreCase = true)) {
                            modHelp(sender)
                            return true
                        }
                    }
                    2 -> {
                        if (args[0].equals("ban", ignoreCase = true)) {
                            if (args[1].equals("pvp", ignoreCase = true) || args[1].equals("arena", ignoreCase = true)) {
                                modHelp(sender)
                                return true
                            }
                        }
                    }
                    3 -> {
                        if (args[0].equals("ban", ignoreCase = true)) {
                            val target = Bukkit.getPlayer(args[2]) ?: run {
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found"))
                                return true
                            }

                            val doublePlayer = plugin.playerManager.getCeraiaPlayer(target.uniqueId)
                            when (args[1].lowercase()) {
                                "arena" -> {
                                    val arenaBanned = doublePlayer.toggleArenaBan()
                                    val message = if (arenaBanned) {
                                        "<red>Banned from creation of arenas by ${sender.name}"
                                    } else {
                                        "<green>Unbanned from creation of arenas by ${sender.name}"
                                    }
                                    sender.sendMessage(MiniMessage.miniMessage().deserialize(message))
                                }
                                "pvp" -> {
                                    val pvpBanned = doublePlayer.togglePvpBan()
                                    val message = if (pvpBanned) {
                                        "<red>Banned from PVPing by ${sender.name}"
                                    } else {
                                        "<green>Unbanned from PVPing by ${sender.name}"
                                    }
                                    sender.sendMessage(MiniMessage.miniMessage().deserialize(message))
                                }
                            }
                        }
                    }
                }
                return true
            }
            "version" -> {
                sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<green>Running <white>Ceraia <green>v${plugin.pluginMeta.version} <green>by <white>Axodouble"
                    )
                )
                return true
            }
            "day", "noon" -> {
                if (!sender.hasPermission("double.time.day") &&
                    !sender.hasPermission("double.time.*")
                ) {
                    plugin.noPermission(sender as Player)
                    return true
                }
                if (sender is Player) {
                    val time = if (command.name.equals("noon", ignoreCase = true)) 6000 else 0
                    sender.world.time = time.toLong()
                    sender.world.weatherDuration = 1
                    sender.world.clearWeatherDuration = 15 * 60 * 20
                }
                return true
            }
            "night" -> {
                if (!sender.hasPermission("double.time.night") &&
                    !sender.hasPermission("double.time.*")
                ) {
                    plugin.noPermission(sender as Player)
                    return true
                }
                if (sender is Player) {
                    sender.world.time = 13000
                }
                return true
            }
            "discord" -> {
                if (!sender.hasPermission("double.discord")) {
                    plugin.noPermission(sender as Player)
                    return true
                }
                sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<green>Click <white><click:copy_to_clipboard:" +
                                plugin.config.getString("DISCORD_INVITE", "EXAMPLE_INVITE") +
                                "><hover:show_text:" +
                                plugin.config.getString("DISCORD_INVITE", "EXAMPLE_INVITE") +
                                ">this</click></white> to copy the Discord link, or click here: <white>" +
                                plugin.config.getString("DISCORD_INVITE", "EXAMPLE_INVITE")
                    )
                )
                return true
            }
        }
        return false
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        val tabOptions = mutableListOf<String>()
        if (command.name.lowercase() == "mod") {
            when (args.size) {
                1 -> {
                    tabOptions.add("ban")
                    tabOptions.add("remove")
                }
                2 -> {
                    if (args[1].equals("ban", ignoreCase = true)) {
                        tabOptions.add("pvp")
                    }
                    tabOptions.add("arena")
                }
                3 -> {
                    when (args[2].lowercase()) {
                        "pvp" -> {
                            Bukkit.getOnlinePlayers().mapTo(tabOptions) { it.name }
                        }
                        "arena" -> {
                            plugin.arenaModule.arenaManager.arenas.mapTo(tabOptions) { it.name }
                        }
                    }
                }
            }
        }

        return StringUtil.copyPartialMatches(args.last(), tabOptions, ArrayList())
    }

    private fun jump(player: Player) {
        if (!player.hasPermission("double.jump")) {
            plugin.noPermission(player)
            return
        }
        // Get the block the player is looking at even if it is out of reach
        val block = player.getTargetBlockExact(1000)

        // Teleport the player on top of the block if it is not null
        if (block != null) {
            // Check if there is air above the block
            if (block.getRelative(0, 1, 0).isEmpty) {
                player.teleport(block.location.add(0.5, 1.0, 0.5))
            } else {
                // Get the highest block above the block
                var highestBlock = block.getRelative(0, 1, 0)
                while (highestBlock.getRelative(0, 1, 0).type != Material.AIR) {
                    highestBlock = highestBlock.getRelative(0, 1, 0)
                }
                player.teleport(highestBlock.location.add(0.5, 1.0, 0.5))
            }
        }
    }

    private fun modHelp(sender: CommandSender) {
        sender.sendMessage(
            MiniMessage.miniMessage().deserialize(
                """
                    <yellow><bold>Mod Help
                    <gray>/mod ban pvp <player>
                    <gray>/mod ban arena <player>
                    <gray>/mod remove pvp <player>
                    <gray>/mod remove arena <player>
                """.trimIndent()
            )
        )
    }
}