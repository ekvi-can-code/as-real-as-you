package com.ekvicancode.asrealasyou.managers

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.WorldSavePath
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory
import java.io.File

data class PlayerLifeData(
    var birthEpochMs: Long = System.currentTimeMillis(),
    var totalDeaths: Int = 0,
    var accumulatedAgeMs: Long = 0L,
    var lastSeenEpochMs: Long = System.currentTimeMillis(),
    var lastDaySpeed: Double = 24.0
) {
    fun currentAgeMs(
        nowMs: Long = System.currentTimeMillis(),
        currentSpeed: Double = RealTimeManager.daySpeed
    ): Long {
        val elapsed = nowMs - lastSeenEpochMs
        if (elapsed <= 0) return accumulatedAgeMs
        val scaleFactor = currentSpeed / 24.0
        return accumulatedAgeMs + (elapsed * scaleFactor).toLong()
    }

    val ageDays: Long get() = currentAgeMs() / (1000L * 60 * 60 * 24)
    val ageYears: Long get() = ageDays / 365L

    fun flushAge(
        nowMs: Long = System.currentTimeMillis(),
        speed: Double = RealTimeManager.daySpeed
    ) {
        val elapsed = nowMs - lastSeenEpochMs
        if (elapsed > 0) {
            val scaleFactor = speed / 24.0
            accumulatedAgeMs += (elapsed * scaleFactor).toLong()
        }
        lastSeenEpochMs = nowMs
        lastDaySpeed = speed
    }
}

object LifeSystemManager {
    private val LOGGER = LoggerFactory.getLogger("LifeSystemManager")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val players = mutableMapOf<String, PlayerLifeData>()
    private lateinit var saveDir: File

    private const val MAX_AGE_MS = 80L * 365 * 24 * 60 * 60 * 1000

    fun init(server: MinecraftServer) {
        saveDir = server.getSavePath(WorldSavePath.ROOT)
            .resolve("playerdata_life")
            .toFile()
        saveDir.mkdirs()
    }

    fun loadPlayer(player: ServerPlayerEntity) {
        val file = File(saveDir, "${player.uuidAsString}.json")
        val data = if (file.exists()) {
            try {
                val loaded = gson.fromJson(file.readText(), PlayerLifeData::class.java)
                loaded
            } catch (e: Exception) {
                LOGGER.error("Failed to load data for ${player.name.string}", e)
                PlayerLifeData()
            }
        } else {
            LOGGER.info("No save found for ${player.name.string}, creating new")
            PlayerLifeData()
        }
        data.lastSeenEpochMs = System.currentTimeMillis()
        players[player.uuidAsString] = data
    }

    fun savePlayer(player: ServerPlayerEntity) {
        val data = players[player.uuidAsString] ?: return
        val file = File(saveDir, "${player.uuidAsString}.json")
        try {
            file.writeText(gson.toJson(data))
        } catch (e: Exception) {
            LOGGER.error("Failed to save data for ${player.name.string}", e)
        }
    }

    fun getData(player: ServerPlayerEntity): PlayerLifeData {
        return players[player.uuidAsString] ?: PlayerLifeData().also {
            players[player.uuidAsString] = it
        }
    }

    fun onPlayerLeave(player: ServerPlayerEntity) {
        val data = players[player.uuidAsString] ?: return
        data.flushAge()
        savePlayer(player)
    }

    fun removePlayer(player: ServerPlayerEntity) {
        players.remove(player.uuidAsString)
    }

    fun onPlayerDeath(player: ServerPlayerEntity) {
        val data = players[player.uuidAsString] ?: return
        data.flushAge()
        data.totalDeaths++
        data.accumulatedAgeMs = 0L
        data.birthEpochMs = System.currentTimeMillis()
        data.lastSeenEpochMs = System.currentTimeMillis()
        data.lastDaySpeed = RealTimeManager.daySpeed
        savePlayer(player)
    }

    fun checkAgeLimitAndKill(player: ServerPlayerEntity) {
        val data = getData(player)
        if (data.currentAgeMs() >= MAX_AGE_MS) {
            player.kill()
        }
    }

    fun flushAllPlayers() {
        val nowMs = System.currentTimeMillis()
        players.values.forEach { it.flushAge(nowMs) }
    }
}