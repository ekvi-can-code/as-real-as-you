package com.ekvicancode.asrealasyou.mixins

import com.ekvicancode.asrealasyou.managers.LifeSystemManager
import com.ekvicancode.asrealasyou.network.SyncPackets
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(PlayerEntity::class)
@Inject(
    method = "method_9236(Lnet/minecraft/class_1282;)V",
    at = @At("RETURN")
    )
    private fun onPlayerDeath(
        damageSource: DamageSource,
        ci: CallbackInfo
    ){
        val player = this as? ServerPlayerEntity ?: return

        LifeSystemManager.onPlayerDeath(player)
        val data = LifeSystemManager.getData(player)

        player.sendMessage(
            Text.literal(
                "§cВы умерли! Жизнь начинается заново."
            )
        )

        SyncPackets.sendLifeData(player, data.birthEpochMs, data.totalDeaths)
        resetPlayerProgress(player)
    }

    private fun resetPlayerProgress(player: ServerPlayerEntity) {
        player.inventory.clear()
        player.experienceLevel = 0
        player.experienceProgress = 0f
        player.totalExperience = 0

        val spawnPos = player.serverWorld.spawnPos
        player.teleport(
            player.serverWorld,
            spawnPos.x.toDouble() + 0.5,
            spawnPos.y.toDouble(),
            spawnPos.z.toDouble() + 0.5,
            0f,
            0f
        )
    }
}