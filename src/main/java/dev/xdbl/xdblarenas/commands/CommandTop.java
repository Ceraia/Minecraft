package dev.xdbl.xdblarenas.commands;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.players.ArenaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandTop implements CommandExecutor, TabCompleter {

    private final XDBLArena plugin;

    public CommandTop(XDBLArena plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("xdbl.pvp")) {
            sender.sendMessage(plugin.getConfig().getString("messages.no_permission").replace("&", "§"));
            return true;
        }

        Player p = (Player) sender;

        // Create and show a string list of the top 10 players with the highest elo
        List<String> top = new ArrayList<>();
        AtomicInteger i = new AtomicInteger();
        i.set(1);

        top.add(plugin.getConfig().getString("messages.scoreboard.top").replace("&", "§"));

        plugin.getPlayerManager().getArenaPlayers().stream().sorted(Comparator.comparingInt(ArenaPlayer::getElo).reversed()).limit(10).forEach(ap -> {
            String playerName = Bukkit.getOfflinePlayer(ap.getUUID()).getName();
            int elo = ap.getElo();

            String medal; // Default medal color for players outside the top 3

            // Check for 1st, 2nd, and 3rd place
            if (i.get() == 1) {
                medal = "§6"; // Gold for 1st place
            } else if (i.get() == 2) {
                medal = "§7"; // Silver for 2nd place
            } else if (i.get() == 3) {
                medal = "§#cd7f32"; // Bronze for 3rd place
            } else {
                medal = "§f"; // Default medal color for players outside the top 3
            }

            top.add(medal + i + " " + playerName + " §8- §7" + elo + " ELO (" + (ap.wins() + ap.losses()) + " games)");
            i.getAndIncrement();
        });

        // Send the top 10 players with the highest elo to the player
        top.forEach(p::sendMessage);


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new ArrayList<>();
    }
}
