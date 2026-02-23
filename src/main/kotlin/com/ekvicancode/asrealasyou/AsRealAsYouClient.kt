package com.ekvicancode.asrealasyou

import com.ekvicancode.asrealasyou.HUD.PassportHUD
import com.ekvicancode.asrealasyou.network.ClientPacketHandler
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.CLIENT)
object AsRealAsYouClient : ClientModInitializer {

    override fun onInitializeClient() {
        AsRealAsYou.LOGGER.info("AsRealAsYou client initializing...")
        ClientPacketHandler.register()
        PassportHUD.register()
        AsRealAsYou.LOGGER.info("AsRealAsYou client initialized!")
    }
}