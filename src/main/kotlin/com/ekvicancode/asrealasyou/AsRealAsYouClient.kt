package com.ekvicancode.asrealasyou

import com.ekvicancode.asrealasyou.HUD.KeyBindings
import com.ekvicancode.asrealasyou.HUD.PassportHUD
import com.ekvicancode.asrealasyou.network.ClientPacketHandler
import com.ekvicancode.asrealasyou.network.SyncPackets
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.CLIENT)
object AsRealAsYouClient : ClientModInitializer {

    override fun onInitializeClient() {
        ClientPacketHandler.register()
        KeyBindings.register()
        PassportHUD.register()
    }
}
