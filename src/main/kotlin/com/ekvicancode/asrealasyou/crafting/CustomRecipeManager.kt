package com.ekvicancode.asrealasyou.crafting

import com.ekvicancode.asrealasyou.AsRealAsYou
import com.ekvicancode.asrealasyou.network.SyncPackets
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import java.util.*

class CustomRecipeManager {

    fun ensurePlayerHasRecipes(player: ServerPlayerEntity, data: PlayerCraftingData) {
        val uuid = player.uuid
        for (itemId in RecipeVariantLoader.getAllItems()) {
            if (data.getAssignedVariant(uuid, itemId) == -1) {
                assignRandomVariant(uuid, itemId, data)
            }
        }
    }

    fun reshuffleRecipes(player: ServerPlayerEntity, data: PlayerCraftingData) {
        val uuid = player.uuid
        for (itemId in RecipeVariantLoader.getAllItems()) {
            assignRandomVariant(uuid, itemId, data)
        }

        player.server?.execute {
            SyncPackets.sendRecipeData(player)
        }
    }

    private fun assignRandomVariant(uuid: UUID, itemId: Identifier, data: PlayerCraftingData) {
        val variants = RecipeVariantLoader.getVariants(itemId)
        if (variants.isEmpty()) return

        val deathCount = data.getDeathCount(uuid)
        val seed = uuid.mostSignificantBits xor uuid.leastSignificantBits xor
                itemId.hashCode().toLong() xor (deathCount.toLong() * 6364136223846793005L)
        val random = Random(seed)
        val variantIndex = random.nextInt(variants.size)

        data.assignVariant(uuid, itemId, variants[variantIndex].variantId)
    }

    fun getActiveVariant(playerUuid: UUID, itemId: Identifier): RecipeVariant? {
        val data = AsRealAsYou.playerCraftingData ?: return null
        val variantId = data.getAssignedVariant(playerUuid, itemId)
        if (variantId == -1) return null
        return RecipeVariantLoader.getVariants(itemId).find { it.variantId == variantId }
    }

    fun tryMatch(playerUuid: UUID, grid: Array<ItemStack>): ItemStack? {
        for (itemId in RecipeVariantLoader.getAllItems()) {
            val variant = getActiveVariant(playerUuid, itemId)
            if (variant != null && variant.matches(grid)) {
                return variant.getResult()
            }
        }
        return null
    }

    fun syncRecipesForPlayer(player: ServerPlayerEntity) {
        val uuid = player.uuid
        val activeAssignments = RecipeVariantLoader.getAllItems()
            .mapNotNull { itemId -> getActiveVariant(uuid, itemId)?.let { itemId to it.variantId } }
            .toMap()

        AsRealAsYou.LOGGER.debug(
            "Synced active custom recipe assignments for {}: {} items",
            player.name.string,
            activeAssignments.size
        )
    }
}