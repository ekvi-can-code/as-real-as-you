package com.ekvicancode.asrealasyou.comps

import net.fabricmc.fabric.api.gametest.v1.FabricGameTestModInitializer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

data class LifeStats(
        val daysLived: Long = 0,
        val yearsLived: Long = 0,
        val maxAgeDays: Long = 29200 // 80 лет
)

class PlayerLife : Component<PlayerEntity> {
    companion object {
        val ID = Identifier("asrealasyou:life")
        fun register() = ComponentRegistry.PLAYER.registerForPlayers(ID, ::PlayerLife)
    }

    private var stats = LifeStats()

    override fun writeNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup): NbtCompound {
        nbt.putLong("DaysLived", stats.daysLived)
        nbt.putLong("YearsLived", stats.yearsLived)
        return nbt
    }

    override fun readNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup) {
        stats = LifeStats(nbt.getLong("DaysLived"), nbt.getLong("YearsLived"))
    }

    fun getStats() = stats
    fun incrementRealDay() {
        stats = stats.copy(
                daysLived = stats.daysLived + 1,
                yearsLived = (stats.daysLived + 1) / 365
        )
    }

    fun checkDeath(): Boolean = stats.daysLived >= stats.maxAgeDays
    fun resetLife() { stats = LifeStats() }
}
