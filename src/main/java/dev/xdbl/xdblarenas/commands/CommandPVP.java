package dev.xdbl.xdblarenas.commands;

import dev.xdbl.xdblarenas.InviteManager;
import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.arenas.Arena;
import dev.xdbl.xdblarenas.arenas.ArenaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandPVP implements CommandExecutor, TabCompleter {

    private final XDBLArena plugin;

    public CommandPVP(XDBLArena plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!sender.hasPermission("xdbl.pvp")){
            sender.sendMessage(plugin.getConfig().getString("messages.no_permission").replace("&", "§"));
            return true;
        }

        if (args.length == 0) {
            pvpHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            Player p = (Player) sender;

            InviteManager.Invite invite = plugin.getInviteManager().invites.get(p);

            if (invite == null) {
                sender.sendMessage(
                        plugin.getConfig().getString("messages.invite_accept.invite_not_found").replace("&", "§")
                );
                plugin.getInviteManager().invites.remove(p);
                return true;
            }

            if (!invite.invited.isOnline() || !invite.inviter.isOnline()) {
                sender.sendMessage(
                        plugin.getConfig().getString("messages.invite_accept.other_player_offline").replace("&", "§")
                );
                plugin.getInviteManager().invites.remove(p);
                return true;
            }

            if (!invite.arena.isReady() || invite.arena.getState() != Arena.ArenaState.WAITING || plugin.getArenaManager().getArenas().stream().noneMatch(
                    a -> a.getName().equalsIgnoreCase(invite.arena.getName())
            )) {
                for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                    pl.sendMessage(
                            plugin.getConfig().getString("messages.invite_accept.arena_not_ready").replace("&", "§")
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
                                plugin.getConfig().getString("messages.invite_accept.group.group_not_found").replace("&", "§")
                        );
                    }
                    plugin.getInviteManager().invites.remove(p);
                    return true;
                }

                if (group1.size() < 2 || group2.size() < 2) {
                    for (Player pl : Arrays.asList(invite.invited, invite.inviter)) {
                        pl.sendMessage(
                                plugin.getConfig().getString("messages.invite_accept.group.group_too_small").replace("&", "§")
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
                                plugin.getConfig().getString("messages.invite_accept.group.someone_already_in_fight").replace("&", "§")
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

            invite.arena.start(invite, playersToFight);

            return true;
        }

        if (args[0].equalsIgnoreCase("scoreboard")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(
                        plugin.getConfig().getString("messages.only_players").replace("&", "§")
                );
                return true;
            }

            Player p = (Player) sender;

            ArenaPlayer arenaPlayer = plugin.getPlayerManager().getArenaPlayer(p.getUniqueId());

            if(arenaPlayer.toggleScoreboard()){
                p.sendMessage(
                        plugin.getConfig().getString("messages.pvp.scoreboard.enabled").replace("&", "§")
                );
            }else{
                p.sendMessage(
                        plugin.getConfig().getString("messages.pvp.scoreboard.disabled").replace("&", "§")
                );
            }
        }
        // open gui for invite player

        String playerName = args[0];

        // reload for op
        if (playerName.equalsIgnoreCase("reload") && sender.isOp()) {
            plugin.reloadConfig();
            plugin.getArenaSelectGUI().reloadConfig();
            sender.sendMessage(
                    "§aReloaded!"
            );
            return true;
        }

        Player invited = Bukkit.getPlayer(playerName);

        if (invited == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.pvp.invite.player_offline").replace("&", "§")
            );
            return true;
        }

        Player inviter = (Player) sender;

        if (inviter == invited) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.pvp.invite.invite_self").replace("&", "§")
            );
            return true;
        }

        plugin.getArenaSelectGUI().openGUI(inviter);

        InviteManager.Invite invite = new InviteManager.Invite(inviter, invited);

        plugin.getInviteManager().selectingInvites.put(inviter, invite);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> tabOptions = new ArrayList<>();
            // If there is an argument, suggest online player names
            tabOptions.add("accept");
            tabOptions.add("scoreboard");
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
        plugin.getConfig().getStringList("messages.pvp.help").forEach(s -> {
            sender.sendMessage(s.replace("&", "§"));
        });
    }
}
