package com.ekvicancode.asrealasyou.mixins

import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.math.floor

@Mixin(World::class)
abstract class RealTimeMixin {

    @Inject(method = ["getTimeOfDay"], at = [arrayOf(At("RETURN"))], cancellable = true)
    private fun modifyTimeOfDay(cir: CallbackInfoReturnable<Long>): Unit {
        val world = this as World
        if (!world.isClient) {
            val realTime = System.currentTimeMillis() % 24000
            cir.returnValue = realTime
        }
    }
}
