package com.ekvicancode.asrealasyou.crafting

import com.ekvicancode.asrealasyou.AsRealAsYou
import net.minecraft.recipe.*
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.util.collection.DefaultedList

object ModRecipeManager {

    fun registerRecipes() {
        for (variant in RecipeVariantLoader.allVariants) {
            val recipeId = variant.getRecipeId()
            AsRealAsYou.LOGGER.debug("Prepared dynamic recipe {} -> {}", recipeId, variant.getResult().item)
        }
    }

    private fun createRecipeFromVariant(variant: RecipeVariant): Recipe<*> {
        return when (variant.type) {
            RecipeType.SHAPED -> {
                val keyMap = variant.key ?: error("Key map missing for shaped variant")
                val pattern = variant.pattern ?: error("Pattern missing for shaped variant")
                val keyMapIngredient = keyMap.mapValues { (_, itemId) ->
                    Ingredient.ofItems(Registries.ITEM.get(itemId))
                }
                val rawPattern = RawShapedRecipe.create(keyMapIngredient, pattern)

                ShapedRecipe("", CraftingRecipeCategory.MISC, rawPattern, variant.getResult(), false)
            }
            RecipeType.SHAPELESS -> {
                val inputItems = variant.ingredients?.map { itemId ->
                    Ingredient.ofItems(Registries.ITEM.get(itemId))
                } ?: emptyList()

                val ingredients = DefaultedList.ofSize(inputItems.size, Ingredient.EMPTY)
                for (i in inputItems.indices) {
                    ingredients[i] = inputItems[i]
                }

                ShapelessRecipe("", CraftingRecipeCategory.MISC, variant.getResult(), ingredients)
            }
        }
    }
}