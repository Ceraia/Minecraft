package com.axodouble.modules;

import com.axodouble.Double;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleRaces implements CommandExecutor, TabCompleter, Listener {

    private final Double plugin;
    public List<Race> races = new ArrayList<>();
    public List<RaceFaction> raceFactions = new ArrayList<>();
    public Map<Player, Map<ItemStack, Race>> playerRaceSelection = new HashMap<>();
    public Map<Player, Map<ItemStack, RaceFaction>> playerRaceFactionSelection = new HashMap<>();
    public Map<Player, RaceFaction> playerRaceFactionSelected = new HashMap<>();
    public Map<Player, Race> playerRaceSelected = new HashMap<>();


    public ModuleRaces(Double plugin) {
        this.plugin = plugin;


        Objects.requireNonNull(plugin.getCommand("race")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("race")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        loadRaces();
        loadRaceFactions();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if(!racesEnabled()){
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Races are not enabled in the config!"));
            return true;
        }

        if(args.length == 0) {
            openFactionGUI(player);
            return true;
        }

        switch (args[0]) {
            case "reload" -> {
                if (!sender.hasPermission("double.races.reload")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }
                reloadRaces();
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloading races..."));
                return true;
            }
            case "become" -> {
                if (!sender.hasPermission("double.races.become")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Please specify what race you want to become."));
                    return true;
                }
                if (args.length > 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Too many arguments."));
                    return true;
                }
                // Find a race with the name
                Race selectedRace = null;
                for (Race race : races) {
                    if (Objects.equals(race.getName(), args[1])) {
                        selectedRace = race;
                    }
                }

                if (selectedRace == null) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Race not found!"));
                    return true;
                }
                else {
                    if (!sender.hasPermission("double.races.become." + args[1]) &&
                            !sender.hasPermission("double.races.become.*")) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to become this race"));
                        return true;
                    }
                    selectedRace.apply(player);
                    player.sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                    "<green>Succesfully changed your race to a <white>" + selectedRace.getName()
                            ));
                    return true;
                }
            }
            case "gui" -> openFactionGUI(player);
            case "restore" -> {
                if (!sender.hasPermission("double.races.restore")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }
                // Restore all races
                player.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                                "<green>Restoring all default races & factions..."
                        ));
                addDefaultRaces();
                addDefaultFactions();
                return true;
            }
            case "faction" -> {
                openFactionGUI(player);
                return true;
            }
        }

        openFactionGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        // Return a string list of all races
        List<String> tabOptions = new ArrayList<>();
        if (args.length == 1) {
            tabOptions.add("reload");
            tabOptions.add("become");
            tabOptions.add("gui");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("become")) {
                for (Race race : races) {
                    tabOptions.add(race.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("become")) {
                tabOptions.add("reload");
                tabOptions.add("become");
            }
        }
        List<String> returnedOptions = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length - 1], tabOptions, returnedOptions);

        return returnedOptions;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        } // If the item doesn't exist or is air, return

        if (Objects.requireNonNull(e.getInventory()).getType() == InventoryType.PLAYER) {
            return;
        } // If the inventory is the player's inventory, return

        Player player = (Player) e.getWhoClicked(); // Get the player who clicked

        if (Objects.equals(e.getView().title().toString(), MiniMessage.miniMessage().deserialize("<green>Select a race to become").toString())) {
            if (playerRaceSelection.containsKey(player)) {
                // Get the relevant slot and race that the player clicked on
                if(playerRaceSelection.get(player).containsKey(e.getCurrentItem())){
                    e.setCancelled(true);
                    playerRaceFactionSelected.get(player).apply(player, playerRaceSelection.get(player).get(e.getCurrentItem()));
                    e.getClickedInventory().close();
                    player.sendMessage(MiniMessage.miniMessage().deserialize("" +
                            "<green>Succesfully changed your faction to <white>" +
                            playerRaceFactionSelected.get(player).getName()+
                            "<green>, and your race to " +
                            playerRaceSelection.get(player).get(e.getCurrentItem()).getName()+"<green>!"));

                    plugin.getPlayerManager().getDoublePlayer(player).setRace(playerRaceSelection.get(player).get(e.getCurrentItem()).getName());
                    plugin.getPlayerManager().getDoublePlayer(player).setFaction(playerRaceFactionSelected.get(player).getName());
                }
            }
        } else if (Objects.equals(e.getView().title().toString(), MiniMessage.miniMessage().deserialize("<green>Select a faction to join").toString())) {
            if (playerRaceFactionSelection.containsKey(player)) {
                // Get the relevant slot and faction that the player clicked on
                if (playerRaceFactionSelection.get(player).containsKey(e.getCurrentItem())) {
                    e.setCancelled(true);
                    playerRaceFactionSelected.put(player, playerRaceFactionSelection.get(player).get(e.getCurrentItem()));
                    openRaceGUI(player);
                }
            }
        }
    }

    public boolean racesEnabled() {
        return plugin.getConfig().getBoolean("races-enabled", true);
    }

    public void reloadRaces() {
        races.clear();
        loadRaces(true);
    }

    public void addDefaultRaces() {
        races.clear();
        {
            races.add(new Race(
                    "Halfling", // Name
                    0.54, // Scale
                    0.12, // Speed
                    14, // Health
                    0.42, // Jumpheight
                    0.95, // Damage
                    2.5, // Reach
                    4.5, // Attack Speed
                    "<gray>Nimble and stealthy,<newline><green>Halflings<gray> excel in evading danger.", // Lore
                    1, // Fall Damage Multiplier
                    5.2, // Mining Efficiency
                    0, // Armor
                    new ItemStack(Material.POTATO) // Item
            ));

            races.add(new Race(
                    "Gnome", // Name
                    0.7, // Scale
                    0.11, // Speed
                    16, // Health
                    0.42, // Jumpheight
                    0.9, // Damage
                    3, // Reach
                    5, // Attack Speed
                    "<gray>Clever and elusive,<newline><green>Gnomes<gray> use their fast attack to outwit foes.", // Lore
                    1, // Fall Damage Multiplier
                    6.65, // Mining Efficiency
                    0, // Armor
                    new ItemStack(Material.RED_MUSHROOM) // Item
            ));

            races.add(new Race(
                    "Dwarf", // Name
                    0.9, // Scale
                    0.1, // Speed
                    24, // Health
                    0.42, // Jumpheight
                    1, // Damage
                    4.5, // Reach
                    4, // Attack Speed
                    "<gray>Sturdy and relentless,<newline><green>Dwarves<gray> are master miners and warriors.", // Lore
                    1, // Fall Damage Multiplier
                    9.95, // Mining Efficiency
                    2, // Armor
                    new ItemStack(Material.IRON_ORE) // Item
            ));

            races.add(new Race(
                    "Short Human", // Name
                    0.95, // Scale
                    0.1, // Speed
                    20, // Health
                    0.42, // Jumpheight
                    1, // Damage
                    5, // Reach
                    4, // Attack Speed
                    "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.", // Lore
                    1, // Fall Damage Multiplier
                    0, // Mining Efficiency
                    0, // Armor
                    new ItemStack(Material.BREAD) // Item
            ));

            races.add(new Race(
                    "Human", // Name
                    1, // Scale
                    0.1, // Speed
                    20, // Health
                    0.42, // Jumpheight
                    1, // Damage
                    5, // Reach
                    4, // Attack Speed
                    "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.", // Lore
                    1, // Fall Damage Multiplier
                    0, // Mining Efficiency
                    0, // Armor
                    new ItemStack(Material.BREAD) // Item
            ));

            races.add(new Race(
                    "Tall Human", // Name
                    1.05, // Scale
                    0.1, // Speed
                    20, // Health
                    0.42, // Jumpheight
                    1, // Damage
                    5, // Reach
                    4, // Attack Speed
                    "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.", // Lore
                    1, // Fall Damage Multiplier
                    0, // Mining Efficiency
                    0, // Armor
                    new ItemStack(Material.BREAD) // Item
            ));

            races.add(new Race(
                    "Satyr", // Name
                    1.05, // Scale
                    0.13, // Speed
                    18, // Health
                    0.52, // Jumpheight
                    1, // Damage
                    5, // Reach
                    3.9, // Attack Speed
                    "<gray>Fast and intelligent<newline><green>Satyrs<gray> are expert explorers and have seen far across the world.", // Lore
                    1, // Fall Damage Multiplier
                    0, // Mining Efficiency
                    0, // Armor
                    new ItemStack(Material.RABBIT_FOOT) // Item
            ));

            races.add(new Race(
                    "Githyanki", // Name
                    1.07, // Scale
                    0.975, // Speed
                    22, // Health
                    0.42, // Jumpheight
                    1, // Damage
                    5, // Reach
                    4.2, // Attack Speed
                    "<gray>Fierce and fast,<newline><green>Githyanki<gray> are veteran warriors.", // Lore
                    1, // Fall Damage Multiplier
                    0, // Mining Efficiency
                    0, // Armor
                    new ItemStack(Material.NETHER_STAR) // Item
            ));

            races.add(new Race(
                    "Half-Elf", // Name
                    1.11, // Scale
                    0.0975, // Speed
                    24, // Health
                    0.52, // Jumpheight
                    1, // Damage
                    5, // Reach
                    3.95, // Attack Speed
                    "<gray>Graceful but adaptable,<newline><green>Half-elves<gray> are the result of a Elven - Human relationship.", // Lore
                    0.925, // Fall Damage Multiplier
                    1, // Mining Efficiency
                    1, // Armor
                    new ItemStack(Material.APPLE) // Item
            ));

            races.add(new Race(
                    "Elf", // Name
                    1.11, // Scale
                    0.095, // Speed
                    26, // Health
                    0.63, // Jumpheight
                    1, // Damage
                    5, // Reach
                    3.9, // Attack Speed
                    "<gray>Graceful and wise,<newline><green>Elves<gray> are good fighters and excel in archery.", // Lore
                    0.75, // Fall Damage Multiplier
                    1, // Mining Efficiency
                    2, // Armor
                    new ItemStack(Material.BOW) // Item
            ));

            races.add(new Race(
                    "Half-Orc", // Name
                    1.15, // Scale
                    0.0925, // Speed
                    26, // Health
                    0.63, // Jumpheight
                    1, // Damage
                    5.5, // Reach
                    3.8, // Attack Speed
                    "<gray>Strong and fierce,<newline><green>Half-orcs<gray> are the result of a Orc - Human relationship.", // Lore
                    0.75, // Fall Damage Multiplier
                    2, // Mining Efficiency
                    3, // Armor
                    new ItemStack(Material.PORKCHOP) // Item
            ));

            races.add(new Race(
                    "Bugbear", // Name
                    1.22, // Scale
                    0.09, // Speed
                    28, // Health
                    0.63, // Jumpheight
                    1.25, // Damage
                    5.8, // Reach
                    3, // Attack Speed
                    "<gray>Fierce and powerful,<newline><green>Bugbears<gray> dominate in brute strength.", // Lore
                    0.75, // Fall Damage Multiplier
                    2, // Mining Efficiency
                    4, // Armor
                    new ItemStack(Material.BEEF) // Item
            ));

            races.add(new Race(
                    "Goliath", // Name
                    1.33, // Scale
                    0.08, // Speed
                    30, // Health
                    0.63, // Jumpheight
                    1.25, // Damage
                    6.1, // Reach
                    3, // Attack Speed
                    "<gray>Large and strong,<newline><green>Goliaths<gray> are often used in heavy labour.", // Lore
                    0.75, // Fall Damage Multiplier
                    0, // Mining Efficiency
                    5, // Armor
                    new ItemStack(Material.OAK_SAPLING) // Item
            ));
        }
        saveAllRaces();
        races.clear();
        loadRaces();
    }

    public void addDefaultFactions(){
        raceFactions.clear();
        {
            raceFactions.add(new RaceFaction(
                    "<red>Gnomish Dynasty",
                    "<red>The Gnomish Dynasty<gray>, formed by the Gnomish, a monarchy.",
                    new ItemStack(Material.RED_MUSHROOM),
                    0,
                    0,
                    0,
                    0,
                    Arrays.asList("Gnome", "Satyr", "Githyanki", "Bugbear")
                    )
            );
            raceFactions.add(new RaceFaction(
                    "<blue>Republic of Man",
                    "<blue>The Republic of Man<gray>, formed by the Humans, a democratic republic.",
                    new ItemStack(Material.BREAD),
                    0,
                    0,
                    0,
                    0,
                    Arrays.asList("Dwarf", "Short Human", "Human", "Tall Human", "Half-Elf", "Half-Orc")
                    )
            );
            raceFactions.add(new RaceFaction(
                    "<green>Elven Demagogue",
                    "<green>The Elven Demagogue<gray>, formed by the Elves, a theocracy.",
                    new ItemStack(Material.PORKCHOP),
                    0,
                    0,
                    0,
                    0,
                    Arrays.asList("Halfling", "Half-Elf", "Elf", "Goliath")
                    )
            );
            raceFactions.add(new RaceFaction(
                    "<yellow>Unaligned",
                    "<yellow>The Unaligned<gray>, formed by those with no king, a loose alliance.",
                    new ItemStack(Material.IRON_ORE),
                    0,
                    0,
                    0,
                    0,
                    Arrays.asList(
                            "*"
                    )
                    )
            );

        }
        saveAllFactions();
        raceFactions.clear();
        loadRaceFactions();
    }

    public void loadRaces(boolean reload) {
        if(reload) plugin.getLogger().info("Reloading races...");
        else plugin.getLogger().info("Loading races...");

        // Load all races from the races.yml file
        File file = new File(plugin.getDataFolder(), "races.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection racesSection = config.getConfigurationSection("races");
        if (racesSection == null) {
            plugin.getLogger().warning("No races found in races.yml! Adding default races.");
            addDefaultRaces();
            return;
        }

        for (String raceName : racesSection.getKeys(false)) {
            String path = "races." + raceName;
            Race race = new Race(
                    raceName,
                    config.getDouble(path + ".scale", 1),
                    config.getDouble(path + ".speed", 0.1),
                    config.getInt(path + ".health", 20),
                    config.getDouble(path + ".jumpheight", 0.42),
                    config.getDouble(path + ".damage", 1),
                    config.getDouble(path + ".reach", 5),
                    config.getDouble(path + ".attackspeed", 4),
                    config.getString(path + ".lore", "<gray>No known lore..."),
                    config.getDouble(path + ".falldamagemultiplier", 1),
                    config.getDouble(path + ".miningefficiency", 0),
                    config.getDouble(path + ".armor", 0),
                    config.getItemStack(path + ".item", new ItemStack(Material.BREAD))
            );
            races.add(race);
        }
    }

    public void loadRaceFactions(boolean reload) {
        if (reload) plugin.getLogger().info("Reloading race factions...");
        else plugin.getLogger().info("Loading race factions...");

        // Load all factions from the races.yml file
        File file = new File(plugin.getDataFolder(), "races.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection factionsSection = config.getConfigurationSection("factions");
        if (factionsSection == null) {
            plugin.getLogger().warning("No factions found in races.yml! Adding default factions.");
            addDefaultFactions();
            return;
        }

        for (String factionName : factionsSection.getKeys(false)) {
            String path = "factions." + factionName;
            RaceFaction faction = new RaceFaction(
                    factionName,
                    config.getString(path + ".lore", "<gray>No known lore..."),
                    config.getItemStack(path + ".item", new ItemStack(Material.BREAD)),
                    config.getInt(path + ".health", 0),
                    config.getDouble(path + ".damage", 0),
                    config.getDouble(path + ".miningefficiency", 0),
                    config.getDouble(path + ".armor", 0),
                    config.getStringList(path + ".raceInhabitants")
            );
            raceFactions.add(faction);
        }
    }

    public void loadRaceFactions(){
        loadRaceFactions(false);
    }

    public void loadRaces(){
        loadRaces(false);
    }

    private void openRaceGUI(Player player) {
        List<Race> selectable = new ArrayList<>();
        for (Race race : races) {
            if(
                    (player.hasPermission("double.races.become." + race.getName()) ||
                    player.hasPermission("double.races.become.*")) &&
                            (playerRaceFactionSelected.get(player).getRaceInhabitants().contains(race.getName())
                            || playerRaceFactionSelected.get(player).getRaceInhabitants().contains("*"))
            ) selectable.add(race);
        }

        // Open a GUI with all selectable races
        int size = Math.max(9, (selectable.size() + 8) / 9 * 9);
        Inventory inv = Bukkit.createInventory(null, size, MiniMessage.miniMessage().deserialize("<green>Select a race to become"));

        Map<ItemStack, Race> raceSelectSlots = new HashMap<>();

        AtomicInteger i = new AtomicInteger(); // Slot
        selectable.forEach(race -> {
                ItemStack itemStack = race.getItem(); // Create the itemstack
                ItemMeta meta = itemStack.getItemMeta();
                meta.displayName(
                        MiniMessage.miniMessage().deserialize("<green>" +race.getName() + "</green>")
                );

                List<Component> lore = new ArrayList<>();

                Arrays.stream(race.getLore().split("<newline>")).toList().forEach(s -> lore.add(MiniMessage.miniMessage().deserialize(s)));
                //lore.add(MiniMessage.miniMessage().deserialize(race.getLore()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Scale : <green>" + race.getScale()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Speed : <green>" + race.getSpeed()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Health : <green>" + race.getHealth()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Jump Height : <green>" + race.getJumpHeight()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Damage : <green>"+race.getDamage()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Reach : <green>"+race.getReach()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Attack Speed : <green>"+race.getAttackSpeed()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Fall Damage Multiplier : <green>"+race.getFallDamageMultiplier()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Mining Efficiency : <green>"+race.getMiningEfficiency()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Armor : <green>"+race.getArmor()));

                meta.lore(lore);

                itemStack.setItemMeta(meta);
                itemStack.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

                inv.setItem(i.get(), itemStack);
                raceSelectSlots.put(itemStack, race);
                i.getAndIncrement();
            });
        playerRaceSelection.put(player, raceSelectSlots);
        player.openInventory(inv);

    }

    private void openFactionGUI(Player player) {
        List<RaceFaction> selectable = new ArrayList<>(raceFactions);
        // Open a GUI with all selectable factions
        int size = Math.max(9, (selectable.size() + 8) / 9 * 9);
        Inventory inv = Bukkit.createInventory(null, size, MiniMessage.miniMessage().deserialize("<green>Select a faction to join"));

        Map<ItemStack, RaceFaction> factionSelectSlots = new HashMap<>();

        AtomicInteger i = new AtomicInteger(); // Slot
        selectable.forEach(faction -> {
            ItemStack itemStack = faction.getItem(); // Create the itemstack
            ItemMeta meta = itemStack.getItemMeta();
            meta.displayName(
                    MiniMessage.miniMessage().deserialize(faction.getName())
            );

            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize(faction.getLore()));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Health Bonus : <green>" + faction.getHealth()));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Damage Bonus : <green>" + faction.getDamage()));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Mining Efficiency Bonus : <green>" + faction.getMiningEfficiency()));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Armor Bonus : <green>" + faction.getArmor()));

            meta.lore(lore);

            itemStack.setItemMeta(meta);
            itemStack.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

            inv.setItem(i.get(), itemStack);
            factionSelectSlots.put(itemStack, faction);
            i.getAndIncrement();
        });
        playerRaceFactionSelection.put(player, factionSelectSlots);
        player.openInventory(inv);
    }

    public void saveAllRaces() {
        // Save the races to the races.yml file
        File file = new File(plugin.getDataFolder(), "races.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (Race race : races) {
            String path = "races." + race.getName();
            config.set(path + ".lore", race.getLore());
            config.set(path + ".scale", race.getScale());
            config.set(path + ".speed", race.getSpeed());
            config.set(path + ".health", race.getHealth());
            config.set(path + ".jumpheight", race.getJumpHeight());
            config.set(path + ".damage", race.getDamage());
            config.set(path + ".reach", race.getReach());
            config.set(path + ".attackspeed", race.getAttackSpeed());
            config.set(path + ".item", race.getItem());
            config.set(path + ".falldamagemultiplier", race.getFallDamageMultiplier());
            config.set(path + ".miningefficiency", race.getMiningEfficiency());
            config.set(path + ".armor", race.getArmor());
        }

        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAllFactions() {
        // Save the factions to the races.yml file
        File file = new File(plugin.getDataFolder(), "races.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (RaceFaction faction : raceFactions) {
            String path = "factions." + faction.getName();
            config.set(path + ".lore", faction.getLore());
            config.set(path + ".item", faction.getItem());
            config.set(path + ".health", faction.getHealth());
            config.set(path + ".damage", faction.getDamage());
            config.set(path + ".miningefficiency", faction.getMiningEfficiency());
            config.set(path + ".armor", faction.getArmor());
            config.set(path + ".raceInhabitants", faction.getRaceInhabitants());
        }

        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Race {
        private final String name;
        private final double scale;
        private final double speed;
        private final int health;
        private final double jumpHeight;
        private final double damage;
        private final double reach;
        private final double attackSpeed;
        private final String lore;
        private final double fallDamageMultiplier ;
        private final double miningEfficiency ;
        private final double armor ;
        private final ItemStack item;

        public Race(String name,
                    double scale,
                    double speed,
                    int health,
                    double jumpHeight,
                    double damage,
                    double reach,
                    double attackSpeed,
                    String lore,
                    double fallDamageMultiplier,
                    double miningEfficiency,
                    double armor,
                    ItemStack item
        ) {
            this.name = name;
            this.scale = scale;
            this.speed = speed;
            this.health = health;
            this.jumpHeight = jumpHeight;
            this.damage = damage;
            this.reach = reach;
            this.attackSpeed = attackSpeed;
            this.lore = lore;
            this.fallDamageMultiplier = fallDamageMultiplier;
            this.miningEfficiency = miningEfficiency;
            this.armor = armor;
            this.item = item;

        }

        public String getName() {
            return name;
        }

        public double getScale() {
            return scale;
        }

        public double getSpeed() {
            return speed;
        }

        public int getHealth() {
            return health;
        }

        public double getJumpHeight() {
            return jumpHeight;
        }

        public double getDamage() {
            return damage;
        }

        public double getReach() {
            return reach;
        }

        public double getAttackSpeed() {
            return attackSpeed;
        }

        public ItemStack getItem() {
            return item;
        }

        public double getFallDamageMultiplier() {
            return fallDamageMultiplier;
        }

        public double getMiningEfficiency() {
            return miningEfficiency;
        }

        public double getArmor() {
            return armor;
        }

        public String getLore() {
            return lore;
        }

        public void apply(Player player) {
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(scale);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(speed);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(health);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH)).setBaseValue(jumpHeight);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_SAFE_FALL_DISTANCE)).setBaseValue(jumpHeight * 7.145);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(damage);
            Objects.requireNonNull(player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)).setBaseValue(reach);
            Objects.requireNonNull(player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)).setBaseValue(reach);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(attackSpeed);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER)).setBaseValue(fallDamageMultiplier);
            Objects.requireNonNull(player.getAttribute(Attribute.PLAYER_MINING_EFFICIENCY)).setBaseValue(miningEfficiency);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ARMOR)).setBaseValue(armor);

        }
    }

    public static class RaceFaction {
        private final String name;
        private final String lore;
        private final ItemStack item;
        private final int health;
        private final double damage;
        private final double miningEfficiency ;
        private final double armor ;
        private final List<String> raceInhabitants = new ArrayList<>();

        public RaceFaction(String name, String lore, ItemStack item, int health, double damage, double miningEfficiency, double armor, List<String> raceInhabitants) {
            this.name = name;
            this.lore = lore;
            this.item = item;
            this.health = health;
            this.damage = damage;
            this.miningEfficiency = miningEfficiency;
            this.armor = armor;
            this.raceInhabitants.addAll(raceInhabitants);
        }

        public String getName() {
            return name;
        }

        public String getLore() {
            return lore;
        }

        public ItemStack getItem() {
            return item;
        }

        public int getHealth() {
            return health;
        }

        public double getDamage() {
            return damage;
        }

        public double getMiningEfficiency() {
            return miningEfficiency;
        }

        public double getArmor() {
            return armor;
        }

        public List<String> getRaceInhabitants() {
            return raceInhabitants;
        }

        public void apply (Player player, Race race){
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(race.getScale());
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(race.getSpeed());
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(health + race.getHealth());
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH)).setBaseValue(race.getJumpHeight());
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_SAFE_FALL_DISTANCE)).setBaseValue(race.getJumpHeight() * 7.145);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(damage+ race.getDamage());
            Objects.requireNonNull(player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)).setBaseValue(race.getReach());
            Objects.requireNonNull(player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)).setBaseValue(race.getReach());
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(race.getAttackSpeed());
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER)).setBaseValue(race.getFallDamageMultiplier());
            Objects.requireNonNull(player.getAttribute(Attribute.PLAYER_MINING_EFFICIENCY)).setBaseValue(miningEfficiency+race.getMiningEfficiency());
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ARMOR)).setBaseValue(armor+race.getArmor());

        }
    }
}
