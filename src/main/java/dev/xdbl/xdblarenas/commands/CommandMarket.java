package dev.xdbl.xdblarenas.commands;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.managers.MarketItemManager;
import dev.xdbl.xdblarenas.types.MarketItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CommandMarket implements CommandExecutor, TabCompleter {

    private final XDBLArena plugin;

    public CommandMarket(XDBLArena plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("xdbl.market")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.no_permission"))));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getMarketGUI().openMarketGUI(player);
            return true;
        }

        if(Objects.equals(args[0], "sell")) {

            if (args.length != 2) {
                marketHelp(sender);
                return true;
            }

            if (!args[1].matches("[0-9]+")) {
                marketHelp(sender);
                return true;
            }
            int price = Integer.parseInt(args[1]);
            if (price < 0) {
                marketHelp(sender);
                return true;
            }

            if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType().isAir()){
                sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.market.create.no_item"))));
                return true;
            }


            if(plugin.getMarketItemManager().createItem(player, player.getInventory().getItemInMainHand(), Integer.parseInt(args[1]))){
                sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.market.create.success"))));
                player.getInventory().setItemInMainHand(null);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.market.create.fail"))));
            }

            return true;
        }
        if(Objects.equals(args[0], "buy")){
            plugin.getMarketGUI().openMarketGUI(player);
            return true;
        }

        return true;
    }

    private void marketHelp(CommandSender sender) {
        plugin.getConfig().getStringList("messages.market.help").forEach(s -> sender.sendMessage(MiniMessage.miniMessage().deserialize(s)));
    }
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if(args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("sell");
            list.add("buy");

            return list;
        }
        if(args[0].equalsIgnoreCase("sell")) {
            List<String> list = new ArrayList<>();
            list.add("<price>");
            return list;
        }
        return new ArrayList<>();
    }
}
