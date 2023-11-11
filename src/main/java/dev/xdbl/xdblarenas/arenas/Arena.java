package dev.xdbl.xdblarenas.arenas;

import dev.xdbl.xdblarenas.InviteManager;
import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.Utils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Arena {

    private final XDBLArena plugin;

    // before ready
    private final String name;
    private final String owner;
    private boolean isPublic = false;
    private File configFile;

    // after ready
    private Location spawnPoint1, spawnPoint2;
    private boolean isReady = false;

    // after start
    private List<Player> team1 = new ArrayList<>();
    private List<Player> team2 = new ArrayList<>();
    private List<Location> placedBlocks = new ArrayList<>();
    private Map<Player, Location> priorLocations = new HashMap<>();

    private ArenaState state = ArenaState.WAITING;

    private final List<Player> startPlayers = new ArrayList<>();

    public Arena(XDBLArena plugin, String name, String owner, Location spawnPoint1, Location spawnPoint2, boolean isPublic, File configFile) {
        this.plugin = plugin;

        this.name = name;
        this.owner = owner;
        this.spawnPoint1 = spawnPoint1;
        this.spawnPoint2 = spawnPoint2;
        this.isReady = true;
        this.isPublic = isPublic;

        this.configFile = configFile;
    }

    public void setSpawnPoint1(Location loc) {
        spawnPoint1 = loc;
        saveArena();
    }

    public void setSpawnPoint2(Location loc) {
        spawnPoint2 = loc;
        saveArena();
    }

    public boolean saveArena() {
        try {
            configFile = new File(plugin.getDataFolder(), "data/arenas/" + name + ".yml");

            // Check if the file already exists
            if (!configFile.exists()) {
                configFile.createNewFile();
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Update or set the values
            config.set("name", name);
            config.set("owner", owner);

            config.set("spawnPoint1", spawnPoint1);
            config.set("spawnPoint2", spawnPoint2);
            config.set("public", isPublic);

            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public boolean delete() {
        return configFile.delete();
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public Location getSpawnPoint1() {
        return spawnPoint1;
    }

    public Location getSpawnPoint2() {
        return spawnPoint2;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public List<Player> getTeam1() {
        return team1;
    }

    public void setTeam1(List<Player> team1) {
        this.team1 = team1;
    }

    public List<Player> getTeam2() {
        return team2;
    }

    public void setTeam2(List<Player> team2) {
        this.team2 = team2;
    }

    public void addPlayer(Player player, int team) {
        plugin.getArenaManager().addPlayerToArena(player, this);
        if (team == 1) {
            team1.add(player);
        } else {
            team2.add(player);
        }

        startPlayers.add(player);
    }

    public List<Player> getStartPlayers() {
        return startPlayers;
    }

    public List<Player> getOnlinePlayers() {
        List<Player> onlinePlayers = new ArrayList<>();
        onlinePlayers.addAll(team1);
        onlinePlayers.addAll(team2);

        return onlinePlayers;
    }

    public void reset() {
        team1.clear();
        team2.clear();
        startPlayers.clear();
        placedBlocks.clear();
        priorLocations.clear();
    }

    public void end(Player player, boolean quit) {
        boolean end = false;
        List<Player> winners = new ArrayList<>();
        List<Player> losers = new ArrayList<>();

        if (this.getTeam1().contains(player)) {
            if (this.getTeam1().size() <= 1) {
                end = true;

                winners.addAll(this.getTeam2());
                losers.addAll(this.getTeam1());
            } else {
                List<Player> team = this.getTeam1();
                team.remove(player);
                this.setTeam1(team);
            }
        } else {
            if (this.getTeam2().size() <= 1) {
                end = true;

                winners.addAll(this.getTeam1());
                losers.addAll(this.getTeam2());
            } else {
                List<Player> team = this.getTeam2();
                team.remove(player);
                this.setTeam2(team);
            }
        }

        if (!end || quit) {
            Utils.teleportPlayerToSpawn(plugin, player, this);
            plugin.getArenaManager().removePlayerFromArena(player);

            player.getInventory().clear();
            Utils.revertInventory(plugin, player, this);
            if (!end) {
                return;
            }
        }

        for (Player pl : this.getOnlinePlayers()) {
            if (pl == player && quit) {
                continue;
            }

            pl.sendMessage(
                    plugin.getConfig().getString("messages.fight.end")
                            .replace("%winner%", winners.stream().map(Player::getName).collect(Collectors.joining(", ")))
                            .replace("%time%", String.valueOf(plugin.getConfig().getInt("cooldown.after")))
                            .replace("&", "§")
            );

            pl.getInventory().clear();

            pl.setHealth(20);
            pl.setFireTicks(0);
            pl.setFoodLevel(20);
            pl.setSaturation(20);
        }

        this.setState(ArenaState.ENDING);

        Arena thisArena = this;

        new BukkitRunnable() {
            public void run() {
                for (Location loc : placedBlocks) {
                    loc.getBlock().setType(Material.AIR);
                }

                for (Player pl : getOnlinePlayers()) {
                    if (pl == player && quit) {
                        continue;
                    }
                    Utils.teleportPlayerToSpawn(plugin, pl, thisArena);

                    plugin.getArenaManager().removePlayerFromArena(pl);

                    Utils.revertInventory(plugin, pl, thisArena);
                }

                // Reward
                for (Player pl : winners) {
                    for (String command : plugin.getConfig().getStringList("rewards")) {
                        pl.performCommand(command.replace("%player%", pl.getName()));
                    }
                }

                // Reward losers
                for (Player pl : losers) {
                    for (String command : plugin.getConfig().getStringList("rewards_lose")) {
                        pl.performCommand(command.replace("%player%", pl.getName()));
                    }
                }

                thisArena.setState(ArenaState.WAITING);
                reset();
            }
        }.runTaskLater(plugin, plugin.getConfig().getInt("cooldown.after") * 20L);
    }

    public void start(InviteManager.Invite invite, List<Player> players) {
        this.setState(Arena.ArenaState.STARTING);

        try {
            for (Player pl : players) {
                priorLocations.put(pl, pl.getLocation());

                ItemStack[] content = pl.getInventory().getContents();

                File file = new File(plugin.getDataFolder(), "data/pinventory_" + this.getName() + "_" + pl.getName() + ".yml");
                file.createNewFile();

                FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                // list of itemstack from array
                for (int i = 0; i < content.length; i++) {
                    if (content[i] == null) {
                        yaml.set("items." + i, "null");
                    } else {
                        yaml.set("items." + i, content[i]);
                    }
                }

                yaml.save(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Problem saving inventories, nothing was deleted!");
            for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                pl.sendMessage(
                        plugin.getConfig().getString("messages.fight.problem_saving_inventories").replace("&", "§")
                );
            }

            return;
        }

        for (Player pl : this.getTeam1()) {
            pl.teleport(
                    this.getSpawnPoint1()
            );
        }
        for (Player pl : this.getTeam2()) {
            pl.teleport(
                    this.getSpawnPoint2()
            );
        }

        for (Player pl : players) {
            pl.setHealth(20);
            pl.setFoodLevel(20);
            pl.setGameMode(GameMode.SURVIVAL);
        }

        this.setState(Arena.ArenaState.STARTING);

        AtomicInteger i = new AtomicInteger(
                plugin.getConfig().getInt("cooldown.before")
        );

        Arena thisArena = this;
        new BukkitRunnable() {
            public void run() {
                for (Player pl : players) {
                    if (i.get() == 0) {
                        pl.sendMessage(
                                plugin.getConfig().getString("messages.fight.started")
                                        .replace("&", "§")
                        );
                    } else {
                        pl.sendMessage(
                                plugin.getConfig().getString("messages.fight.starting")
                                        .replace("%time%", String.valueOf(i.get()))
                                        .replace("&", "§")
                        );
                    }
                }

                if (i.get() == 0) {
                    thisArena.setState(Arena.ArenaState.RUNNING);
                    cancel();
                    return;
                }

                i.decrementAndGet();

            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public enum ArenaState {
        WAITING, STARTING, RUNNING, ENDING
    }

    public void placeBlock(Location loc) {
        placedBlocks.add(loc);
    }

    public void removeBlock(Location loc) {
        placedBlocks.remove(loc);
    }

    public List<Location> getPlacedBlocks() {
        return placedBlocks;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("public", isPublic);
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Location getPlayerPriorLocation(Player pl) {
        return priorLocations.get(pl);
    }
}
