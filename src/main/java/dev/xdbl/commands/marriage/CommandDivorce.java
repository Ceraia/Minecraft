package dev.xdbl.commands.marriage;

import dev.xdbl.Double;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandDivorce implements CommandExecutor, TabCompleter {

    private final Double plugin;

    public CommandDivorce(Double plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: <white>/divorce <player>"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found"));
            return true;
        }

        plugin.getMarriageManager().divorce(player);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return new ArrayList<>(Collections.singleton(plugin.getPlayerManager().getPlayer(sender.getName()).getMarriedName()));
    }
}
