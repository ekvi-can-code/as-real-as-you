package com.ekvicancode.asrealasyou.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class RecipeSyncPayload(
    val assignedVariants: Map<String, Int>
) : CustomPayload {

    companion object {
        val ID: CustomPayload.Id<RecipeSyncPayload> = CustomPayload.Id(
            Identifier.of("asrealasyou", "recipe_sync")
        )

        val CODEC: PacketCodec<PacketByteBuf, RecipeSyncPayload> =
            PacketCodec.of(
                { value, buf ->
                    buf.writeVarInt(value.assignedVariants.size)
                    for ((key, variantId) in value.assignedVariants) {
                        buf.writeString(key)
                        buf.writeVarInt(variantId)
                    }
                },
                { buf ->
                    val size = buf.readVarInt()
                    val map = mutableMapOf<String, Int>()
                    repeat(size) {
                        map[buf.readString()] = buf.readVarInt()
                    }
                    RecipeSyncPayload(map)
                }
            )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}