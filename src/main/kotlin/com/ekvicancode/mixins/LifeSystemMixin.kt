package com.ekvicancode.mixins

import com.ekvicancode.comps.PlayerLife
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(PlayerEntity::class)
class LifeSystemMixin {
    @Inject(method = "tick", at = At("HEAD"))
    private fun checkLifeStatus(ci: CallbackInfo) {
        val player = this as PlayerEntity
        if (player.world.isClient) return

        val lifeComp = PlayerLife.ID.get(player)

        if (lifeComp.checkDeath()) {
            player.sendMessage(Text.literal("Ты умер от старости"), true)
            player.damage(DamageSource.GENERIC, 1000f)
        }
    }

    @Inject(method = "onDeath", at = At("HEAD"), cancellable = true)
    private fun rebirth(source: DamageSource, ci: CallbackInfo) {
        val player = this as PlayerEntity
        if (player.world.isClient) return

        val lifeComp = PlayerLife.ID.get(player)
        lifeComp.resetLife()

        player.apply {
            inventory.clear()
            experienceLevel = 0
            totalExperience = 0
            health = 20f
            foodLevel = 20
        }

        player.teleport(player.world as ServerWorld,
                player.world.spawnPos.x + 0.5, player.world.spawnPos.y + 1.0, player.world.spawnPos.z + 0.5, 0f, 0f)

        player.sendMessage(Text.literal("Новая жизнь началась!"), true)
        ci.cancel()
    }
}
