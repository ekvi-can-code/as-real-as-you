package com.ekvicancode.asrealasyou

import com.ekvicancode.asrealasyou.commands.ModCommands
import com.ekvicancode.asrealasyou.managers.LifeSystemManager
import com.ekvicancode.asrealasyou.managers.RealTimeManager
import com.ekvicancode.asrealasyou.network.SyncPackets
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
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

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			ModCommands.register(dispatcher)
		}

		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			LifeSystemManager.init(server)
			disableDaylightCycle(server)
			LOGGER.info("Daylight cycle disabled, real time sync active")
		}

		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			val player = handler.player
			LifeSystemManager.loadPlayer(player)
			val data = LifeSystemManager.getData(player)

			LOGGER.info(
				"Player ${player.name.string} joined. " +
						"Age: ${data.ageDays} days, Deaths: ${data.totalDeaths}"
			)

			SyncPackets.sendLifeData(player)

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

		ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
			if (entity is ServerPlayerEntity) {
				onPlayerDied(entity)
			}
		}

		ServerTickEvents.END_SERVER_TICK.register { server ->
			tickCounter++
			if (tickCounter >= TIME_SYNC_INTERVAL) {
				tickCounter = 0
				syncTimeForAllWorlds(server)
				checkAgeLimits(server)
				server.playerManager.playerList.forEach { player ->
					SyncPackets.sendLifeData(player)
				}
			}
		}

		LOGGER.info("AsRealAsYou initialized!")
	}

	private fun onPlayerDied(player: ServerPlayerEntity) {
		LifeSystemManager.onPlayerDeath(player)
		val data = LifeSystemManager.getData(player)

		player.sendMessage(
			Text.literal("§cВы умерли! Жизнь начинается заново.")
		)

		SyncPackets.sendLifeData(player)
		resetPlayerProgress(player)
	}

	private fun resetPlayerProgress(player: ServerPlayerEntity) {
		player.inventory.clear()
		player.experienceLevel = 0
		player.experienceProgress = 0f
		player.totalExperience = 0

		val spawnPos = player.serverWorld.spawnPos
		player.teleport(
			player.serverWorld,
			spawnPos.x.toDouble() + 0.5,
			spawnPos.y.toDouble(),
			spawnPos.z.toDouble() + 0.5,
			emptySet(),
			0f,
			0f
		)
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

	private fun checkAgeLimits(server: MinecraftServer) {
		for (player in server.playerManager.playerList) {
			LifeSystemManager.checkAgeLimitAndKill(player)
		}
	}
}