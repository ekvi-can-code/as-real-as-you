package com.ekvicancode.asrealasyou.managers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.WorldSavePath
import org.slf4j.LoggerFactory
import java.io.File

data class PlayerLifeData(
    var totalDeaths: Int = 0,
    var accumulatedAgeTicks: Long = 0L,
    var lastDaySpeed: Double = 24.0
)

object LifeSystemManager {
    private val LOGGER = LoggerFactory.getLogger("LifeSystemManager")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val players = mutableMapOf<String, PlayerLifeData>()
    private val sessionStartMs = mutableMapOf<String, Long>()

    private lateinit var saveDir: File
    private const val MAX_AGE_TICKS = 80L * 365L * 24_000L

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
                gson.fromJson(file.readText(), PlayerLifeData::class.java).also {
                    val days = it.accumulatedAgeTicks / 24_000L
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to load ${player.name.string}", e)
                PlayerLifeData()
            }
        } else {
            PlayerLifeData()
        }
        players[player.uuidAsString] = data
        sessionStartMs[player.uuidAsString] = System.currentTimeMillis()
    }

    fun currentAgeTicks(player: ServerPlayerEntity): Long {
        val data = players[player.uuidAsString] ?: return 0L
        val sessionStart = sessionStartMs[player.uuidAsString]
            ?: return data.accumulatedAgeTicks
        val elapsedMs = System.currentTimeMillis() - sessionStart
        val sessionTicks = RealTimeManager.realMsToGameTicks(elapsedMs)
        return data.accumulatedAgeTicks + sessionTicks
    }

    fun getData(player: ServerPlayerEntity): PlayerLifeData {
        return players[player.uuidAsString] ?: PlayerLifeData().also {
            players[player.uuidAsString] = it
        }
    }

    fun flushPlayer(player: ServerPlayerEntity) {
        val data = players[player.uuidAsString] ?: return
        val sessionStart = sessionStartMs[player.uuidAsString] ?: return
        val elapsedMs = System.currentTimeMillis() - sessionStart
        val sessionTicks = RealTimeManager.realMsToGameTicks(elapsedMs)
        data.accumulatedAgeTicks += sessionTicks
        data.lastDaySpeed = RealTimeManager.daySpeed
        sessionStartMs[player.uuidAsString] = System.currentTimeMillis()
    }

    fun flushAllPlayers(playerList: List<ServerPlayerEntity>) {
        playerList.forEach { flushPlayer(it) }
    }

    fun setAge(player: ServerPlayerEntity, days: Long) {
        val data = players[player.uuidAsString] ?: return
        data.accumulatedAgeTicks = days * 24_000L
        sessionStartMs[player.uuidAsString] = System.currentTimeMillis()
        savePlayer(player)
        LOGGER.info("${player.name.string}: set ${days}d")
    }

    fun savePlayer(player: ServerPlayerEntity) {
        val data = players[player.uuidAsString] ?: return
        val file = File(saveDir, "${player.uuidAsString}.json")
        try {
            file.writeText(gson.toJson(data))
        } catch (e: Exception) {
            LOGGER.error("Failed to save ${player.name.string}", e)
        }
    }

    fun onPlayerLeave(player: ServerPlayerEntity) {
        flushPlayer(player)
        savePlayer(player)
    }

    fun removePlayer(player: ServerPlayerEntity) {
        players.remove(player.uuidAsString)
        sessionStartMs.remove(player.uuidAsString)
    }

    fun onPlayerDeath(player: ServerPlayerEntity) {
        val data = players[player.uuidAsString] ?: return
        data.totalDeaths++
        data.accumulatedAgeTicks = 0L
        data.lastDaySpeed = RealTimeManager.daySpeed
        sessionStartMs[player.uuidAsString] = System.currentTimeMillis()
        savePlayer(player)
    }

    fun checkAgeLimitAndKill(player: ServerPlayerEntity) {
        if (currentAgeTicks(player) >= MAX_AGE_TICKS) {
            player.kill()
        }
    }
}