package com.ceraia.modules.races

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.PaginatedGui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.util.StringUtil
import java.io.File
import java.util.*

class ModuleRaces(private val plugin: com.ceraia.Ceraia) : CommandExecutor, TabCompleter, Listener {
    private var races: MutableList<Race> = ArrayList()
    private var raceFactions: MutableList<RaceFaction> = ArrayList()


    init {
        plugin.getCommand("race")?.setExecutor(this)
        plugin.getCommand("race")?.tabCompleter = this
        Bukkit.getPluginManager().registerEvents(this, plugin)

        loadRaces()
        loadRaceFactions()
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            return true
        }

        if (!racesEnabled()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Races are not enabled in the config!"))
            return true
        }

        if (args.isEmpty()) {
            openFactionGui(sender)
            return true
        }

        when (args[0]) {
            "reload" -> {
                if (!sender.hasPermission("ceraia.races.reload")) {
                    plugin.noPermission(sender)
                    return true
                }
                reloadRaces()
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloading races..."))
                return true
            }

            "become" -> {
                if (!sender.hasPermission("ceraia.races.become")) {
                    plugin.noPermission(sender)
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage(
                        MiniMessage.miniMessage().deserialize("<red>Please specify what race you want to become.")
                    )
                    return true
                }
                if (args.size > 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Too many arguments."))
                    return true
                }
                // Find a race with the name
                var selectedRace: Race? = null
                for (race in races) {
                    if (race.name == args[1]) {
                        selectedRace = race
                    }
                }

                if (selectedRace == null) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Race not found!"))
                    return true
                } else {
                    if (!sender.hasPermission("ceraia.races.become." + args[1]) &&
                        !sender.hasPermission("ceraia.races.become.*")
                    ) {
                        sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>You do not have permission to become this race")
                        )
                        return true
                    }
                    selectedRace.apply(sender)
                    sender.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            "<green>Succesfully changed your race to a <white>" + selectedRace.name
                        )
                    )
                    return true
                }
            }

            "gui" -> openFactionGui(sender)
            "restore" -> {
                if (!sender.hasPermission("ceraia.races.restore")) {
                    plugin.noPermission(sender)
                    return true
                }
                // Restore all races
                sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<green>Restoring all default races & factions..."
                    )
                )
                addDefaultRacesFactions()
                return true
            }

            "faction" -> {
                openFactionGui(sender)
                return true
            }
        }

        openFactionGui(sender)
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<String>): List<String> {
        // Return a string list of all races
        val tabOptions: MutableList<String> = ArrayList()
        if (args.size == 1) {
            tabOptions.add("reload")
            tabOptions.add("become")
            tabOptions.add("gui")
        } else if (args.size == 2) {
            if (args[0].equals("become", ignoreCase = true)) {
                for (race in races) {
                    tabOptions.add(race.name)
                }
            }
        } else if (args.size == 3) {
            if (args[0].equals("become", ignoreCase = true)) {
                tabOptions.add("reload")
                tabOptions.add("become")
            }
        }
        val returnedOptions: MutableList<String> = ArrayList()
        StringUtil.copyPartialMatches(args[args.size - 1], tabOptions, returnedOptions)

        return returnedOptions
    }

    private fun racesEnabled(): Boolean {
        return plugin.config.getBoolean("races-enabled", true)
    }

    private fun reloadRaces() {
        loadRaces(true)
    }

    private fun addDefaultRacesFactions() {
        val dataFile = File(plugin.dataFolder, "races.yml")
        if (!dataFile.exists()) {
            plugin.saveResource("races.yml", false)
        }

        val dataConfig: FileConfiguration = YamlConfiguration.loadConfiguration(dataFile)
        val existingRaces = dataConfig.getList("races")?.map { (it as Map<*, *>)["name"] }?.toSet() ?: emptySet()
        val existingFactions = dataConfig.getList("factions")?.map { (it as Map<*, *>)["name"] }?.toSet() ?: emptySet()

        val resourceConfig: FileConfiguration = plugin.getResource("races.yml")?.reader()?.let {
            YamlConfiguration.loadConfiguration(it)
        } ?: YamlConfiguration()

        val defaultRaces = resourceConfig.getList("races")?.map { it as Map<*, *> } ?: emptyList()
        val defaultFactions = resourceConfig.getList("factions")?.map { it as Map<*, *> } ?: emptyList()

        val newRaces = defaultRaces.filter { it["name"] !in existingRaces }
        val newFactions = defaultFactions.filter { it["name"] !in existingFactions }

        if (newRaces.isNotEmpty()) {
            val racesSection = dataConfig.getList("races")?.toMutableList() ?: mutableListOf()
            racesSection.addAll(newRaces)
            dataConfig.set("races", racesSection)
        }

        if (newFactions.isNotEmpty()) {
            val factionsSection = dataConfig.getList("factions")?.toMutableList() ?: mutableListOf()
            factionsSection.addAll(newFactions)
            dataConfig.set("factions", factionsSection)
        }

        dataConfig.save(dataFile)
    }

    @JvmOverloads
    fun loadRaces(reload: Boolean = false) {
        races.clear()
        if (reload) plugin.logger.info("Reloading races...")
        else plugin.logger.info("Loading races...")

        // Load all races from the races.yml file
        val file = File(plugin.dataFolder, "races.yml")
        val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)
        val racesSection = config.getConfigurationSection("races")
        if (racesSection == null) {
            plugin.logger.warning("No races found in races.yml! Adding default races.")
            addDefaultRacesFactions()
            return
        }

        for (raceEntry in racesSection.getKeys(false)) {
            plugin.logger.info("races.$raceEntry")
            val path = "races.$raceEntry"
            val race = Race(
                config.getString("$path.name", "<gray>No name...")!!,
                config.getDouble("$path.scale", 1.0),
                config.getDouble("$path.speed", 0.1),
                config.getInt("$path.health", 20),
                config.getDouble("$path.jumpheight", 0.42),
                config.getDouble("$path.damage", 1.0),
                config.getDouble("$path.reach", 5.0),
                config.getDouble("$path.attackspeed", 4.0),
                config.getString("$path.lore", "<gray>No known lore...")!!,
                config.getDouble("$path.falldamagemultiplier", 1.0),
                config.getDouble("$path.miningefficiency", 0.0),
                config.getDouble("$path.armor", 0.0),
                ItemStack(Material.valueOf(config.getString("$path.item", "BREAD")!!)),
            )
            plugin.logger.info(config.getString("$path.name", "<gray>No name..."))
            plugin.logger.info("$path.name" )
            races.add(race)
        }
    }

    @JvmOverloads
    fun loadRaceFactions(reload: Boolean = false) {
        raceFactions.clear()
        if (reload) plugin.logger.info("Reloading race factions...")
        else plugin.logger.info("Loading race factions...")

        // Load all factions from the races.yml file
        val file = File(plugin.dataFolder, "races.yml")
        val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)
        val factionsSection = config.getConfigurationSection("factions")
        if (factionsSection == null) {
            plugin.logger.warning("No factions found in races.yml! Adding default factions.")
            return
        }

        for (factionName in factionsSection.getKeys(false)) {
            val path = "factions.$factionName"
            val faction = RaceFaction(
                config.getString("$path.name", "<gray>No known name...")!!,
                config.getString("$path.lore", "<gray>No known lore...")!!,
                ItemStack(Material.valueOf(config.getString("$path.item", "BREAD")!!)),
                config.getInt("$path.health", 0),
                config.getDouble("$path.damage", 0.0),
                config.getDouble("$path.miningefficiency", 0.0),
                config.getDouble("$path.armor", 0.0),
                config.getStringList("$path.raceInhabitants")
            )
            raceFactions.add(faction)
        }
    }
    
    private fun openRaceGui(player: Player, faction: RaceFaction) {
        val gui : PaginatedGui = Gui.paginated()
            .title(Component.text("Select a race to become"))
            .disableAllInteractions()
            .rows(4)
            .create()

        gui.setItem(4, 3, ItemBuilder.from(Material.PAPER).name(Component.text("Previous")).asGuiItem { gui.previous() })
        gui.setItem(4, 7, ItemBuilder.from(Material.PAPER).name(Component.text("Next")).asGuiItem { gui.next() })

        val selectable: MutableList<Race> = ArrayList()
        for (race in races) {
            if (player.isOp ||
                (player.hasPermission("ceraia.races.become." + race.name) || player.hasPermission("ceraia.races.become.*")) &&
                (faction.getRaceInhabitants().contains(race.name) || faction.getRaceInhabitants().contains("*"))
            ) {
                selectable.add(race)
            }
        }

        selectable.forEach { race ->
            val lore: MutableList<Component> = ArrayList()

            lore.add(MiniMessage.miniMessage().deserialize("<gray>Scale : <green>" + race.scale))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Speed : <green>" + race.speed))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Health : <green>" + race.health))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Jump Height : <green>" + race.jumpHeight))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Damage : <green>" + race.damage))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Reach : <green>" + race.reach))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Attack Speed : <green>" + race.attackSpeed))
            lore.add(
                MiniMessage.miniMessage()
                    .deserialize("<gray>Fall Damage Multiplier : <green>" + race.fallDamageMultiplier)
            )
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Mining Efficiency : <green>" + race.miningEfficiency))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Armor : <green>" + race.armor))

            gui.addItem(
                ItemBuilder.from(race.item)
                    .lore(lore as List<Component?>)
                    .name(MiniMessage.miniMessage().deserialize(race.name))
                    .asGuiItem {
                        plugin.playerManager.getCeraiaPlayer(player).setRace(race.name)
                        plugin.playerManager.getCeraiaPlayer(player).setFaction(faction.name)
                        faction.apply(player, race)

                        player.sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                "<green>Succesfully changed your faction to <white>" +
                                        faction.name +
                                        "<green>, and your race to " +
                                        race.name + "<green>!"
                            )
                        )
                        gui.close(player)
                    }
            )

        }

        gui.open(player)
}

    private fun openFactionGui(player: Player){
        val gui : PaginatedGui = Gui.paginated()
            .title(Component.text("Select a faction to join"))
            .disableAllInteractions()
            .rows(4)
            .create()

        gui.setItem(4, 3, ItemBuilder.from(Material.PAPER).name(Component.text("Previous")).asGuiItem { gui.previous() })
        gui.setItem(4, 7, ItemBuilder.from(Material.PAPER).name(Component.text("Next")).asGuiItem { gui.next() })

        val selectable: List<RaceFaction> = ArrayList(raceFactions)

        selectable.forEach { raceFaction ->
            val lore: MutableList<Component> = ArrayList()
            lore.add(MiniMessage.miniMessage().deserialize(raceFaction.lore))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Health Bonus : <green>" + raceFaction.health))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Damage Bonus : <green>" + raceFaction.damage))
            lore.add(
                MiniMessage.miniMessage()
                    .deserialize("<gray>Mining Efficiency Bonus : <green>" + raceFaction.miningEfficiency)
            )
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Armor Bonus : <green>" + raceFaction.armor))

            gui.addItem(
                ItemBuilder.from(raceFaction.item)
                    .lore(lore as List<Component?>)
                    .name(MiniMessage.miniMessage().deserialize(raceFaction.name))
                    .asGuiItem {
                        openRaceGui(player, raceFaction)
                    })
        }

        gui.open(player)
    }

    class Race(
        val name: String,
        val scale: Double,
        val speed: Double,
        val health: Int,
        val jumpHeight: Double,
        val damage: Double,
        val reach: Double,
        val attackSpeed: Double,
        val lore: String,
        val fallDamageMultiplier: Double,
        val miningEfficiency: Double,
        val armor: Double,
        val item: ItemStack
    ) {
        fun apply(player: Player) {
            player.getAttribute(Attribute.SCALE)?.baseValue =
                scale
            player.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue =
                speed
            player.getAttribute(Attribute.MAX_HEALTH)?.baseValue =
                health.toDouble()
            player.getAttribute(Attribute.JUMP_STRENGTH)?.baseValue =
                jumpHeight
            player.getAttribute(Attribute.SAFE_FALL_DISTANCE)?.baseValue =
                jumpHeight * 7.145
            player.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue =
                damage
            player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.baseValue =
                reach
            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.baseValue =
                reach
            player.getAttribute(Attribute.ATTACK_SPEED)?.baseValue =
                attackSpeed
            player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.baseValue =
                fallDamageMultiplier
            player.getAttribute(Attribute.MINING_EFFICIENCY)?.baseValue =
                miningEfficiency
            player.getAttribute(Attribute.ARMOR)?.baseValue =
                armor
        }
    }

    class RaceFaction(
        val name: String,
        val lore: String,
        val item: ItemStack,
        val health: Int,
        val damage: Double,
        val miningEfficiency: Double,
        val armor: Double,
        raceInhabitants: List<String>
    ) {
        private val raceInhabitants: MutableList<String> = ArrayList()

        init {
            this.raceInhabitants.addAll(raceInhabitants)
        }

        fun getRaceInhabitants(): List<String> {
            return raceInhabitants
        }

        fun apply(player: Player, race: Race) {
            player.getAttribute(Attribute.SCALE)?.baseValue =
                race.scale
            player.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue =
                race.speed
            player.getAttribute(Attribute.MAX_HEALTH)?.baseValue =
                (health + race.health).toDouble()
            player.getAttribute(Attribute.JUMP_STRENGTH)?.baseValue =
                race.jumpHeight
            player.getAttribute(Attribute.SAFE_FALL_DISTANCE)?.baseValue =
                race.jumpHeight * 7.145
            player.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue =
                damage + race.damage
            player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.baseValue =
                race.reach
            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.baseValue =
                race.reach
            player.getAttribute(Attribute.ATTACK_SPEED)?.baseValue =
                race.attackSpeed
            player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.baseValue =
                race.fallDamageMultiplier
            player.getAttribute(Attribute.MINING_EFFICIENCY)?.baseValue =
                miningEfficiency + race.miningEfficiency
            player.getAttribute(Attribute.ARMOR)?.baseValue =
                armor + race.armor
        }
    }
}
