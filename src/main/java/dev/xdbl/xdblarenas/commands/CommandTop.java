package dev.xdbl.xdblarenas.commands;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.players.ArenaPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandTop implements CommandExecutor, TabCompleter {

    private final XDBLArena plugin;

    public CommandTop(XDBLArena plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("xdbl.pvp")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.no_permission"))));
            return true;
        }

        Player p = (Player) sender;

        // Create and show a string list of the top 10 players with the highest elo
        List<Component> top = new ArrayList<>();
        AtomicInteger i = new AtomicInteger();
        i.set(1);

        top.add(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.scoreboard.top"))));

        plugin.getPlayerManager().getArenaPlayers().stream().sorted(Comparator.comparingInt(ArenaPlayer::getElo).reversed()).limit(10).forEach(ap -> {
            String playerName = Bukkit.getOfflinePlayer(ap.getUUID()).getName();
            int elo = ap.getElo();

            String medal; // Default medal color for players outside the top 3

            // Check for 1st, 2nd, and 3rd place
            if (i.get() == 1) {
                medal = "<gold>"; // Gold for 1st place
            } else if (i.get() == 2) {
                medal = "<#C0C0C0>"; // Silver for 2nd place
            } else if (i.get() == 3) {
                medal = "<#cd7f32>"; // Bronze for 3rd place
            } else {
                medal = "<white>"; // Default medal color for players outside the top 3
            }

            top.add(MiniMessage.miniMessage().deserialize(medal + i + " " + playerName + " <dark_gray>- <gray>" + elo + " ELO (" + (ap.wins() + ap.losses()) + " games)"));
            i.getAndIncrement();
        });

        // Send the top 10 players with the highest elo to the player
        top.forEach(p::sendMessage);


        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return new ArrayList<>();
    }
}
