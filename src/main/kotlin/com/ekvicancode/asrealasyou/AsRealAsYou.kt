package com.ekvicancode.asrealasyou

import com.ekvicancode.asrealasyou.commands.ModCommands
import com.ekvicancode.asrealasyou.crafting.CustomRecipeManager
import com.ekvicancode.asrealasyou.crafting.ModRecipeManager
import com.ekvicancode.asrealasyou.crafting.PlayerCraftingData
import com.ekvicancode.asrealasyou.crafting.RecipeVariantLoader
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
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeEntry
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.world.GameRules
import org.slf4j.LoggerFactory

object AsRealAsYou : ModInitializer {

	const val MOD_ID = "asrealasyou"
	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	private const val SYNC_INTERVAL = 5
	private const val SAVE_INTERVAL = 6000
	private var tickCounter = 0

	var recipeManager: CustomRecipeManager? = null
		private set
	var playerCraftingData: PlayerCraftingData? = null
		private set

	override fun onInitialize() {
		LOGGER.info("AsRealAsYou initializing...")

		playerCraftingData = PlayerCraftingData()
		recipeManager = CustomRecipeManager()

		SyncPackets.registerServer()

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			ModCommands.register(dispatcher)
		}

		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			LifeSystemManager.init(server)
			disableDaylightCycle(server)
			RealTimeManager.initBase()

			RecipeVariantLoader.loadAllVariants()
			removeVanillaRecipesForVariants(server)
			playerCraftingData?.load(server)
			LOGGER.info("Loaded {} recipe variant files", RecipeVariantLoader.loadedCount)

			ModRecipeManager.registerRecipes()

			LOGGER.info("Server started, time initialized")
		}

		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			LOGGER.info("Saving all players...")
			val players = server.playerManager.playerList
			LifeSystemManager.flushAllPlayers(players)
			players.forEach { LifeSystemManager.savePlayer(it) }
			playerCraftingData?.save(server)
		}

		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			val player = handler.player
			LifeSystemManager.loadPlayer(player)

			val craftData = playerCraftingData
			val rm = recipeManager
			if (craftData != null && rm != null) {
				rm.ensurePlayerHasRecipes(player, craftData)
				rm.syncRecipesForPlayer(player)
				SyncPackets.sendRecipeData(player)
			}

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
				playerCraftingData?.save(server)
			}
		}

		LOGGER.info("AsRealAsYou initialized!")
	}

	private fun onPlayerDied(player: ServerPlayerEntity) {
		LifeSystemManager.onPlayerDeath(player)

		val craftData = playerCraftingData
		val rm = recipeManager
		if (craftData != null && rm != null) {
			LOGGER.info("Player {} died, shuffling recipes", player.name.string)
			craftData.incrementDeathCount(player.uuid)
			rm.reshuffleRecipes(player, craftData)
			player.server?.let { craftData.save(it) }
		}

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

	private fun removeVanillaRecipesForVariants(server: MinecraftServer) {
		val recipeManager = server.recipeManager
		val recipesField = try {
			recipeManager::class.java.getDeclaredField("recipes")
		} catch (e: NoSuchFieldException) {
			recipeManager::class.java.getDeclaredField("recipesById") // fallback для старых версий
		}
		recipesField.isAccessible = true

		@Suppress("UNCHECKED_CAST")
		val originalRecipes = recipesField.get(recipeManager) as Map<Identifier, RecipeEntry<*>>

		val mutableRecipes = originalRecipes.toMutableMap()

		val toRemove = mutableListOf<Identifier>()
		for ((id, entry) in mutableRecipes) {
			if (id.namespace != "minecraft") continue
			val recipe = entry.value()
			if (recipe is CraftingRecipe) {
				val result = recipe.getResult(server.registryManager)
				val itemId = Registries.ITEM.getId(result.item)
				if (RecipeVariantLoader.hasVariants(itemId)) {
					toRemove.add(id)
				}
			}
		}

		toRemove.forEach {
			mutableRecipes.remove(it)
			LOGGER.info("Removed vanilla recipe: {}", it)
		}

		recipesField.set(recipeManager, mutableRecipes)
	}
}