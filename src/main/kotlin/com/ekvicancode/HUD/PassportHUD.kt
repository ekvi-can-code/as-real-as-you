package com.ekvicancode.HUD

import com.ekvicancode.AsRealAsYouMod
import com.ekvicancode.comps.PlayerLife
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis

object PassportHUD {
    private val texture = Identifier("asrealasyou:textures/gui/passport.png")
    var isOpen = false

    fun toggle() { isOpen = !isOpen }

    fun render(context: DrawContext, tickDelta: Float) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        renderMiniInfo(context, player)

        if (isOpen) renderPassport(context, player, tickDelta)
    }

    private fun renderMiniInfo(context: DrawContext, player: PlayerEntity) {
        val lifeComp = PlayerLife.ID.get(player)
        val stats = lifeComp.getStats()

        val x = context.scaledWindowWidth - 150
        context.drawTextWithShadow(client.textRenderer,
                "${stats.yearsLived}лет ${stats.daysLived}д", x, 10, 0xFFFF00)
    }

    private fun renderPassport(context: DrawContext, player: PlayerEntity, tickDelta: Float) {
        val matrices = context.matrices
        val client = MinecraftClient.getInstance()

        val width = 256
        val height = 160
        val x = (context.scaledWindowWidth - width) / 2
        val y = (context.scaledWindowHeight - height) / 2

        matrices.push()
        matrices.translate(x.toDouble(), y.toDouble(), 0.0)

        context.drawTexture(texture, 0, 0, 0f, 0f, width, height, width, height)

        context.drawCenteredTextWithShadow(client.textRenderer, player.name.string, 128, 20, 0x000000)

        val lifeComp = PlayerLife.ID.get(player)
        val stats = lifeComp.getStats()
        val worldName = client.world?.registryKey?.value?.path ?: "хз"

        context.drawText(client.textRenderer, "Страна: $worldName", 20, 50, 0x222222)
        context.drawText(client.textRenderer, "Лет: ${stats.yearsLived}", 20, 70, 0x222222)
        context.drawText(client.textRenderer, "Дней: ${stats.daysLived}", 20, 85, 0x222222)

        renderPlayerModel(context, player, 128, 80, tickDelta)

        matrices.pop()
    }

    private fun renderPlayerModel(context: DrawContext, player: PlayerEntity, x: Int, y: Int, tickDelta: Float) {
        val matrices = context.getMatrices()
        matrices.push()

        matrices.translate(x.toDouble(), y.toDouble(), 50.0)
        matrices.scale(-20f, -20f, 20f)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f))
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(System.currentTimeMillis() % 360 * 0.05f.toFloat()))

        val renderDispatcher = MinecraftClient.getInstance().entityRenderDispatcher
        renderDispatcher.setRenderShadows(false)

        val renderLayerParent = player
        val playerRenderer = renderDispatcher.getRenderer(player)!!

        playerRenderer.setupTransforms(
                renderDispatcher.camera,
                player.pose().getForFirstPerson(playerRenderer.model),
                player.age + tickDelta,
                player.headYaw,
                0f,
                0f,
                player.pitch,
                player.bodyYaw,
                player.headYaw
        )

        playerRenderer.render(player, 0f, 0f, 0f, 0f, 0f, matrices, context.getVertexConsumers(), 0xF000F0)

        matrices.pop()
        renderDispatcher.setRenderShadows(true)
    }
}