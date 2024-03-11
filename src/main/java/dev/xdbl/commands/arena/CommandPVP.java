package dev.xdbl.commands.arena;

import dev.xdbl.Double;
import dev.xdbl.managers.InviteManager;
import dev.xdbl.types.Arena;
import dev.xdbl.types.DoublePlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CommandPVP implements CommandExecutor, TabCompleter {

    private final Double plugin;

    public CommandPVP(Double plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("pvp")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("xdbl.pvp")) {
            this.plugin.noPermission((Player) sender);
            return true;
        }

        if (args.length == 0) {
            pvpHelp(sender);
            return true;
        }

        // Check if the sender is pvpbanned
        DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(((Player) sender).getUniqueId());
        if (doublePlayer.pvpBanned()) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>You are banned from PvP!")
            );
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            Player p = (Player) sender;

            // Check if the player is banned
            if (plugin.getPlayerManager().getDoublePlayer(p.getUniqueId()).pvpBanned()) {
                sender.sendMessage(
                        MiniMessage.miniMessage().deserialize("<red>This player is banned from PvP!")
                );
                return true;
            }

            InviteManager.Invite invite = plugin.getInviteManager().invites.get(p);

            if (invite == null) {
                sender.sendMessage(
                        MiniMessage.miniMessage().deserialize("<red>You don't have any invites!")
                );
                plugin.getInviteManager().invites.remove(p);
                return true;
            }

            if (!invite.invited.isOnline() || !invite.inviter.isOnline()) {
                sender.sendMessage(
                        MiniMessage.miniMessage().deserialize("<red>Player not found!")
                );
                return true;
            }

            if (invite.accepted) {
                sender.sendMessage(
                        MiniMessage.miniMessage().deserialize("<red>You already accepted this invite!")
                );
                return true;
            }

            if (invite.arena.getState() != Arena.ArenaState.WAITING || plugin.getArenaManager().getArenas().stream().noneMatch(
                    a -> a.getName().equalsIgnoreCase(invite.arena.getName())
            )) {
                for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                    pl.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Arena not found!")
                    );
                }
                plugin.getInviteManager().invites.remove(p);
                return true;
            }

            List<Player> playersToFight = new ArrayList<>();

            if (invite.group) {
                List<Player> group1 = plugin.getGroupManager().getPlayersByGroup(invite.inviter);
                List<Player> group2 = plugin.getGroupManager().getPlayersByGroup(invite.invited);

                if (group1 == null || group2 == null) {
                    for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                        pl.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>Group not found!")
                        );
                    }
                    plugin.getInviteManager().invites.remove(p);
                    return true;
                }

                if (group1.size() < 2 || group2.size() < 2) {
                    for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                        pl.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>Group must have at least 2 players!")
                        );
                    }
                    plugin.getInviteManager().invites.remove(p);
                    return true;
                }

                boolean allPlayersAreReady = true;

                for (Player pl : group1) {
                    if (plugin.getArenaManager().getArena(pl) != null) {
                        return true;
                    }
                }

                for (Player pl : group2) {
                    if (plugin.getArenaManager().getArena(pl) != null) {
                        return true;
                    }
                }

                if (!allPlayersAreReady) {
                    for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                        pl.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>One or more players are already in an arena!")
                        );
                    }
                    plugin.getInviteManager().invites.remove(p);
                    return true;
                }

                playersToFight.addAll(group1);
                playersToFight.addAll(group2);

                group1.forEach(pl -> invite.arena.addPlayer(pl, 1));
                group2.forEach(pl -> invite.arena.addPlayer(pl, 2));
            } else {
                playersToFight.add(invite.invited);
                playersToFight.add(invite.inviter);
                invite.arena.addPlayer(invite.invited, 1);
                invite.arena.addPlayer(invite.inviter, 2);
            }

            // Starting arena
            invite.accepted = true;
            invite.arena.start(invite, playersToFight);

            return true;
        }

        // open gui for invite player

        String playerName = args[0];

        // reload for op
        if (playerName.equalsIgnoreCase("reload") && sender.isOp()) {
            plugin.reloadConfig();
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Reloaded!")
            );
            return true;
        } // If the player is reload and the sender is op, reload the config

        Player invited = Bukkit.getPlayer(playerName);

        if (invited == null) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Player not found!")
            );
            return true;
        } // If the player is offline, return

        Player inviter = (Player) sender;

        if (inviter == invited) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>You can't invite yourself!")
            );
            return true;
        } // If the inviter is the same as the invited, return

        plugin.getArenaSelectGUI().openArenaList(inviter, invited);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> tabOptions = new ArrayList<>();
            // If there is an argument, suggest online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Exclude the sender's name from the suggestions
                if (!player.getName().equals(sender.getName())) {
                    tabOptions.add(player.getName());
                }
            }

            return tabOptions;
        }
        // If there is more than one argument, return an empty list
        return new ArrayList<>();
    }

    private void pvpHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("""
                <gray>Usage: /pvp <player>
                <gray>Usage: /pvp accept
                <gray>Usage: /pvp reload
                """));
    }
}
