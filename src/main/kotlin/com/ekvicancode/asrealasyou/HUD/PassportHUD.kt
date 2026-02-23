package com.ekvicancode.asrealasyou.HUD

import com.ekvicancode.asrealasyou.AsRealAsYou
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.GameRenderer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object PassportHUD : HudRenderCallback {
    private val TEXTURE = Identifier("asrealasyou", "textures/gui/passport.png")

    override fun onHudRender(drawContext: DrawContext, tickDelta: Float) {
        val client = MinecraftClient.getInstance()
        if (client.player == null || !AsRealAsYou.keyBinding?.wasPressed()!!) return

        val player = client.player!!
        val width = client.window.scaledWidth
        val height = client.window.scaledHeight

        RenderSystem.setShader { GameRenderer::getPositionTexProgram }
        RenderSystem.setShaderTexture(0, TEXTURE)
        drawContext.drawTexture(TEXTURE, width / 2 - 100, height / 2 - 100, 0f, 0f, 200, 200, 200, 200)

        drawContext.drawCenteredTextWithShadow(client.textRenderer, player.name.string, width / 2, height / 2 - 60, 0xFFFFFF)

        val stats = player.getStatHandler().stats
        drawContext.drawText(client.textRenderer, "Days lived: ${AsRealAsYou.realDayTime / 24000}", width / 2 - 80, height / 2 - 30, 0xFFFFFF)
    }
}
