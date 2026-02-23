package com.ekvicancode.asrealasyou

import com.ekvicancode.asrealasyou.managers.LifeSystemManager
import com.ekvicancode.asrealasyou.managers.RealTimeManager
import com.ekvicancode.asrealasyou.network.SyncPackets
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.world.GameRules
import org.slf4j.LoggerFactory

object AsRealAsYou : ModInitializer {

	const val MOD_ID = "asrealasyou"
	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	private const val TIME_SYNC_INTERVAL = 20
	private var tickCounter = 0

	override fun onInitialize() {
		LOGGER.info("AsRealAsYou initializing...")

		SyncPackets.registerServer()

		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			disableDaylightCycle(server)
			LOGGER.info("Daylight cycle disabled, real time sync active")
		}

		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			val player = handler.player
			LifeSystemManager.loadPlayer(player)
			val data = LifeSystemManager.getData(player)

			val absentMs = System.currentTimeMillis() - data.lastSeenEpochMs
			val absentMinutes = absentMs / 60000L

			if (absentMinutes > 0) {
				LOGGER.info(
					"Player ${player.name.string} absent for " +
							"$absentMinutes min. Age: ${data.ageDays} days"
				)
			}

			SyncPackets.sendLifeData(player, data.birthEpochMs, data.totalDeaths)

			player.sendMessage(
				Text.literal(
					"§aДобро пожаловать! Возраст: ${data.ageYears} лет " +
							"(${data.ageDays} дней). Перерождений: ${data.totalDeaths}"
				)
			)
		}

		ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
			val player = handler.player
			LifeSystemManager.onPlayerLeave(player)
			LifeSystemManager.removePlayer(player)
		}

		ServerTickEvents.END_SERVER_TICK.register { server ->
			tickCounter++
			if (tickCounter >= TIME_SYNC_INTERVAL) {
				tickCounter = 0
				syncTimeForAllWorlds(server)
			}
		}

		LOGGER.info("AsRealAsYou initialized!")
	}

	private fun disableDaylightCycle(server: MinecraftServer) {
		server.worlds.forEach { world ->
			world.gameRules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server)
		}
	}

	private fun syncTimeForAllWorlds(server: MinecraftServer) {
		val gameTime = RealTimeManager.getCurrentGameTimeExact()
		server.worlds.forEach { world ->
			setWorldTime(world, gameTime)
		}
	}

	private fun setWorldTime(world: ServerWorld, timeOfDay: Long) {
		val currentDay = world.timeOfDay / 24000L
		val newTime = currentDay * 24000L + timeOfDay
		world.timeOfDay = newTime
	}
}