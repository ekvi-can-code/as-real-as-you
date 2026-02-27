package com.ekvicancode.asrealasyou.HUD

import com.ekvicancode.asrealasyou.network.ClientLifeData
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.render.RenderTickCounter
import org.joml.Quaternionf
import org.joml.Vector3f
import java.time.LocalTime
@Environment(EnvType.CLIENT)
object PassportHUD {
    var isVisible = false

    private const val PASSPORT_WIDTH = 260
    private const val PASSPORT_HEIGHT = 160

    private const val PADDING = 10

    fun register() {
        HudRenderCallback.EVENT.register { drawContext, tickCounter ->
            render(drawContext, tickCounter)
        }
    }
    private fun worldTicksToTimeString(worldTimeTicks: Long): String {
        val dayTicks     = worldTimeTicks % 24_000L
        val shiftedTicks = (dayTicks + 6_000L) % 24_000L
        val totalSeconds = shiftedTicks * 86_400L / 24_000L

        val hours   = (totalSeconds / 3_600L) % 24L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun render(drawContext: DrawContext, tickCounter: RenderTickCounter) {
        if (!isVisible) return

        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val textRenderer = client.textRenderer

        val screenWidth = client.window.scaledWidth
        val screenHeight = client.window.scaledHeight

        val passportX = (screenWidth - PASSPORT_WIDTH) / 2
        val passportY = (screenHeight - PASSPORT_HEIGHT) / 2

        drawContext.fill(
            passportX - 3,
            passportY - 3,
            passportX + PASSPORT_WIDTH + 3,
            passportY + PASSPORT_HEIGHT + 3,
            0xFF1A3A1A.toInt()
        )

        drawContext.fill(
            passportX,
            passportY,
            passportX + PASSPORT_WIDTH,
            passportY + PASSPORT_HEIGHT,
            0xFF2D5A2D.toInt()
        )

        val textAreaX = passportX + 90
        drawContext.fill(
            textAreaX - 4,
            passportY + 8,
            passportX + PASSPORT_WIDTH - 8,
            passportY + PASSPORT_HEIGHT - 8,
            0xCC000000.toInt()
        )

        drawContext.fill(
            passportX + 5,
            passportY + 8,
            passportX + 85,
            passportY + PASSPORT_HEIGHT - 8,
            0xCC000000.toInt()
        )

        drawContext.drawCenteredTextWithShadow(
            textRenderer,
            "§6§lПАСПОРТ ИГРОКА",
            passportX + PASSPORT_WIDTH / 2,
            passportY - 14,
            0xFFD700 // золотой
        )

        val modelCenterX = passportX + 45
        val modelCenterY = passportY + PASSPORT_HEIGHT - 20

        val mouseX: Float
        val mouseY: Float

        if (client.mouse.isCursorLocked) {
            mouseX = modelCenterX.toFloat()
            mouseY = (passportY + PASSPORT_HEIGHT / 2).toFloat()
        } else {
            mouseX = client.mouse.x.toFloat() / client.window.scaleFactor.toFloat()
            mouseY = client.mouse.y.toFloat() / client.window.scaleFactor.toFloat()
        }

        InventoryScreen.drawEntity(
            drawContext,
            passportX + 5,          // x1 - левая граница области
            passportY + 8,          // y1 - верхняя граница
            passportX + 85,         // x2 - правая граница
            passportY + PASSPORT_HEIGHT - 8, // y2 - нижняя граница
            35,                     // scale - масштаб модели
            0f,                     // delta - вертикальное смещение
            mouseX,                 // X - для поворота головы
            mouseY,                 // Y - для поворота головы
            player                  // игрок
        )

        val textX = textAreaX
        var textY = passportY + 14

        drawContext.drawTextWithShadow(
            textRenderer,
            "§7Имя:",
            textX, textY, 0xAAAAAA
        )
        textY += 9

        drawContext.drawTextWithShadow(
            textRenderer,
            "§f§l${player.name.string}",
            textX, textY, 0xFFFFFF
        )
        textY += 14

        drawContext.fill(
            textX, textY,
            passportX + PASSPORT_WIDTH - 10, textY + 1,
            0x66FFFFFF
        )
        textY += 5

        val days = ClientLifeData.ageDays
        val years = ClientLifeData.ageYears

        drawContext.drawTextWithShadow(
            textRenderer,
            "§7Возраст:",
            textX, textY, 0xAAAAAA
        )
        textY += 9

        drawContext.drawTextWithShadow(
            textRenderer,
            "§e$years лет",
            textX, textY, 0xFFFFFF
        )
        textY += 9

        drawContext.drawTextWithShadow(
            textRenderer,
            "§e($days дней)",
            textX, textY, 0xFFFFFF
        )
        textY += 14

        //drawContext.drawTextWithShadow(
        //    textRenderer,
        //    "§8[debug] ticks=${ClientLifeData.ageTicks} spd=${ClientLifeData.receivedDaySpeed}",
        //    textX, textY + 100, 0xAAAAAA
        //)

        drawContext.fill(
            textX, textY,
            passportX + PASSPORT_WIDTH - 10, textY + 1,
            0x66FFFFFF
        )
        textY += 5

        drawContext.drawTextWithShadow(
            textRenderer,
            "§7Перерождений:",
            textX, textY, 0xAAAAAA
        )
        textY += 9

        val deathColor = when {
            ClientLifeData.totalDeaths < 3 -> 0x55FF55 // зелёный — ни разу не умирал
            ClientLifeData.totalDeaths < 10  -> 0xFFFF55 // жёлтый
            else                            -> 0xFF5555 // красный — много смертей
        }

        drawContext.drawTextWithShadow(
            textRenderer,
            "${ClientLifeData.totalDeaths}",
            textX, textY, deathColor
        )
        textY += 14

        drawContext.fill(
            textX, textY,
            passportX + PASSPORT_WIDTH - 10, textY + 1,
            0x66FFFFFF
        )
        textY += 5

        val timeStr = worldTicksToTimeString(client.world?.timeOfDay ?: 0L)

        drawContext.drawTextWithShadow(
            textRenderer,
            "§7Время:",
            textX, textY, 0xAAAAAA
        )
        textY += 9

        drawContext.drawTextWithShadow(
            textRenderer,
            "§b$timeStr",
            textX, textY, 0xFFFFFF
        )
        textY += 14
    }
}