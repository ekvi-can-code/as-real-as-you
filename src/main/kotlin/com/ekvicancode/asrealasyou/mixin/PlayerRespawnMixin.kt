package com.ekvicancode.asrealasyou.mixin

import com.ekvicancode.asrealasyou.AsRealAsYou
import com.ekvicancode.asrealasyou.network.SyncPackets
import net.minecraft.server.PlayerManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PlayerManager::class)
abstract class PlayerRespawnMixin {

    @Inject(method = ["respawnPlayer"], at = [At("RETURN")])
    private fun onRespawn(
        player: ServerPlayerEntity,
        alive: Boolean,
        removalReason: Entity.RemovalReason,
        cir: CallbackInfoReturnable<ServerPlayerEntity>
    ) {
        val respawnedPlayer = cir.returnValue
        if (respawnedPlayer != null && respawnedPlayer.server != null) {
            respawnedPlayer.server.execute {
                val craftData = AsRealAsYou.playerCraftingData
                val recipeManager = AsRealAsYou.recipeManager
                if (craftData != null && recipeManager != null) {
                    recipeManager.ensurePlayerHasRecipes(respawnedPlayer, craftData)
                    recipeManager.syncRecipesForPlayer(respawnedPlayer)
                }
                SyncPackets.sendRecipeData(respawnedPlayer)
            }
        }
    }
}