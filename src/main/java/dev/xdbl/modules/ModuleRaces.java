package dev.xdbl.modules;

import dev.xdbl.Double;
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
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModuleRaces implements CommandExecutor, TabCompleter, Listener {

    private final Double plugin;
    public List<Race> races;

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


        switch (args[0]) {
            case "reload" -> {
                reloadRaces();
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloading races..."));
                return true;
            }
            case "become" -> {
                if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Please specify what race you want to become."));
                }
                if (args.length > 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Too many arguments."));
                }
                // Find a race with the name
                Race selectedRace = null;
                for (Race race : races) {
                    if (Objects.equals(race.getName(), args[1])) {
                        selectedRace = race;
                    }
                }

                if (selectedRace == null)
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Race not found!"));
                else {
                    selectedRace.apply(player);
                    player.sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                    "<green>Succesfully changed your race to a <white>" + selectedRace.getName()
                            ));
                }
                return true;
            }
            case "restore" -> {
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
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("reload");
            options.add("become");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("become")) {
                for (Race race : races) {
                    options.add(race.getName());
                }
            }
        }

        return options;
    }

    public void reloadRaces() {
        races.clear();
        loadRaces();
    }

    public void addDefaultRaces() {
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
                new ItemStack(ItemStack.of(Material.POTATO)),
                new File(f, "Halfling.yml")
        ).saveFile());
        races.add(new Race(
                "Dwarven",
                0.9,
                0.09,
                24,
                0.42,
                1,
                4.5,
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
                new ItemStack(ItemStack.of(Material.BOW)),
                new File(f, "Elven.yml")
        ).saveFile());
        races.add(new Race(
                "Bugbear",
                1.33,
                0.09,
                30,
                0.64,
                1.1,
                6.65,
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

    public class Race {
        private final String name;
        private final double scale;
        private final double speed;
        private final int health;
        private final double jumpHeight;
        private final double damage;
        private final double reach;
        private final ItemStack item;
        private final File configFile;

        public Race(String name,
                    double scale,
                    double speed,
                    int health,
                    double jumpHeight,
                    double damage,
                    double reach,
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

        public ItemStack getItem() {
            return item;
        }

        public File getConfigFile() {
            return configFile;
        }

        public Race saveFile() {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
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
            player.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(reach);
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(reach);
        }
    }
}
