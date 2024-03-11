package dev.xdbl.commands.arena;

import dev.xdbl.Double;
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
import java.util.List;
import java.util.Objects;

public class CommandProfile implements CommandExecutor, TabCompleter {

    private final Double plugin;

    public CommandProfile(Double plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("xdbl.pvp")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid usage"));
            return true;
        }

        Player player;
        if (args.length == 1) {
            player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("<red>Invalid usage"))));
                return true;
            }
        } else {
            player = (Player) sender;
        }

        // Return the player's profile
        DoublePlayer doublePlayer = plugin.getPlayerManager().getDoublePlayer(player.getUniqueId());
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                """
                        <yellow><bold>Profile of %player%
                        <yellow><bold>ELO: <green>%elo%
                        <yellow><bold>Games: <green>%games%
                        <yellow><bold>Wins: <green>%wins%
                        <yellow><bold>Losses: <green>%losses%
                        <yellow><bold>PVP-Banned: <green>%pvpbanned%
                        <yellow><bold>Arena-Banned: <green>%arenabanned%
                        """
        ));

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        // Return a list of all players
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return players;
        }
        return new ArrayList<>();
    }


}
