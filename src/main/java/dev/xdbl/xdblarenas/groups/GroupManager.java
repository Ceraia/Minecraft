package dev.xdbl.xdblarenas.groups;

import dev.xdbl.xdblarenas.InviteManager;
import dev.xdbl.xdblarenas.XDBLArena;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupManager implements CommandExecutor, Listener {

    private final XDBLArena plugin;
    private final Map<Player, List<Player>> groups = new HashMap<>();
    private final Map<Player, Player> playersByGroup = new HashMap<>();

    private final Map<Player, Player> invites = new HashMap<>();

    public GroupManager(XDBLArena plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.getConfig().getStringList("messages.gvg.help").forEach(s -> {
                sender.sendMessage(s.replace("&", "§"));
            });
            return true;
        }

        Player player = (Player) sender;

        if (args[0].equalsIgnoreCase("invite")) {
            if (playersByGroup.containsKey(player) && !groups.containsKey(player)) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.dont_have_group").replace("&", "§"));
                return true;
            }

            if (args.length == 1) {
                badUsage(sender);
                return true;
            }

            List<Player> targets = new ArrayList<>();
            List<String> notOnline = new ArrayList<>();

            for (int i = 1; i < args.length; i++) {
                Player target = Bukkit.getPlayer(args[i]);
                if (target == null) {
                    notOnline.add(args[i]);
                } else {
                    targets.add(target);
                }

                if (playersByGroup.containsKey(target)) {
                    sender.sendMessage(plugin.getConfig().getString("messages.gvg.invite.already_in_group")
                            .replace("%player%", target.getName())
                            .replace("&", "§"));
                    return true;
                }
            }

            if (targets.contains(player)) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.invite.cant_invite_yourself").replace("&", "§"));
                return true;
            }

            if (notOnline.size() > 0) {
                System.out.println(notOnline);
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.player_offline")
                        .replace("%player%", String.join(", ", notOnline))
                        .replace("&", "§"));
                return true;
            }

            if (notOnline.contains(player.getName())) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.invite.invite_yourself").replace("&", "§"));
                return true;
            }

            for (Player target : targets) {
                String invite_message = plugin.getConfig().getString("messages.gvg.invite.invite_message")
                        .replace("%inviter%", player.getName())
                        .replace("&", "§");
                String[] split = invite_message.split("@");
                // get between two @
                TextComponent message = new TextComponent(split[0]);

                TextComponent clickableMessage = new TextComponent(split[1]);
                clickableMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gvg accept"));
                clickableMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClick to accept").create()));
                message.addExtra(clickableMessage);
                message.addExtra(split[2]);
                target.spigot().sendMessage(message);

                invites.put(target, player);
            }

            sender.sendMessage(plugin.getConfig().getString("messages.gvg.invite.invite_sent")
                    .replace("%player%", targets.stream().map(Player::getName).collect(Collectors.joining(", ")))
                    .replace("&", "§"));
            return true;
        } else if (args[0].equalsIgnoreCase("accept")) {
            Player inviter = invites.get(player);

            if (inviter == null || !inviter.isOnline()) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.accept.invite_not_found").replace("&", "§"));
                return true;
            }

            if (playersByGroup.containsKey(inviter) && !groups.containsKey(inviter)) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.accept.invite_not_found").replace("&", "§"));
                return true;
            }

            if (playersByGroup.containsKey(player)) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.accept.you_already_in_group").replace("&", "§"));
                return true;
            }

            List<Player> group = groups.get(inviter);
            if (group == null) {
                group = new ArrayList<>();
                group.add(inviter);
                playersByGroup.put(inviter, inviter);
            }
            group.add(player);
            groups.put(inviter, group);

            playersByGroup.put(player, inviter);
            sender.sendMessage(plugin.getConfig().getString("messages.gvg.accept.accepted").replace("&", "§"));
            for (Player pl : group) {
                pl.sendMessage(plugin.getConfig().getString("messages.gvg.accept.invite_accepted")
                        .replace("%player%", player.getName())
                        .replace("&", "§"));
            }
            return true;
        } else if (args[0].equalsIgnoreCase("leave")) {
            leaveGang(player);
        } else if (args[0].equalsIgnoreCase("kick")) {
            if (!groups.containsKey(player)) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.kick.not_in_group").replace("&", "§"));
                return true;
            }

            if (args.length == 1) {
                badUsage(sender);
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.player_offline")
                        .replace("%player%", args[1])
                        .replace("&", "§"));
                return true;
            }

            List<Player> group = groups.get(player);

            if (!group.contains(target)) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.kick.player_not_in_group").replace("&", "§"));
                return true;
            }

            group.remove(target);
            playersByGroup.remove(target);

            target.sendMessage(plugin.getConfig().getString("messages.gvg.kick.you_kicked").replace("&", "§"));

            if (group.size() <= 1) {

                groups.get(player).forEach(p -> {
                    player.sendMessage(plugin.getConfig().getString("messages.gvg.you_left").replace("&", "§"));
                    playersByGroup.remove(p);
                    groups.remove(player);
                });

                groups.remove(player);
            } else {
                group.forEach(p -> {
                    p.sendMessage(plugin.getConfig().getString("messages.gvg.kicked")
                            .replace("%player%", target.getName())
                            .replace("&", "§"));
                });
            }
            return true;
        } else {
            String playerName = args[0];
            Player invited = Bukkit.getPlayer(playerName);

            if (invited == null) {
                sender.sendMessage(
                        plugin.getConfig().getString("messages.gvg.player_offline")
                                .replace("%player%", playerName)
                                .replace("&", "§")
                );
                return true;
            }

            if (invited == player) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.invite.invite_yourself").replace("&", "§"));
                return true;
            }

            if (!groups.containsKey(player)) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.not_in_group").replace("&", "§"));
                return true;
            }


            if (!groups.containsKey(invited) || (groups.get(player).contains(invited) || groups.get(invited).contains(player))) {
                sender.sendMessage(plugin.getConfig().getString("messages.gvg.player_not_in_group").replace("&", "§"));
                return true;
            }

            plugin.getArenaSelectGUI().openGUI(player);

            InviteManager.Invite invite = new InviteManager.Invite(player, invited);
            invite.group = true;

            plugin.getInviteManager().selectingInvites.put(player, invite);

        }

        return true;
    }

    private void leaveGang(Player player) {
        if (!playersByGroup.containsKey(player)) {
            player.sendMessage(plugin.getConfig().getString("messages.gvg.not_in_group").replace("&", "§"));
            return;
        }

        Player owner = playersByGroup.get(player);
        List<Player> group = groups.get(owner);

        if (group != null || group.size() <= 2) {
            group.forEach(pl -> {
                pl.sendMessage(plugin.getConfig().getString("messages.gvg.you_left").replace("&", "§"));
                playersByGroup.remove(pl);
            });

            groups.remove(owner);
        } else {
            group.forEach(p -> {
                player.sendMessage(plugin.getConfig().getString("messages.gvg.leave.player_left_group")
                        .replace("%player%", player.getName())
                        .replace("&", "§"));
            });

            group.remove(player);
            groups.put(owner, group);

            playersByGroup.remove(player);
        }
        return;
    }

    private void badUsage(CommandSender sender) {
        sender.sendMessage(plugin.getConfig().getString("messages.bad_usage").replace("&", "§"));
    }

    public List<Player> getPlayersByGroup(Player player) {
        return groups.get(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (playersByGroup.containsKey(player)) {
            leaveGang(player);
        }
    }
}
