package com.ekvicancode.asrealasyou.commands

import com.ekvicancode.asrealasyou.managers.LifeSystemManager
import com.ekvicancode.asrealasyou.managers.RealTimeManager
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object ModCommands {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("changedayspeed")
                .requires { it.hasPermissionLevel(4) }
                .executes { context ->
                    val server = context.source.server
                    LifeSystemManager.flushAllPlayers(server.playerManager.playerList)
                    RealTimeManager.daySpeed = 1.0
                    context.source.sendFeedback(
                        { Text.literal("Скорость дня: 1") }, true
                    )
                    1
                }
                .then(
                    CommandManager.argument("speed", IntegerArgumentType.integer(1))
                        .executes { context ->
                            val server = context.source.server
                            val speed = IntegerArgumentType.getInteger(context, "speed")
                            LifeSystemManager.flushAllPlayers(server.playerManager.playerList)
                            RealTimeManager.daySpeed = speed.toDouble()
                            context.source.sendFeedback(
                                { Text.literal("Скорость дня: $speed") }, true
                            )
                            1
                        }
                )
        )
    }
}