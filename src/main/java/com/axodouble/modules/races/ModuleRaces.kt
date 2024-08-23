package com.axodouble.modules.races

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
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.util.StringUtil
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max

class ModuleRaces(private val plugin: com.axodouble.Ceraia) : CommandExecutor, TabCompleter, Listener {
    private var races: MutableList<Race> = ArrayList()
    private var raceFactions: MutableList<RaceFaction> = ArrayList()
    private var playerRaceSelection: MutableMap<Player, Map<ItemStack?, Race>> = HashMap()
    private var playerRaceFactionSelection: MutableMap<Player, Map<ItemStack?, RaceFaction>> = HashMap()
    private var playerRaceFactionSelected: MutableMap<Player, RaceFaction?> = HashMap()


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

        if (args.size == 0) {
            openFactionGUI(sender)
            return true
        }

        when (args[0]) {
            "reload" -> {
                if (!sender.hasPermission("double.races.reload")) {
                    plugin.noPermission(sender)
                    return true
                }
                reloadRaces()
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloading races..."))
                return true
            }

            "become" -> {
                if (!sender.hasPermission("double.races.become")) {
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
                    if (!sender.hasPermission("double.races.become." + args[1]) &&
                        !sender.hasPermission("double.races.become.*")
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

            "gui" -> openFactionGUI(sender)
            "restore" -> {
                if (!sender.hasPermission("double.races.restore")) {
                    plugin.noPermission(sender)
                    return true
                }
                // Restore all races
                sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<green>Restoring all default races & factions..."
                    )
                )
                addDefaultRaces()
                addDefaultFactions()
                return true
            }

            "faction" -> {
                openFactionGUI(sender)
                return true
            }
        }

        openFactionGUI(sender)
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

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        if (e.currentItem == null || e.currentItem!!.type == Material.AIR) {
            return
        } // If the item doesn't exist or is air, return


        if ((e.inventory).type == InventoryType.PLAYER) {
            return
        } // If the inventory is the player's inventory, return


        val player = e.whoClicked as Player // Get the player who clicked

        if (e.view.title().toString() == MiniMessage.miniMessage().deserialize("<green>Select a race to become")
                .toString()
        ) {
            if (playerRaceSelection.containsKey(player)) {
                // Get the relevant slot and race that the player clicked on
                if (playerRaceSelection[player]!!.containsKey(e.currentItem)) {
                    e.isCancelled = true
                    playerRaceFactionSelected[player]!!.apply(
                        player,
                        playerRaceSelection[player]!![e.currentItem]!!
                    )
                    e.clickedInventory!!.close()
                    player.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            "<green>Succesfully changed your faction to <white>" +
                                    playerRaceFactionSelected[player]!!.name +
                                    "<green>, and your race to " +
                                    playerRaceSelection[player]!![e.currentItem]!!.name + "<green>!"
                        )
                    )

                    plugin.playerManager.getCeraiaPlayer(player).setRace(
                        playerRaceSelection[player]!![e.currentItem]!!.name
                    )
                    plugin.playerManager.getCeraiaPlayer(player).setFaction(playerRaceFactionSelected[player]!!.name)
                }
            }
        } else if (e.view.title().toString() == MiniMessage.miniMessage().deserialize("<green>Select a faction to join")
                .toString()
        ) {
            if (playerRaceFactionSelection.containsKey(player)) {
                // Get the relevant slot and faction that the player clicked on
                if (playerRaceFactionSelection[player]!!.containsKey(e.currentItem)) {
                    e.isCancelled = true
                    playerRaceFactionSelected[player] = playerRaceFactionSelection[player]!![e.currentItem]
                    openRaceGUI(player)
                }
            }
        }
    }

    fun racesEnabled(): Boolean {
        return plugin.config.getBoolean("races-enabled", true)
    }

    fun reloadRaces() {
        loadRaces(true)
    }

    fun addDefaultRaces() {
        run {
            races.add(
                Race(
                    "Halfling",  // Name
                    0.54,  // Scale
                    0.12,  // Speed
                    14,  // Health
                    0.42,  // Jumpheight
                    0.95,  // Damage
                    2.5,  // Reach
                    4.5,  // Attack Speed
                    "<gray>Nimble and stealthy,<newline><green>Halflings<gray> excel in evading danger.",  // Lore
                    1.0,  // Fall Damage Multiplier
                    5.2,  // Mining Efficiency
                    0.0,  // Armor
                    ItemStack(Material.POTATO) // Item
                )
            )
            races.add(
                Race(
                    "Gnome",  // Name
                    0.7,  // Scale
                    0.11,  // Speed
                    16,  // Health
                    0.42,  // Jumpheight
                    0.9,  // Damage
                    3.0,  // Reach
                    5.0,  // Attack Speed
                    "<gray>Clever and elusive,<newline><green>Gnomes<gray> use their fast attack to outwit foes.",  // Lore
                    1.0,  // Fall Damage Multiplier
                    6.65,  // Mining Efficiency
                    0.0,  // Armor
                    ItemStack(Material.RED_MUSHROOM) // Item
                )
            )

            races.add(
                Race(
                    "Dwarf",  // Name
                    0.9,  // Scale
                    0.1,  // Speed
                    24,  // Health
                    0.42,  // Jumpheight
                    1.0,  // Damage
                    4.5,  // Reach
                    4.0,  // Attack Speed
                    "<gray>Sturdy and relentless,<newline><green>Dwarves<gray> are master miners and warriors.",  // Lore
                    1.0,  // Fall Damage Multiplier
                    9.95,  // Mining Efficiency
                    2.0,  // Armor
                    ItemStack(Material.IRON_ORE) // Item
                )
            )

            races.add(
                Race(
                    "Short Human",  // Name
                    0.95,  // Scale
                    0.1,  // Speed
                    20,  // Health
                    0.42,  // Jumpheight
                    1.0,  // Damage
                    5.0,  // Reach
                    4.0,  // Attack Speed
                    "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.",  // Lore
                    1.0,  // Fall Damage Multiplier
                    0.0,  // Mining Efficiency
                    0.0,  // Armor
                    ItemStack(Material.BREAD) // Item
                )
            )

            races.add(
                Race(
                    "Human",  // Name
                    1.0,  // Scale
                    0.1,  // Speed
                    20,  // Health
                    0.42,  // Jumpheight
                    1.0,  // Damage
                    5.0,  // Reach
                    4.0,  // Attack Speed
                    "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.",  // Lore
                    1.0,  // Fall Damage Multiplier
                    0.0,  // Mining Efficiency
                    0.0,  // Armor
                    ItemStack(Material.BREAD) // Item
                )
            )

            races.add(
                Race(
                    "Tall Human",  // Name
                    1.05,  // Scale
                    0.1,  // Speed
                    20,  // Health
                    0.42,  // Jumpheight
                    1.0,  // Damage
                    5.0,  // Reach
                    4.0,  // Attack Speed
                    "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.",  // Lore
                    1.0,  // Fall Damage Multiplier
                    0.0,  // Mining Efficiency
                    0.0,  // Armor
                    ItemStack(Material.BREAD) // Item
                )
            )

            races.add(
                Race(
                    "Satyr",  // Name
                    1.05,  // Scale
                    0.13,  // Speed
                    18,  // Health
                    0.52,  // Jumpheight
                    1.0,  // Damage
                    5.0,  // Reach
                    3.9,  // Attack Speed
                    "<gray>Fast and intelligent<newline><green>Satyrs<gray> are expert explorers and have seen far across the world.",  // Lore
                    1.0,  // Fall Damage Multiplier
                    0.0,  // Mining Efficiency
                    0.0,  // Armor
                    ItemStack(Material.RABBIT_FOOT) // Item
                )
            )

            races.add(
                Race(
                    "Githyanki",  // Name
                    1.07,  // Scale
                    0.975,  // Speed
                    22,  // Health
                    0.42,  // Jumpheight
                    1.0,  // Damage
                    5.0,  // Reach
                    4.2,  // Attack Speed
                    "<gray>Fierce and fast,<newline><green>Githyanki<gray> are veteran warriors.",  // Lore
                    1.0,  // Fall Damage Multiplier
                    0.0,  // Mining Efficiency
                    0.0,  // Armor
                    ItemStack(Material.NETHER_STAR) // Item
                )
            )

            races.add(
                Race(
                    "Half-Elf",  // Name
                    1.11,  // Scale
                    0.0975,  // Speed
                    24,  // Health
                    0.52,  // Jumpheight
                    1.0,  // Damage
                    5.0,  // Reach
                    3.95,  // Attack Speed
                    "<gray>Graceful but adaptable,<newline><green>Half-elves<gray> are the result of a Elven - Human relationship.",  // Lore
                    0.925,  // Fall Damage Multiplier
                    1.0,  // Mining Efficiency
                    1.0,  // Armor
                    ItemStack(Material.APPLE) // Item
                )
            )

            races.add(
                Race(
                    "Elf",  // Name
                    1.11,  // Scale
                    0.095,  // Speed
                    26,  // Health
                    0.63,  // Jumpheight
                    1.0,  // Damage
                    5.0,  // Reach
                    3.9,  // Attack Speed
                    "<gray>Graceful and wise,<newline><green>Elves<gray> are good fighters and excel in archery.",  // Lore
                    0.75,  // Fall Damage Multiplier
                    1.0,  // Mining Efficiency
                    2.0,  // Armor
                    ItemStack(Material.BOW) // Item
                )
            )

            races.add(
                Race(
                    "Half-Orc",  // Name
                    1.15,  // Scale
                    0.0925,  // Speed
                    26,  // Health
                    0.63,  // Jumpheight
                    1.0,  // Damage
                    5.5,  // Reach
                    3.8,  // Attack Speed
                    "<gray>Strong and fierce,<newline><green>Half-orcs<gray> are the result of a Orc - Human relationship.",  // Lore
                    0.75,  // Fall Damage Multiplier
                    2.0,  // Mining Efficiency
                    3.0,  // Armor
                    ItemStack(Material.PORKCHOP) // Item
                )
            )

            races.add(
                Race(
                    "Bugbear",  // Name
                    1.22,  // Scale
                    0.09,  // Speed
                    28,  // Health
                    0.63,  // Jumpheight
                    1.25,  // Damage
                    5.8,  // Reach
                    3.0,  // Attack Speed
                    "<gray>Fierce and powerful,<newline><green>Bugbears<gray> dominate in brute strength.",  // Lore
                    0.75,  // Fall Damage Multiplier
                    2.0,  // Mining Efficiency
                    4.0,  // Armor
                    ItemStack(Material.BEEF) // Item
                )
            )
            races.add(
                Race(
                    "Goliath",  // Name
                    1.33,  // Scale
                    0.08,  // Speed
                    30,  // Health
                    0.63,  // Jumpheight
                    1.25,  // Damage
                    6.1,  // Reach
                    3.0,  // Attack Speed
                    "<gray>Large and strong,<newline><green>Goliaths<gray> are often used in heavy labour.",  // Lore
                    0.75,  // Fall Damage Multiplier
                    0.0,  // Mining Efficiency
                    5.0,  // Armor
                    ItemStack(Material.OAK_SAPLING) // Item
                )
            )
        }
        saveAllRaces()
        loadRaces()
    }

    fun addDefaultFactions() {
        run {
            raceFactions.add(
                RaceFaction(
                    "<red>Gnomish Dynasty",
                    "<red>The Gnomish Dynasty<gray>, formed by the Gnomish, a monarchy.",
                    ItemStack(Material.RED_MUSHROOM),
                    0,
                    0.0,
                    0.0,
                    0.0,
                    mutableListOf("Gnome", "Satyr", "Githyanki", "Bugbear")
                )
            )
            raceFactions.add(
                RaceFaction(
                    "<blue>Republic of Man",
                    "<blue>The Republic of Man<gray>, formed by the Humans, a democratic republic.",
                    ItemStack(Material.BREAD),
                    0,
                    0.0,
                    0.0,
                    0.0,
                    mutableListOf("Dwarf", "Short Human", "Human", "Tall Human", "Half-Elf", "Half-Orc")
                )
            )
            raceFactions.add(
                RaceFaction(
                    "<green>Elven Demagogue",
                    "<green>The Elven Demagogue<gray>, formed by the Elves, a theocracy.",
                    ItemStack(Material.PORKCHOP),
                    0,
                    0.0,
                    0.0,
                    0.0,
                    mutableListOf("Halfling", "Half-Elf", "Elf", "Goliath")
                )
            )
            raceFactions.add(
                RaceFaction(
                    "<yellow>Unaligned",
                    "<yellow>The Unaligned<gray>, formed by those with no king, a loose alliance.",
                    ItemStack(Material.IRON_ORE),
                    0,
                    0.0,
                    0.0,
                    0.0,
                    listOf(
                        "*"
                    )
                )
            )
        }
        saveAllFactions()
        loadRaceFactions()
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
            addDefaultRaces()
            return
        }

        for (raceName in racesSection.getKeys(false)) {
            val path = "races.$raceName"
            val race = Race(
                raceName,
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
                config.getItemStack("$path.item", ItemStack(Material.BREAD))!!
            )
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
            addDefaultFactions()
            return
        }

        for (factionName in factionsSection.getKeys(false)) {
            val path = "factions.$factionName"
            val faction = RaceFaction(
                factionName,
                config.getString("$path.lore", "<gray>No known lore...")!!,
                config.getItemStack("$path.item", ItemStack(Material.BREAD))!!,
                config.getInt("$path.health", 0),
                config.getDouble("$path.damage", 0.0),
                config.getDouble("$path.miningefficiency", 0.0),
                config.getDouble("$path.armor", 0.0),
                config.getStringList("$path.raceInhabitants")
            )
            raceFactions.add(faction)
        }
    }

    private fun openRaceGUI(player: Player) {
        val selectable: MutableList<Race> = ArrayList()
        for (race in races) {
            if ((player.hasPermission("double.races.become." + race.name) ||
                        player.hasPermission("double.races.become.*")) &&
                (playerRaceFactionSelected[player]!!.getRaceInhabitants().contains(race.name)
                        || playerRaceFactionSelected[player]!!.getRaceInhabitants().contains("*"))
            ) selectable.add(race)
        }

        // Open a GUI with all selectable races
        val size = max(9.0, ((selectable.size + 8) / 9 * 9).toDouble()).toInt()
        val inv =
            Bukkit.createInventory(null, size, MiniMessage.miniMessage().deserialize("<green>Select a race to become"))

        val raceSelectSlots: MutableMap<ItemStack?, Race> = HashMap()

        val i = AtomicInteger() // Slot
        selectable.forEach(Consumer<Race> { race: Race ->
            val itemStack = race.item // Create the itemstack
            val meta = itemStack.itemMeta
            meta.displayName(
                MiniMessage.miniMessage().deserialize("<green>" + race.name + "</green>")
            )

            val lore: MutableList<Component> = ArrayList()

            Arrays.stream(race.lore.split("<newline>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).toList()
                .forEach(
                    Consumer { s: String -> lore.add(MiniMessage.miniMessage().deserialize(s)) })
            //lore.add(MiniMessage.miniMessage().deserialize(race.getLore()));
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

            meta.lore(lore)

            itemStack.setItemMeta(meta)
            itemStack.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)

            inv.setItem(i.get(), itemStack)
            raceSelectSlots[itemStack] = race
            i.getAndIncrement()
        })
        playerRaceSelection[player] = raceSelectSlots
        player.openInventory(inv)
    }

    private fun openFactionGUI(player: Player) {
        val selectable: List<RaceFaction> = ArrayList(raceFactions)
        // Open a GUI with all selectable factions
        val size = max(9.0, ((selectable.size + 8) / 9 * 9).toDouble()).toInt()
        val inv =
            Bukkit.createInventory(null, size, MiniMessage.miniMessage().deserialize("<green>Select a faction to join"))

        val factionSelectSlots: MutableMap<ItemStack?, RaceFaction> = HashMap()

        val i = AtomicInteger() // Slot
        selectable.forEach(Consumer<RaceFaction> { faction: RaceFaction ->
            val itemStack = faction.item // Create the itemstack
            val meta = itemStack.itemMeta
            meta.displayName(
                MiniMessage.miniMessage().deserialize(faction.name)
            )

            val lore: MutableList<Component> = ArrayList()
            lore.add(MiniMessage.miniMessage().deserialize(faction.lore))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Health Bonus : <green>" + faction.health))
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Damage Bonus : <green>" + faction.damage))
            lore.add(
                MiniMessage.miniMessage()
                    .deserialize("<gray>Mining Efficiency Bonus : <green>" + faction.miningEfficiency)
            )
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Armor Bonus : <green>" + faction.armor))

            meta.lore(lore)

            itemStack.setItemMeta(meta)
            itemStack.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)

            inv.setItem(i.get(), itemStack)
            factionSelectSlots[itemStack] = faction
            i.getAndIncrement()
        })
        playerRaceFactionSelection[player] = factionSelectSlots
        player.openInventory(inv)
    }

    fun saveAllRaces() {
        // Save the races to the races.yml file
        val file = File(plugin.dataFolder, "races.yml")
        val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)

        for (race in races) {
            val path = "races." + race.name
            config["$path.lore"] = race.lore
            config["$path.scale"] = race.scale
            config["$path.speed"] = race.speed
            config["$path.health"] = race.health
            config["$path.jumpheight"] = race.jumpHeight
            config["$path.damage"] = race.damage
            config["$path.reach"] = race.reach
            config["$path.attackspeed"] = race.attackSpeed
            config["$path.item"] = race.item
            config["$path.falldamagemultiplier"] = race.fallDamageMultiplier
            config["$path.miningefficiency"] = race.miningEfficiency
            config["$path.armor"] = race.armor
        }

        try {
            config.save(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveAllFactions() {
        // Save the factions to the races.yml file
        val file = File(plugin.dataFolder, "races.yml")
        val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)

        for (faction in raceFactions) {
            val path = "factions." + faction.name
            config["$path.lore"] = faction.lore
            config["$path.item"] = faction.item
            config["$path.health"] = faction.health
            config["$path.damage"] = faction.damage
            config["$path.miningefficiency"] = faction.miningEfficiency
            config["$path.armor"] = faction.armor
            config["$path.raceInhabitants"] = faction.getRaceInhabitants()
        }

        try {
            config.save(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            player.getAttribute(Attribute.GENERIC_SCALE)?.baseValue =
                scale
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue =
                speed
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue =
                health.toDouble()
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH)?.baseValue =
                jumpHeight
            player.getAttribute(Attribute.GENERIC_SAFE_FALL_DISTANCE)?.baseValue =
                jumpHeight * 7.145
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue =
                damage
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.baseValue =
                reach
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)?.baseValue =
                reach
            player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)?.baseValue =
                attackSpeed
            player.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER)?.baseValue =
                fallDamageMultiplier
            player.getAttribute(Attribute.PLAYER_MINING_EFFICIENCY)?.baseValue =
                miningEfficiency
            player.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue =
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
            player.getAttribute(Attribute.GENERIC_SCALE)?.baseValue =
                race.scale
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue =
                race.speed
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue =
                (health + race.health).toDouble()
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH)?.baseValue =
                race.jumpHeight
            player.getAttribute(Attribute.GENERIC_SAFE_FALL_DISTANCE)?.baseValue =
                race.jumpHeight * 7.145
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue =
                damage + race.damage
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.baseValue =
                race.reach
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)?.baseValue =
                race.reach
            player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)?.baseValue =
                race.attackSpeed
            player.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER)?.baseValue =
                race.fallDamageMultiplier
            player.getAttribute(Attribute.PLAYER_MINING_EFFICIENCY)?.baseValue =
                miningEfficiency + race.miningEfficiency
            player.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue =
                armor + race.armor
        }
    }
}
