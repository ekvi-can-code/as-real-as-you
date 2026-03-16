package com.ekvicancode.asrealasyou.crafting

import com.ekvicancode.asrealasyou.AsRealAsYou
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

enum class RecipeType {
    SHAPED, SHAPELESS
}

class RecipeVariant private constructor(
    val resultItem: Identifier,
    val variantId: Int,
    val type: RecipeType,
    val resultCount: Int,
    val pattern: List<String>?,
    val key: Map<Char, Identifier>?,
    val ingredients: List<Identifier>?
) {

    companion object {
        fun fromJson(resultItem: Identifier, json: JsonObject): RecipeVariant? {
            return try {
                val id = json.get("id").asInt
                val typeStr = if (json.has("type")) json.get("type").asString else "shaped"
                val type = if (typeStr == "shapeless") RecipeType.SHAPELESS else RecipeType.SHAPED
                val count = if (json.has("result_count")) json.get("result_count").asInt else 1

                if (type == RecipeType.SHAPED) {
                    val patternArray = json.getAsJsonArray("pattern")
                    val pattern = patternArray.map { it.asString }

                    val keyObj = json.getAsJsonObject("key")
                    val key = mutableMapOf<Char, Identifier>()
                    for ((k, v) in keyObj.entrySet()) {
                        key[k[0]] = Identifier.of(v.asString)
                    }

                    RecipeVariant(resultItem, id, type, count, pattern, key, null)
                } else {
                    val ingredientsArray = json.getAsJsonArray("ingredients")
                    val ingredients = ingredientsArray.map { Identifier.of(it.asString) }

                    RecipeVariant(resultItem, id, type, count, null, null, ingredients)
                }
            } catch (e: Exception) {
                AsRealAsYou.run { LOGGER.error("Failed parse recipe variant for {}", resultItem, e) }
                null
            }
        }
    }

    fun matches(grid: Array<ItemStack>): Boolean {
        return when (type) {
            RecipeType.SHAPED -> matchesShaped(grid)
            RecipeType.SHAPELESS -> matchesShapeless(grid)
        }
    }

    private fun matchesShaped(grid: Array<ItemStack>): Boolean {
        val pat = pattern ?: return false
        val k = key ?: return false
        val height = pat.size
        val width = pat.maxOf { it.length }

        for (offsetX in 0..(3 - width)) {
            for (offsetY in 0..(3 - height)) {
                if (matchesShapedAt(grid, offsetX, offsetY, width, height, pat, k)) {
                    return true
                }
            }
        }
        return false
    }

    private fun matchesShapedAt(
        grid: Array<ItemStack>,
        offsetX: Int, offsetY: Int,
        width: Int, height: Int,
        pat: List<String>, k: Map<Char, Identifier>
    ): Boolean {
        for (y in 0 until 3) {
            for (x in 0 until 3) {
                val index = y * 3 + x
                val stackInSlot = grid[index]

                val patternX = x - offsetX
                val patternY = y - offsetY

                var expectedItem: Identifier? = null

                if (patternX in 0 until width && patternY in 0 until height) {
                    val row = pat[patternY]
                    if (patternX < row.length) {
                        val c = row[patternX]
                        if (c != ' ') {
                            expectedItem = k[c]
                        }
                    }
                }

                if (expectedItem != null) {
                    if (stackInSlot.isEmpty) return false
                    val stackId = Registries.ITEM.getId(stackInSlot.item)
                    if (stackId != expectedItem) return false
                } else {
                    if (!stackInSlot.isEmpty) return false
                }
            }
        }
        return true
    }

    private fun matchesShapeless(grid: Array<ItemStack>): Boolean {
        val ing = ingredients ?: return false
        val remaining = ing.toMutableList()

        for (stack in grid) {
            if (!stack.isEmpty) {
                val stackId = Registries.ITEM.getId(stack.item)
                if (!remaining.remove(stackId)) return false
            }
        }

        return remaining.isEmpty()
    }

    fun getResult(): ItemStack {
        val item = Registries.ITEM.get(resultItem)
        return ItemStack(item, resultCount)
    }

    fun getRecipeId(): Identifier {
        return Identifier.of(AsRealAsYou.MOD_ID, "${resultItem.path}/${variantId}")
    }
}