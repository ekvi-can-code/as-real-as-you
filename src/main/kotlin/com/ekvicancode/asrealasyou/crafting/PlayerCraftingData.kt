package com.ekvicancode.asrealasyou.crafting

import com.ekvicancode.asrealasyou.AsRealAsYou
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerCraftingData {

    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().create()
    }

    data class PlayerData(
        var deathCount: Int = 0,
        val assignedVariants: MutableMap<String, Int> = mutableMapOf()
    )

    private val playerDataMap = ConcurrentHashMap<UUID, PlayerData>()

    fun getOrCreate(uuid: UUID): PlayerData =
        playerDataMap.computeIfAbsent(uuid) { PlayerData() }

    fun incrementDeathCount(uuid: UUID) {
        getOrCreate(uuid).deathCount++
    }

    fun getDeathCount(uuid: UUID): Int = getOrCreate(uuid).deathCount

    fun assignVariant(uuid: UUID, itemId: Identifier, variantId: Int) {
        getOrCreate(uuid).assignedVariants[itemId.toString()] = variantId
    }

    fun getAssignedVariant(uuid: UUID, itemId: Identifier): Int =
        getOrCreate(uuid).assignedVariants.getOrDefault(itemId.toString(), -1)

    fun getAllAssignedVariants(uuid: UUID): Map<String, Int> =
        getOrCreate(uuid).assignedVariants

    fun save(server: MinecraftServer) {
        val savePath = getSavePath(server)
        try {
            Files.createDirectories(savePath.parent)

            val root = JsonObject()
            for ((uuid, data) in playerDataMap) {
                val playerObj = JsonObject()
                playerObj.addProperty("deathCount", data.deathCount)

                val variantsObj = JsonObject()
                for ((item, variantId) in data.assignedVariants) {
                    variantsObj.addProperty(item, variantId)
                }
                playerObj.add("assignedVariants", variantsObj)
                root.add(uuid.toString(), playerObj)
            }

            Files.writeString(savePath, GSON.toJson(root))
            AsRealAsYou.LOGGER.debug("Saved player crafting data for {} players", playerDataMap.size)
        } catch (e: Exception) {
            AsRealAsYou.LOGGER.error("Failed to save player crafting data", e)
        }
    }

    fun load(server: MinecraftServer) {
        val savePath = getSavePath(server)
        if (!Files.exists(savePath)) {
            AsRealAsYou.LOGGER.debug("No existing player crafting data found")
            return
        }

        try {
            val json = Files.readString(savePath)
            val root = GSON.fromJson(json, JsonObject::class.java)

            playerDataMap.clear()
            for ((key, value) in root.entrySet()) {
                val uuid = UUID.fromString(key)
                val playerObj = value.asJsonObject

                val data = PlayerData()
                data.deathCount = playerObj.get("deathCount").asInt

                val variantsObj = playerObj.getAsJsonObject("assignedVariants")
                for ((item, variantId) in variantsObj.entrySet()) {
                    data.assignedVariants[item] = variantId.asInt
                }

                playerDataMap[uuid] = data
            }
        } catch (e: Exception) {
            AsRealAsYou.LOGGER.error("Failed to load player crafting data", e)
        }
    }

    private fun getSavePath(server: MinecraftServer) =
        server.getSavePath(WorldSavePath.ROOT)
            .resolve(AsRealAsYou.MOD_ID)
            .resolve("player_crafting_data.json")
}