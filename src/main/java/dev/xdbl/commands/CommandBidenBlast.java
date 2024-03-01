package dev.xdbl.commands;

import dev.xdbl.Double;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandBidenBlast implements CommandExecutor, TabCompleter, Listener {

    private final Double plugin;

    public CommandBidenBlast(Double plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("Only players can use this command."));

        }

        // If the player is holding a bow, set the display name to "BidenBlaster"
        Player p = (Player) sender;
        if (p.getInventory().getItemInMainHand().getType().name().contains("BOW")) {
            p.getInventory().getItemInMainHand().editMeta(meta ->
                        meta.displayName(MiniMessage.miniMessage().deserialize("BidenBlaster"))
                    );
            p.sendMessage(MiniMessage.miniMessage().deserialize("You have been given the BidenBlaster!"));
        } else {

            p.sendMessage(MiniMessage.miniMessage().deserialize("You must be holding a bow to use this command."));
        }return true;
    };

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args){
        return null;
    };

}
