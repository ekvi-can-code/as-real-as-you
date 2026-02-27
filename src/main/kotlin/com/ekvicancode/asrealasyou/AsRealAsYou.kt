package com.ekvicancode.asrealasyou

import com.ekvicancode.asrealasyou.commands.ModCommands
import com.ekvicancode.asrealasyou.managers.LifeSystemManager
import com.ekvicancode.asrealasyou.managers.RealTimeManager
import com.ekvicancode.asrealasyou.network.ClientLifeData
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

	private const val SYNC_INTERVAL = 5
	private const val SAVE_INTERVAL = 6000
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
			RealTimeManager.initBase()
			LOGGER.info("Server started, time initialized")
		}

		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			LOGGER.info("Saving all players...")
			val players = server.playerManager.playerList
			LifeSystemManager.flushAllPlayers(players)
			players.forEach { LifeSystemManager.savePlayer(it) }
		}

		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			val player = handler.player
			LifeSystemManager.loadPlayer(player)

			val data = LifeSystemManager.getData(player)
			val days = ClientLifeData.ageDays
			val years = ClientLifeData.ageYears

			SyncPackets.sendLifeData(player)

			player.sendMessage(
				Text.literal(
					"§aДобро пожаловать! Возраст: $years лет " +
							"($days дней). Перерождений: ${data.totalDeaths}"
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

			if (tickCounter % SYNC_INTERVAL == 0) {
				syncTimeForAllWorlds(server)
				checkAgeLimits(server)
				server.playerManager.playerList.forEach { player ->
					SyncPackets.sendLifeData(player)
				}
			}

			if (tickCounter % SAVE_INTERVAL == 0) {
				tickCounter = 0
				LOGGER.info("Auto-save players")
				val players = server.playerManager.playerList
				LifeSystemManager.flushAllPlayers(players)
				players.forEach { LifeSystemManager.savePlayer(it) }
			}
		}

		LOGGER.info("AsRealAsYou initialized!")
	}

	private fun onPlayerDied(player: ServerPlayerEntity) {
		LifeSystemManager.onPlayerDeath(player)
		player.sendMessage(Text.literal("§cВы умерли! Жизнь начинается заново."))
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
			0f, 0f
		)
	}

	private fun disableDaylightCycle(server: MinecraftServer) {
		server.worlds.forEach { world ->
			world.gameRules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server)
		}
	}

	private fun syncTimeForAllWorlds(server: MinecraftServer) {
		val gameTime = RealTimeManager.getCurrentGameTimeTicks()
		server.worlds.forEach { world ->
			setWorldTime(world, gameTime)
		}
	}

	private fun setWorldTime(world: ServerWorld, timeOfDay: Long) {
		val currentDay = world.timeOfDay / 24000L
		world.timeOfDay = currentDay * 24000L + timeOfDay
	}

	private fun checkAgeLimits(server: MinecraftServer) {
		server.playerManager.playerList.forEach { player ->
			LifeSystemManager.checkAgeLimitAndKill(player)
		}
	}
}