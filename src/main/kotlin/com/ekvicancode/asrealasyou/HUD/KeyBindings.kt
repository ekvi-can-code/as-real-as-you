package com.ekvicancode.asrealasyou.HUD

import com.ekvicancode.asrealasyou.HUD.PassportHUD
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

@Environment(EnvType.CLIENT)
object KeyBindings {
    private lateinit var passportKey: KeyBinding
    fun register() {
        passportKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.asrealasyou.passport",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.asrealasyou.general"
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            while (passportKey.wasPressed()) {
                PassportHUD.isVisible = !PassportHUD.isVisible
            }
        }
    }
}