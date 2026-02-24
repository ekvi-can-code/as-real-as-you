package com.ekvicancode.asrealasyou.managers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.WorldSavePath
import java.io.File

// Данные игрока прямо здесь — без отдельного файла
data class PlayerLifeData(
    @SerializedName("birth_epoch_ms")
    var birthEpochMs: Long = System.currentTimeMillis(),
    @SerializedName("total_deaths")
    var totalDeaths: Int = 0,
    @SerializedName("last_seen_epoch_ms")
    var lastSeenEpochMs: Long = System.currentTimeMillis()
) {
    val ageMs: Long
        get() = System.currentTimeMillis() - birthEpochMs

    val ageDays: Long
        get() = ageMs / (1000L * 60L * 60L * 24L)

    val ageYears: Long
        get() = ageDays / 365L
}

object LifeSystemManager {

    private val playerData = mutableMapOf<String, PlayerLifeData>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private var dataDir: File? = null

    fun init(server: MinecraftServer) {
        dataDir = File(
            server.getSavePath(WorldSavePath.ROOT).toFile(),
            "asrealasyou"
        )
        dataDir!!.mkdirs()
    }

    private fun getFile(uuid: String): File {
        return File(dataDir, "$uuid.json")
    }

    fun getData(player: ServerPlayerEntity): PlayerLifeData {
        return playerData.getOrPut(player.uuidAsString) {
            PlayerLifeData(birthEpochMs = System.currentTimeMillis())
        }
    }

    fun loadPlayer(player: ServerPlayerEntity) {
        val file = getFile(player.uuidAsString)
        if (file.exists()) {
            try {
                val data = gson.fromJson(file.readText(), PlayerLifeData::class.java)
                playerData[player.uuidAsString] = data
            } catch (e: Exception) {
                playerData[player.uuidAsString] = PlayerLifeData(
                    birthEpochMs = System.currentTimeMillis()
                )
            }
        } else {
            playerData[player.uuidAsString] = PlayerLifeData(
                birthEpochMs = System.currentTimeMillis()
            )
            savePlayer(player)
        }
    }

    fun savePlayer(player: ServerPlayerEntity) {
        val data = getData(player)
        val file = getFile(player.uuidAsString)
        try {
            file.writeText(gson.toJson(data))
        } catch (e: Exception) {
            // ignore
        }
    }

    fun onPlayerLeave(player: ServerPlayerEntity) {
        val data = getData(player)
        data.lastSeenEpochMs = System.currentTimeMillis()
        savePlayer(player)
    }

    fun onPlayerDeath(player: ServerPlayerEntity) {
        val data = getData(player)
        data.totalDeaths++
        data.birthEpochMs = System.currentTimeMillis()
        data.lastSeenEpochMs = System.currentTimeMillis()
        playerData[player.uuidAsString] = data
        savePlayer(player)
    }

    fun removePlayer(player: ServerPlayerEntity) {
        playerData.remove(player.uuidAsString)
    }
}