package dev.xdbl.commands.factions;

import dev.xdbl.Double;
import dev.xdbl.types.DoublePlayer;
import dev.xdbl.types.Faction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandFaction implements CommandExecutor, TabCompleter {

    private final Double plugin;

    public CommandFaction(Double plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        switch (args[1]){
            case "add" -> {
                switch (args[2]){
                    case "faction" -> {
                        Faction faction = plugin.getFactionManager().newFaction(args[3]);
                        faction.saveFaction();
                    }
                    case "member" -> {

                    }
                }
            }
            case "remove" -> {
                switch (args[2]){
                    case "faction" -> {
                        plugin.getLogger().info("faction removal not supported");
                    }
                    case "member" -> {
                        plugin.getLogger().info("member removal not supported");
                    }
                }
            }
        }


        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> tabOptions = new ArrayList<>();

            tabOptions.add("add");
            tabOptions.add("remove");

            return tabOptions;
        }
        if (args.length == 2) {
            List<String> tabOptions = new ArrayList<>();

            tabOptions.add("member");
            tabOptions.add("faction");

            return tabOptions;
        }
        if (args.length == 3) {
            List<String> tabOptions = new ArrayList<>();

            if(Objects.equals(args[2], "member"))
                for (Player player : Bukkit.getOnlinePlayers()) {
                    tabOptions.add(player.getName());
                }

            if(Objects.equals(args[2], "faction"))
                for (Faction faction : plugin.getFactionManager().getFactions()) {
                    tabOptions.add(faction.getName());
                }

            return tabOptions;
        }
        // If there is more than one argument, return an empty list
        return new ArrayList<>();
    }
}
