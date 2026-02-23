package com.ekvicancode.asrealasyou.comps

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import kotlin.math.max

data class PlayerLife(val daysLived: Long = 0, val totalLives: Int = 3) {
    fun writeNbt(tag: NbtCompound): NbtCompound {
        tag.putLong("DaysLived", daysLived)
        tag.putInt("TotalLives", totalLives)
        return tag
    }

    companion object {
        fun fromNbt(tag: NbtCompound): PlayerLife {
            return PlayerLife(
                tag.getLong("DaysLived"),
                tag.getInt("TotalLives")
            )
        }
    }
}
