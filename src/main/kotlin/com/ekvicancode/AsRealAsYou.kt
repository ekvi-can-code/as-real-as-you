package com.ekvicancode

import com.ekvicancode.comps.PlayerLife
import com.ekvicancode.HUD.PassportHUD
import com.ekvi.network.SyncNetwork
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

@Mod("as-real-as-you")
object AsRealAsYouMod : ModInitializer {
	lateinit var PASSPORT_KEY: KeyBinding

	override fun onInitialize() {
		PlayerLife.register()
		SyncNetwork.register()

		ServerTickEvents.END_WORLD_TICK.register { world ->
			if (!world.isClient && world.server?.isRunning) {
				if (world.time % (20 * 60 * 60) == 0L) {
					world.players.forEach { player ->
						PlayerLife.ID.get(player).incrementRealDay()
						SyncNetwork.sendLifeData(player)
					}
				}
			}
		}

		PASSPORT_KEY = KeyBindingHelper.registerKeyBinding(
				KeyBinding("key.asrealasyou.passport", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.asrealasyou")
		)

		HudRenderCallback.EVENT.register(PassportHUD::render)
		ClientTickEvents.END_CLIENT_TICK.register { client ->
			while (PASSPORT_KEY.wasPressed()) PassportHUD.toggle()
		}
	}
}
