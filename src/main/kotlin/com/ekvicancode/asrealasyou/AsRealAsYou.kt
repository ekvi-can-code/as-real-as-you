package com.ekvicancode.asrealasyou

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

object AsRealAsYou : ModInitializer {
	var realDayTime: Long = 0
	private var keyBinding: KeyBinding? = null

	override fun onInitialize() {
		if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.isClient()) return

		realDayTime = System.currentTimeMillis()

		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.forEach { handler, _, _ -> }

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register { server ->
			val world = server.overworld
			if (world != null) {
				val players = world.players
				players.forEach { player -> }
				realDayTime += 1
			}
		}
	}

	fun incrementRealDay() {
		realDayTime += 24000
	}
}
