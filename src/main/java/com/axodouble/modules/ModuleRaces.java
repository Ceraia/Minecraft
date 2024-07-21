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
    public List<Race> races;
    public Map<Player, Map<ItemStack, Race>> playerOpenGuis = new HashMap<>();

    public ModuleRaces(Double plugin) {
        this.plugin = plugin;
        this.races = new ArrayList<>();


        Objects.requireNonNull(plugin.getCommand("race")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("race")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        reloadRaces();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if(args.length == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid arguments!"));
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
            case "gui" -> {
                if (!sender.hasPermission("double.races.become")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }
                List<Race> selectable = new ArrayList<>();
                for (Race race : races) {
                    if(sender.hasPermission("double.races.become." + race.getName()) ||
                            sender.hasPermission("double.races.become.*")) selectable.add(race);
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

                    meta.lore(lore);

                    itemStack.setItemMeta(meta);
                    itemStack.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

                    inv.setItem(i.get(), itemStack);
                    raceSelectSlots.put(itemStack, race);
                    i.getAndIncrement();
                });
                playerOpenGuis.put(player, raceSelectSlots);
                player.openInventory(inv);
                return true;
            }
            case "restore" -> {
                if (!sender.hasPermission("double.races.restore")) {
                    this.plugin.noPermission((Player) sender);
                    return true;
                }
                // Restore all races
                player.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                                "<green>Restoring all default races..."
                        ));
                addDefaultRaces();
                return true;
            }
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid arguments!"));

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
            if (playerOpenGuis.containsKey(player)) {
                // Get the relevant slot and race that the player clicked on
                if(playerOpenGuis.get(player).containsKey(e.getCurrentItem())){
                    e.setCancelled(true);
                    playerOpenGuis.get(player).get(e.getCurrentItem()).apply(player);
                }
            }
        }
    }

    public void reloadRaces() {
        races.clear();
        loadRaces();
    }

    public void addDefaultRaces() {
        races.clear();
        File f = new File(plugin.getDataFolder(), "races");
        if (!f.exists())
            f.mkdirs();

        races.add(new Race(
                "Halfling",
                0.54,
                0.13,
                14,
                0.42,
                0.9,
                2.7,
                4,
                "<gray>Nimble and stealthy,<newline><green>Halflings<gray> excel in evading danger.",
                new ItemStack(ItemStack.of(Material.POTATO)),
                new File(f, "Halfling.yml")
        ).saveFile());
        races.add(new Race(
                "Gnome",
                0.6,
                0.12,
                16,
                0.42,
                0.95,
                3.15,
                4,
                "<gray>Clever and elusive,<newline><green>Gnomes<gray> use their fast attack to outwit foes.",
                new ItemStack(ItemStack.of(Material.RED_MUSHROOM)),
                new File(f, "Gnome.yml")
        ).saveFile());
        races.add(new Race(
                "Dwarven",
                0.9,
                0.09,
                24,
                0.42,
                1,
                4.5,
                4.2,
                "<gray>Sturdy and relentless,<newline><green>Dwarves<gray> are master miners and warriors.",
                new ItemStack(ItemStack.of(Material.IRON_PICKAXE)),
                new File(f, "Dwarven.yml")
        ).saveFile());
        races.add(new Race(
                "Short-Human",
                0.95,
                0.1,
                20,
                0.42,
                1,
                5,
                4,
                "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.",
                new ItemStack(ItemStack.of(Material.BREAD)),
                new File(f, "Short-Human.yml")
        ).saveFile());
        races.add(new Race(
                "Human",
                1,
                0.1,
                20,
                0.42,
                1,
                5,
                4,
                "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.",
                new ItemStack(ItemStack.of(Material.BREAD)),
                new File(f, "Human.yml")
        ).saveFile());
        races.add(new Race(
                "Tall-Human",
                1.05,
                0.1,
                20,
                0.42,
                1,
                5,
                4,
                "<gray>Balanced and adaptable,<newline><green>Humans<gray> thrive in any environment.",
                new ItemStack(ItemStack.of(Material.BREAD)),
                new File(f, "Tall-Human.yml")
        ).saveFile());
        races.add(new Race(
                "Elven",
                1.11,
                0.095,
                26,
                0.504,
                1.05,
                5.55,
                4.5,
                "<gray>Graceful and wise,<newline><green>Elves<gray> are good fighters and excel in archery.",
                new ItemStack(ItemStack.of(Material.BOW)),
                new File(f, "Elven.yml")
        ).saveFile());
        races.add(new Race(
                "Bugbear",
                1.33,
                0.08,
                30,
                0.64,
                1.1,
                6.65,
                3.2,
                "<gray>Fierce and powerful,<newline><green>Bugbears<gray> dominate in brute strength.",
                new ItemStack(ItemStack.of(Material.BEEF)),
                new File(f, "Bugbear.yml")
        ).saveFile());
    }

    public void loadRaces() {
        plugin.getLogger().info("(Re)Loading races...");
        // Load all races
        File f = new File(plugin.getDataFolder(), "races");
        if (!f.exists()) {
            f.mkdirs();
            // Add the default races
            addDefaultRaces();
        }

        File[] files = f.listFiles();
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            Race race = new Race(
                    file.getName().split("\\.")[0],
                    config.getDouble("scale", 1),
                    config.getDouble("speed", 0.1),
                    config.getInt("health", 20),
                    config.getDouble("jumpheight", 0.42),
                    config.getDouble("damage", 1),
                    config.getDouble("reach", 5),
                    config.getDouble("attackspeed", 4),
                    config.getString("lore", "<gray>No known lore..."),
                    config.getItemStack("item", new ItemStack(ItemStack.of(Material.BREAD))),
                    file
            );
            races.add(race);
        }
    }

    public void saveRaces() {
        // Save all races
        for (Race race : races) {
            race.saveFile();
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
        private final ItemStack item;
        private final File configFile;

        public Race(String name,
                    double scale,
                    double speed,
                    int health,
                    double jumpHeight,
                    double damage,
                    double reach,
                    double attackSpeed,
                    String lore,
                    ItemStack item,
                    File configFile
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
            this.item = item;
            this.configFile = configFile;
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
        public String getLore() {
            return lore;
        }

        public File getConfigFile() {
            return configFile;
        }

        public Race saveFile() {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("lore", lore);
            config.set("scale", scale);
            config.set("speed", speed);
            config.set("health", health);
            config.set("jumpheight", jumpHeight);
            config.set("damage", damage);
            config.set("reach", reach);
            config.set("item", item);
            try {
                config.save(configFile);
                return this;
            } catch (Exception e) {
                e.printStackTrace();
                return this;
            }
        }

        public void apply(Player player) {
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(scale);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(speed);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(health);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(damage);
            Objects.requireNonNull(player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)).setBaseValue(reach);
            Objects.requireNonNull(player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)).setBaseValue(reach);
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(attackSpeed);
        }
    }
}
