package com.ekvicancode.asrealasyou.mixin

import com.ekvicancode.asrealasyou.AsRealAsYou
import com.ekvicancode.asrealasyou.crafting.RecipeVariantLoader
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.CraftingResultInventory
import net.minecraft.inventory.RecipeInputInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeEntry
import net.minecraft.registry.Registries
import net.minecraft.screen.CraftingScreenHandler
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(CraftingScreenHandler::class)
abstract class CraftingScreenHandlerMixin {

    private companion object {

        @JvmStatic
        @Inject(
            method = ["updateResult"],
            at = [At("HEAD")],
            cancellable = true
        )
        private fun onUpdateResult(
            handler: ScreenHandler,
            world: World,
            player: PlayerEntity,
            craftingInventory: RecipeInputInventory,
            resultInventory: CraftingResultInventory,
            recipe: RecipeEntry<CraftingRecipe>?,
            ci: CallbackInfo
        ) {
            if (world.isClient) return
            if (player !is ServerPlayerEntity) return
            val recipeManager = AsRealAsYou.recipeManager ?: return

            val grid = Array(9) { i ->
                if (i < craftingInventory.size()) craftingInventory.getStack(i) else ItemStack.EMPTY
            }

            val customResult = recipeManager.tryMatch(player.uuid, grid)
            if (customResult != null) {
                resultInventory.setStack(0, customResult)
                ci.cancel()
                return
            }

            if (recipe != null) {
                val vanillaResult = recipe.value().getResult(world.registryManager)
                val resultId = Registries.ITEM.getId(vanillaResult.item)
                if (RecipeVariantLoader.hasVariants(resultId)) {
                    resultInventory.setStack(0, ItemStack.EMPTY)
                    ci.cancel()
                }
            }
        }
    }
}