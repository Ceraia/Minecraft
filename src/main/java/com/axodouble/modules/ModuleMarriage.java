package com.axodouble.modules;

import com.axodouble.Double;
import com.axodouble.types.DoublePlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ModuleMarriage implements CommandExecutor, TabCompleter, Listener {

    private final Double plugin;

    private final Map<Player, Player> invites = new HashMap<>();

    public ModuleMarriage(Double plugin) {
        this.plugin = plugin;

        Objects.requireNonNull(plugin.getCommand("marry")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("divorce")).setExecutor(this);

        Objects.requireNonNull(plugin.getCommand("marry")).setTabCompleter(this);
        Objects.requireNonNull(plugin.getCommand("divorce")).setTabCompleter(this);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (!sender.hasPermission("double.marry")) {
            this.plugin.noPermission((Player) sender);
            return true;
        }

        switch (cmd.getName()) {
            case "divorce" ->
                plugin.getModuleMarriage().divorce(player);
            case "marry" -> {
                if (args.length == 0) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: <white>/marry <player>"));
                    return true;
                }

                Player target = plugin.getServer().getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found"));
                    return true;
                }

                plugin.getModuleMarriage().invite(player, target);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        // Return all online players except the sender
        List<String> tabOptions = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.getName().equals(sender.getName())) {
                tabOptions.add(player.getName());
            }
        }

        List<String> returnedOptions = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length - 1], tabOptions, returnedOptions);

        return returnedOptions;
    }

    public void invite(Player sender, Player target) {
        if (this.plugin.getPlayerManager().getDoublePlayer(sender.getUniqueId()).isMarried()) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<red>You are already married!"
                    )
            );
            return;
        }
        if (this.plugin.getPlayerManager().getDoublePlayer(target.getUniqueId()).isMarried()) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<red>" + target.getName() + " is already married!"
                    )
            );
            return;
        }

        if(this.invites.containsKey(sender)) {
            // Get the key and check if the value is the same as the target
            if (this.invites.get(sender).equals(target)) {
                accept(sender, target);
                return;
            }
        }

        this.invites.put(target, sender);
        this.plugin.getServer().sendMessage(
                MiniMessage.miniMessage().deserialize(
                        "<green>"+sender.getName()+"<gray> has invited <green>" + target.getName() + "<gray> to marry them!"
                )
        );
        target.sendMessage(
                MiniMessage.miniMessage().deserialize(
                        "<green>" + sender.getName() + "<gray> has invited you to marry them! Click <hover:show_text:'Click to accept the marriage proposal.'><click:run_command:/marry "+sender.getName()+">[<green>here<gray>]</click><gray> to accept."
                )
        );
    }

    public void accept(Player target, Player sender) {
        if (this.plugin.getPlayerManager().getDoublePlayer(sender.getUniqueId()).isMarried()) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<red>You are already married!"
                    )
            );
            target.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<red>" + sender.getName() + " is already married!"
                    )
            );
            return;
        }
        if (this.plugin.getPlayerManager().getDoublePlayer(target.getUniqueId()).isMarried()) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<red>" + target.getName() + " is already married!"
                    )
            );
            target.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<red>You are already married!"
                    )
            );
            return;
        }
        if (!this.invites.containsKey(target) || !this.invites.get(target).equals(sender)) {
            return;
        }

        this.plugin.getServer().sendMessage(
                MiniMessage.miniMessage().deserialize(
                        "<green>" + target.getName() + "<gray> has accepted <green>" + sender.getName() + "<gray>'s marriage proposal!"
                )
        );
        DoublePlayer doubleTarget = this.plugin.getPlayerManager().getDoublePlayer(target.getUniqueId());
        DoublePlayer doubleSender = this.plugin.getPlayerManager().getDoublePlayer(sender.getUniqueId());
        doubleTarget.marry(doubleSender.getName());
        doubleSender.marry(doubleTarget.getName());
        this.invites.remove(target);
    }

    public int decline(Player target, Player sender) {
        if (this.plugin.getPlayerManager().getDoublePlayer(sender.getUniqueId()).isMarried()) {
            return 1;
        }
        if (this.plugin.getPlayerManager().getDoublePlayer(target.getUniqueId()).isMarried()) {
            return 2;
        }
        if (!this.invites.containsKey(target) || !this.invites.get(target).equals(sender)) {
            return 3;
        }

        this.plugin.getServer().sendMessage(
                MiniMessage.miniMessage().deserialize(
                        "<green>" + target.getName() + "<gray> has declined <green>" + sender.getName() + "<gray>'s marriage proposal!"
                )
        );
        this.invites.remove(target);
        return 4;
    }

    public void divorce(Player player) {
        DoublePlayer doublePlayer = this.plugin.getPlayerManager().getDoublePlayer(player.getUniqueId());
        DoublePlayer doublePartner = this.plugin.getPlayerManager().getDoublePlayer(doublePlayer.getPartner());
        doublePlayer.divorce();
        doublePartner.divorce();

        this.plugin.getServer().sendMessage(
                MiniMessage.miniMessage().deserialize(
                        "<green>" + player.getName() + " has divorced " + doublePartner.getName() + "."
                )
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.invites.remove(player);
    }

    // Kiss manager, if the player right clicks another player, and they are married, they will kiss
    // If the player right clicks another player, and they are not married, they will not kiss
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player player) {
            if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                return;
            }
            if (!event.getPlayer().isSneaking()) {
                return;
            }

            DoublePlayer doublePlayer = this.plugin.getPlayerManager().getDoublePlayer(event.getPlayer().getUniqueId());
            if (doublePlayer.isMarried()) {
                if (Objects.requireNonNull(((Player) event.getRightClicked()).getPlayer()).getName().equals(doublePlayer.getMarriedName())) {
                    // Spawn a bunch of hearts
                    spawnHeartsAroundPlayer(player);
                    spawnHeartsAroundPlayer(event.getPlayer());
                }
            }
        }
    }

    // Method to spawn hearts around a player
    private void spawnHeartsAroundPlayer(Player player) {
        World world = player.getWorld();
        Location playerLocation = player.getLocation();

        int heartsToSpawn = (int) (Math.random() * 3) + 2;
        for (int i = 0; i < heartsToSpawn; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 0.5; // Adjust this value as needed
            double x = playerLocation.getX() + Math.cos(angle) * radius;
            double y = playerLocation.getY() + (float) (Math.random() * 0.3) + 1.5;
            double z = playerLocation.getZ() + Math.sin(angle) * radius;

            Location particleLocation = new Location(world, x, y, z);
            world.spawnParticle(Particle.HEART, particleLocation, 1);
        }
    }

    public Collection<String> getRequests(Player player) {
        // Get the requests for the player
        Collection<String> requests = new ArrayList<>();
        for (Player target : this.invites.keySet()) {
            if (this.invites.get(target).equals(player)) {
                requests.add(target.getName());
            }
        }
        return requests;
    }
}
