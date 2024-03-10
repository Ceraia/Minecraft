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
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        switch (args[0]) {
            case "add" -> {
                switch (args[1]) {
                    case "kingdom" -> {
                        plugin.getLogger().info("kingdom addition called");
                        Kingdom kingdom = plugin.getKingdomManager().newKingdom(args[2]);
                        kingdom.addMember(player.getName());
                        kingdom.saveKingdom();
                    }
                    case "member" -> {
                        plugin.getLogger().info("member addition called");
                    }
                }
            }
            case "remove" -> {
                switch (args[1]) {
                    case "kingdom" -> {
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
            tabOptions.add("kingdom");

            return tabOptions;
        }
        if (args.length == 3) {
            List<String> tabOptions = new ArrayList<>();

            if (Objects.equals(args[2], "member")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    tabOptions.add(player.getName());
                }
            }

            if (Objects.equals(args[2], "kingdom")) {
                for (Kingdom kingdom : plugin.getKingdomManager().getKingdoms()) {
                    tabOptions.add(kingdom.getName());
                }
            }

            return tabOptions;
        }
        // If there is more than one argument, return an empty list
        return new ArrayList<>();
    }
}
