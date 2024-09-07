package com.ceraia.modules.arenas

import com.ceraia.Ceraia
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.Listener

class ArenaCommands(plugin: Ceraia) : Listener {
    private val plugin: Ceraia = plugin
    private val dispatcher: CommandDispatcher<CommandSender> = CommandDispatcher()

    init {
        registerCommands()
    }

    private fun registerCommands() {
        dispatcher.register(
            LiteralArgumentBuilder.literal<CommandSender>("arena")
                .then(
                    LiteralArgumentBuilder.literal<CommandSender>("create")
                        .then(
                            RequiredArgumentBuilder.argument<CommandSender, String>("name", StringArgumentType.string())
                                .executes { context: CommandContext<CommandSender> -> createArena(context) }
                        )
                )
                .then(
                    LiteralArgumentBuilder.literal<CommandSender>("delete")
                        .then(
                            RequiredArgumentBuilder.argument<CommandSender, String>("name", StringArgumentType.string())
                                .executes { context: CommandContext<CommandSender> -> deleteArena(context) }
                        )
                )
        )
    }

    private fun createArena(context: CommandContext<CommandSender>): Int {
        val sender = context.source
        val name = StringArgumentType.getString(context, "name")

        sender.sendMessage("Arena $name created.")
        return 1
    }

    private fun deleteArena(context: CommandContext<CommandSender>): Int {
        val sender = context.source
        val name = StringArgumentType.getString(context, "name")

        sender.sendMessage("Arena $name deleted.")
        return 1
    }

    fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        try {
            dispatcher.execute(command.name + " " + args?.joinToString(" "), sender)
        } catch (e: CommandSyntaxException) {
            sender.sendMessage("Invalid command syntax.")
        }
        return true
    }

    fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>?): MutableList<String>? {

        return null
    }
}