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
                    LifeSystemManager.flushAllPlayers()
                    RealTimeManager.daySpeed = 24.0
                    context.source.sendFeedback(
                        { Text.literal("Скорость дня установлена: 24") }, false
                    )
                    1
                }
                .then(
                    CommandManager.argument("speed", IntegerArgumentType.integer(1))
                        .executes { context ->
                            val speed = IntegerArgumentType.getInteger(context, "speed")
                            LifeSystemManager.flushAllPlayers()
                            RealTimeManager.daySpeed = speed.toDouble()
                            context.source.sendFeedback(
                                { Text.literal("Скорость дня установлена: $speed") }, false
                            )
                            1
                        }
                )
        )
    }
}