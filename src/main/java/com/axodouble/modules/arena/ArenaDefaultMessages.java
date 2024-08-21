package com.axodouble.modules.arena;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public class ArenaDefaultMessages {
    public static void arenaHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                """
                                < yellow ><bold > Arena Help
                                <green>/arena list -Show your arenas
                                <green >/arena delete <name > -Delete your arena
                                <green >/arena public <name > -Make arena public/private
                                <yellow > How to create a new arena:
                                <gold > 1. < green >/arena create <name > -Create a new arena
                                <gold> 2. < green >/arena sp1 <name > -Set spawn point for the first player
                                <gold> 3. < green >/arena sp2 <name > -Set spawn point for the second player
                        """
        ));
    }

    public  static void notYours(CommandSender sender) {
        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<red>Arena is not yours.")
        );
    }

    public static void notFound(CommandSender sender) {
        sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<red>Arena not found.")
        );
    }

    public static  void pvpHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("""
                <gray> Usage: /pvp<player>
                <gray> Usage: /pvp accept
                <gray> Usage: /pvp reload
                """));
    }
}
