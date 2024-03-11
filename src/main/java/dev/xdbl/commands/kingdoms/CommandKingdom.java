package dev.xdbl.commands.kingdoms;

import dev.xdbl.Double;
import dev.xdbl.types.Kingdom;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandKingdom implements CommandExecutor, TabCompleter {

    private final Double plugin;

    public CommandKingdom(Double plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(this.plugin.getCommand("kingdom")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        switch (args[0]) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage("You must specify a name for your kingdom");
                    return true;
                }

                // Combine all the arguments after the first one to get the name of the kingdom
                String kingdomName = String.join(" ", args).substring(7);


                // Check if the player is already in a kingdom
                if (plugin.getKingdomManager().getKingdom(player) != null) {
                    player.sendMessage("You are already in a kingdom");
                    return true;
                }

                // Check if the name of the kingdom is only alphanumeric + spaces
                if (!kingdomName.matches("^[a-zA-Z0-9 ]+$")) {
                    player.sendMessage("The name of the kingdom can only contain letters, numbers, and spaces");
                    return true;
                }

                // Check if the kingdom already exists
                if (plugin.getKingdomManager().getKingdom(kingdomName.toLowerCase().replace(" ", "_")) != null) {
                    player.sendMessage("The kingdom " + kingdomName + " already exists");
                    return true;
                }

                // Check if the kingdom name is none which is a reserved name
                if (kingdomName.equalsIgnoreCase("none")) {
                    player.sendMessage("The name of the kingdom cannot be 'none'");
                    return true;
                }

                Kingdom kingdom = plugin.getKingdomManager().newKingdom(args[1]);
                kingdom.addMember(player.getName());
                kingdom.setMemberRank(player.getName(), 4); // 4 is the highest rank
                kingdom.saveKingdom();

                // 4 - King
                // 3 - Duke
                // 2 - Baron
                // 1 - Citizen
                // 0 - Guest / Introductory

                player.sendMessage("You have created a new kingdom called " + args[1] + ".");
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage("You must specify a player to invite");
                    return true;
                }

                Player invited = Bukkit.getPlayer(args[1]);
                if (invited == null) {
                    player.sendMessage("The player " + args[1] + " is not online");
                    return true;
                }

                Kingdom kingdom = plugin.getKingdomManager().getKingdom(player);

                if (kingdom == null) {
                    player.sendMessage("You are not in a kingdom");
                    return true;
                }

                if (kingdom.getMemberRank(player.getName()) <= 2) {
                    player.sendMessage("You do not have permission to invite players to your kingdom, you need to be at least a Baron");
                    return true;
                }

                if (kingdom.invitePlayer(args[1])) {
                    player.sendMessage("You have invited " + args[1] + " to join your kingdom");
                    invited.sendMessage("You have been invited to join " + kingdom.getName() + ". Use /kingdom accept " + kingdom.getName() + " to join.");
                } else {
                    player.sendMessage(args[1] + " is already a member of your kingdom");
                }
            }
            case "accept" -> {
                if (args.length < 2) {
                    player.sendMessage("You must specify a kingdom to accept an invitation from");
                    return true;
                }

                Kingdom kingdom = plugin.getKingdomManager().getKingdom(args[1]);

                if (kingdom == null) {
                    player.sendMessage("The kingdom " + args[1] + " does not exist");
                    return true;
                }

                if (kingdom.acceptInvite(player)) {
                    player.sendMessage("You have joined " + kingdom.getName());
                } else {
                    player.sendMessage("You have not been invited to join " + kingdom.getName());
                }
            }
            case "leave" -> {
                Kingdom kingdom = plugin.getKingdomManager().getKingdom(player);

                if (kingdom == null) {
                    player.sendMessage("You are not in a kingdom");
                    return true;
                }

                kingdom.removeMember(player.getName());
                player.sendMessage("You have left " + kingdom.getName());
            }
            case "info" -> {
                if (args.length < 2) {
                    Kingdom kingdom = plugin.getKingdomManager().getKingdom(player);
                    if (kingdom == null) {
                        player.sendMessage("You are not in a kingdom");
                        return true;
                    }
                    player.sendMessage("Name: " + kingdom.getName());
                    player.sendMessage("Members: " + String.join(", ", kingdom.getMembers()));
                } else {
                    Kingdom kingdom = plugin.getKingdomManager().getKingdom(args[1]);
                    if (kingdom == null) {
                        player.sendMessage("The kingdom " + args[1] + " does not exist");
                        return true;
                    }
                    player.sendMessage("Name: " + kingdom.getName());
                    player.sendMessage("Members: " + String.join(", ", kingdom.getMembers()));
                }
            }
            case "list" -> {
                List<Kingdom> kingdoms = plugin.getKingdomManager().getKingdoms();
                player.sendMessage("Kingdoms:");
                for (Kingdom kingdom : kingdoms) {
                    player.sendMessage(kingdom.getName());
                }
            }
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage("You must specify a player to kick");
                    return true;
                }

                Kingdom kingdom = plugin.getKingdomManager().getKingdom(player);

                if (kingdom == null) {
                    player.sendMessage("You are not in a kingdom");
                    return true;
                }

                if (kingdom.getMemberRank(player.getName()) <= 2) {
                    player.sendMessage("You do not have permission to kick players from your kingdom, you need to be at least a Baron");
                    return true;
                }

                if (kingdom.getMemberRank(args[1]) >= kingdom.getMemberRank(player.getName())) {
                    player.sendMessage("You cannot kick a member with an equal or higher rank than you");
                    return true;
                }

                if (kingdom.removeMember(args[1])) {
                    player.sendMessage("You have kicked " + args[1] + " from your kingdom");
                } else {
                    player.sendMessage(args[1] + " is not a member of your kingdom");
                }
            }
            case "promote" -> {
                if (args.length < 2) {
                    player.sendMessage("You must specify a player to promote");
                    return true;
                }

                Kingdom kingdom = plugin.getKingdomManager().getKingdom(player);

                if (kingdom == null) {
                    player.sendMessage("You are not in a kingdom");
                    return true;
                }

                if (kingdom.getMemberRank(player.getName()) <= 3) {
                    player.sendMessage("You do not have permission to promote players in your kingdom, you need to be at least a Duke");
                    return true;
                }

                if (kingdom.getMemberRank(args[1]) >= kingdom.getMemberRank(player.getName())) {
                    player.sendMessage("You cannot promote a member with an equal or higher rank than you");
                    return true;
                }

                int newRank = kingdom.promoteMember(args[1]);
                if (newRank == 5) {
                    player.sendMessage(args[1] + " is not a member of your kingdom");
                } else {
                    player.sendMessage(args[1] + " has been promoted to rank " + newRank);
                }
            }
            case "demote" -> {
                if (args.length < 2) {
                    player.sendMessage("You must specify a player to demote");
                    return true;
                }

                Kingdom kingdom = plugin.getKingdomManager().getKingdom(player);

                if (kingdom == null) {
                    player.sendMessage("You are not in a kingdom");
                    return true;
                }

                if (kingdom.getMemberRank(player.getName()) <= 3) {
                    player.sendMessage("You do not have permission to demote players in your kingdom, you need to be at least a Duke");
                    return true;
                }

                if (kingdom.getMemberRank(args[1]) >= kingdom.getMemberRank(player.getName())) {
                    player.sendMessage("You cannot demote a member with an equal or higher rank than you");
                    return true;
                }

                int newRank = kingdom.demoteMember(args[1]);
                if (newRank == 5) {
                    player.sendMessage(args[1] + " is not a member of your kingdom");
                } else {
                    player.sendMessage(args[1] + " has been demoted to rank " + newRank);
                }
            }
        }


        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> tabOptions = new ArrayList<>();

///kingdom create [kingdom name]: Create a new kingdom with the specified name.
///kingdom invite [player]: Invite a player to join your kingdom.
///kingdom accept [name]: Accept an invitation to join a kingdom.
///kingdom leave: Leave your current kingdom.
///kingdom info [name]: Display information about a specific kingdom.
///kingdom list: List all kingdoms on the server.
///kingdom kick [player]: Kick a member from your kingdom.
//
///kingdom promote [player]: Promote a member to a higher rank within your kingdom.
///kingdom demote [player]: Demote a member to a lower rank within your kingdom.
//
///kingdom ally [kingdom name]: Form an alliance with another kingdom.
///kingdom allyaccept [kingdom name]: Accept an alliance request from another kingdom.
///kingdom allydeny [kingdom name]: Deny an alliance request from another kingdom.
//
///kingdom enemy [name]: Declare another kingdom as an enemy.
//
///kingdom war [kingdom name]: Initiate a war with another kingdom.
///kingdom warlist: View a list of ongoing wars and their status.
///kingdom siege [kingdom name]: Begins a Siege on another Kingdom
///kingdom surrender [kingdom name]: Surrender to a kingdom you are at war with.
//
///kingdom claim: Claim land for your kingdom.
///kingdom unclaim: Unclaim land previously claimed by your kingdom.
//
///kingdom map: View a map showing claimed territory of different kingdoms.
///kingdom home: Teleport to your kingdom's home location.
///kingdom sethome: Set the home location for your kingdom.
//
///kingdom chat [message]: Send a message to members of your kingdom.
//
///kingdom deposit [amount]: Deposit resources or currency into your kingdom's treasury.
///kingdom withdraw [amount]: Withdraw resources or currency from your kingdom's treasury.
//
///kingdom setbanner [hold the banner in your hand]: Set a custom banner for your kingdom.
//
///kingdom help: Display a list of available commands and their descriptions.
            tabOptions.add("create");
            tabOptions.add("invite");
            tabOptions.add("accept");
            tabOptions.add("leave");
            tabOptions.add("info");
            tabOptions.add("list");
            tabOptions.add("kick");
            tabOptions.add("promote");
            tabOptions.add("demote");
            tabOptions.add("ally");
            tabOptions.add("allyaccept");
            tabOptions.add("allydeny");
            tabOptions.add("enemy");
            tabOptions.add("war");
            tabOptions.add("warlist");
            tabOptions.add("siege");
            tabOptions.add("surrender");
            tabOptions.add("claim");
            tabOptions.add("unclaim");
            tabOptions.add("map");
            tabOptions.add("home");
            tabOptions.add("sethome");
            tabOptions.add("chat");
            tabOptions.add("deposit");
            tabOptions.add("withdraw");
            tabOptions.add("setbanner");
            tabOptions.add("help");

            return tabOptions;
        }
        if (args.length == 2) {
            List<String> tabOptions = new ArrayList<>();

            if (
                    Objects.equals(args[0], "invite") ||
                            Objects.equals(args[0], "kick") ||
                            Objects.equals(args[0], "promote") ||
                            Objects.equals(args[0], "demote") ||
                            Objects.equals(args[0], "info")
            ) {
                // Add all online players to the tab options
                for (Player player : Bukkit.getOnlinePlayers()) {
                    tabOptions.add(player.getName());
                }
            } else if (Objects.equals(args[0], "accept")) {
                Player player = (Player) sender;
                // Send all the kingdoms that have invited the player to the tab options
                this.plugin.getKingdomManager().getInvites(player).forEach(kingdom -> tabOptions.add(kingdom.getName()));
            }

            return tabOptions;
        }

        // If there is more than one argument, return an empty list
        return new ArrayList<>();
    }
}
