package com.ekvicancode.mixins

import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.time.LocalTime

@Mixin(ServerWorld::class)
class RealTimeMixin {
    companion object {
        fun setRealTime(world: World) {
            if (world !is ServerWorld) return
            val now = LocalTime.now()
            val hours = now.hour * 1000L //
            val minutes = (now.minute * 1000L / 60)
            val gameTime = hours + minutes

            world.timeOfDay = gameTime
        }
    }

    @Inject(method = "tick", at = At("HEAD"))
    private fun syncRealTime(ci: CallbackInfo) {
        val world = this as ServerWorld
        setRealTime(world)
    }
}
