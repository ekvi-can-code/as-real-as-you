package com.ekvicancode.asrealasyou.crafting

import com.ekvicancode.asrealasyou.AsRealAsYou
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object RecipeVariantLoader {

    private val GSON = Gson()
    private val variants = mutableMapOf<Identifier, MutableList<RecipeVariant>>()
    var loadedCount = 0
        private set
    private const val INDEX_PATH = "data/${AsRealAsYou.MOD_ID}/recipe_variants/index.json"
    private const val BASE_PATH = "data/${AsRealAsYou.MOD_ID}/recipe_variants/"

    val allVariants: List<RecipeVariant>
        get() = variants.values.flatten()

    fun loadAllVariants() {
        variants.clear()
        loadedCount = 0
        loadFromClasspath()
    }

    private fun loadFromClasspath() {
        val indexStream = RecipeVariantLoader::class.java.classLoader.getResourceAsStream(INDEX_PATH)
            ?: run {
                AsRealAsYou.LOGGER.warn("No recipe_variants/index.json found in resources")
                return
            }

        indexStream.use { stream ->
            val root = GSON.fromJson(
                InputStreamReader(stream, StandardCharsets.UTF_8),
                JsonObject::class.java
            )
            loadVariantsFromIndex(root)
        }
    }

    private fun loadVariantsFromIndex(root: JsonObject) {
        val filesArray = root.getAsJsonArray("files") ?: run {
            AsRealAsYou.LOGGER.warn("recipe_variants/index.json does not contain 'files' array")
            return
        }

        for (fileElement in filesArray) {
            try {
                val fileName = fileElement.asString
                val resourcePath = BASE_PATH + fileName
                val fileStream = RecipeVariantLoader::class.java.classLoader.getResourceAsStream(resourcePath)
                if (fileStream == null) {
                    AsRealAsYou.LOGGER.warn("Recipe variant file not found: {}", resourcePath)
                    continue
                }
                fileStream.use { stream ->
                    val fileRoot = GSON.fromJson(
                        InputStreamReader(stream, StandardCharsets.UTF_8),
                        JsonObject::class.java
                    )
                    loadVariantFile(fileName, fileRoot)
                }
            } catch (e: Exception) {
                AsRealAsYou.LOGGER.error("Failed to load variant file {}", fileElement.asString, e)
            }
        }
    }

    private fun loadVariantFile(fileName: String, root: JsonObject) {
        val itemId = root.get("item")?.asString ?: run {
            AsRealAsYou.LOGGER.warn("Recipe variant file {} does not contain 'item'", fileName)
            return
        }

        val variantsArray = root.getAsJsonArray("variants") ?: run {
            AsRealAsYou.LOGGER.warn("Recipe variant file {} does not contain 'variants'", fileName)
            return
        }

        try {
            val itemIdentifier = Identifier.of(itemId)
            val item = Registries.ITEM.get(itemIdentifier)
            if (item == Items.AIR && itemId != "minecraft:air") {
                AsRealAsYou.LOGGER.warn("Unknown item in variant file: {}", itemId)
                return
            }

            val variantList = mutableListOf<RecipeVariant>()

            for (variantElement in variantsArray) {
                val variant = RecipeVariant.fromJson(itemIdentifier, variantElement.asJsonObject)
                if (variant != null) {
                    variantList.add(variant)
                }
            }

            if (variantList.isNotEmpty()) {
                variants[itemIdentifier] = variantList
                loadedCount++
                AsRealAsYou.LOGGER.debug("Loaded {} variants for {} from {}", variantList.size, itemId, fileName)
            }
        } catch (e: Exception) {
            AsRealAsYou.LOGGER.error("Failed to load variants for {} from {}", itemId, fileName, e)
        }
    }

    fun getVariants(itemId: Identifier): List<RecipeVariant> = variants[itemId] ?: emptyList()
    fun getAllItems(): Set<Identifier> = variants.keys
    fun hasVariants(itemId: Identifier): Boolean = variants.containsKey(itemId) && variants[itemId]!!.isNotEmpty()
    fun getAllVariantRecipeIds(): List<Identifier> = allVariants.map { it.getRecipeId() }
}