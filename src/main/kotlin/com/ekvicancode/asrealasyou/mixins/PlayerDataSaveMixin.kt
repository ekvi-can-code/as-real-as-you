package com.ekvicancode.asrealasyou.mixins

import com.ekvicancode.asrealasyou.managers.LifeSystemManager
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ServerPlayerEntity::class)
class PlayerDataSaveMixin {

    @Inject(
        method = ["method_9251(Lnet/minecraft/nbt/NbtCompound;)V"],
        at = [At("RETURN")]
    )
    private fun onWriteNbt(nbt: NbtCompound, ci: CallbackInfo) {
        val player = this as ServerPlayerEntity
        nbt.put("AsRealAsYouLifeData", LifeSystemManager.serializeToNbt(player))
    }

    @Inject(
        method = ["method_9253(Lnet/minecraft/nbt/NbtCompound;)V"],
        at = [At("RETURN")]
    )
    private fun onReadNbt(nbt: NbtCompound, ci: CallbackInfo) {
        val player = this as ServerPlayerEntity
        if (nbt.contains("AsRealAsYouLifeData")) {
            LifeSystemManager.deserializeFromNbt(
                player,
                nbt.getCompound("AsRealAsYouLifeData")
            )
        }
    }
}