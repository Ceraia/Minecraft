package dev.xdbl.modules;

import dev.xdbl.Double;
import dev.xdbl.types.DoublePlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

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

        if (args[0].equalsIgnoreCase("reload")) {
            reloadRaces();
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloading races..."));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        // Return a string list of all races
        List<String> options = new ArrayList<>();
        for (Race race : races) {
            options.add(race.getName());
        }

        return options;
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

        public void saveFile() {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void apply(Player player) {
            player.registerAttribute(Attribute.GENERIC_MAX_HEALTH);
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
            player.registerAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
            player.registerAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            player.registerAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(reach);
        }
    }

    public void reloadRaces(){
        races.clear();
        loadRaces();
    }
    public void loadRaces(){
        plugin.getLogger().info("(Re)Loading races...");
        // Load all races
        File f = new File(plugin.getDataFolder(), "races");
        if (!f.exists()) {
            f.mkdirs();
            // Add the default races
            Race race = new Race(
                    "Human",
                    1,
                    0.1,
                    20,
                    0.42,
                    1,
                    5,
                    new ItemStack(ItemStack.of(Material.BREAD)),
                    new File(f, "Human.yml")
            );

            race.saveFile();
            races.add(race);
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
    public void saveRaces(){
        // Save all races
        for (Race race : races) {
            race.saveFile();
        }
    }
}
