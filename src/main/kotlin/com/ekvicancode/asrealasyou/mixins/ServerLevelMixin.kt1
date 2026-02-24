package com.ekvicancode.asrealasyou.mixins

import com.ekvicancode.asrealasyou.managers.RealTimeManager
import net.minecraft.server.world.ServerWorld
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.function.BooleanSupplier

@Mixin(ServerWorld::class)
class ServerLevelMixin {

    @Inject(
        method = ["method_18765(Ljava/util/function/BooleanSupplier;)V"],
        at = [At("RETURN")]
    )
    private fun onTick(shouldKeepTicking: BooleanSupplier, ci: CallbackInfo) {
        val world = this as ServerWorld
        if (world.server.isPaused) return

        val realGameTime = RealTimeManager.getCurrentGameTimeExact()
        val currentDay = world.timeOfDay / 24000L
        val newTime = currentDay * 24000L + realGameTime
        world.timeOfDay = newTime
    }
}