package com.ekvicancode.asrealasyou.mixins

import com.ekvicancode.asrealasyou.comps.PlayerLife
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PlayerEntity::class)
abstract class LifeSystemMixin {

    @Inject(method = ["hurtTime"], at = [arrayOf(At("HEAD"))])
    private fun checkDeath(source: DamageSource, amount: Float, cir: CallbackInfoReturnable<Boolean>): Boolean {
        val player = this as PlayerEntity
        if (player is ServerPlayerEntity) {
            val world = player.world as ServerWorld
            val lifeComp = player.getComponent(PlayerLife.key)
            if (lifeComp != null && lifeComp.totalLives <= 0) {
                player.sendMessage(Text.literal("You have no lives left!"), true)
                player.damage(source, Float.MAX_VALUE)
                return true
            }
        }
        return false
    }

    @Inject(method = ["onDeath"], at = [arrayOf(At("HEAD"))])
    private fun onPlayerDeath(source: DamageSource, cir: CallbackInfoReturnable<Boolean>) {
        val player = this as PlayerEntity
        val world = player.world
        resetLife(player)
    }

    private fun resetLife(player: PlayerEntity) {
        player.inventory.clear()
        player.experienceLevel = 0
        player.totalExperience = 0
        player.health = 20f
        player.hungerManager.foodLevel = 20

        val spawnPos = (player.world as? ServerWorld)?.spawnPos
        if (spawnPos != null) {
            player.teleport(player.world as ServerWorld, spawnPos.x.toDouble(), spawnPos.y.toDouble(), spawnPos.z.toDouble(), 0f, 0f)
        }
        player.sendMessage(Text.literal("Respawned with new life!"), true)
    }
}
