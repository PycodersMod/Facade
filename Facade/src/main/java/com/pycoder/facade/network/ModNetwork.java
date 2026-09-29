package com.pycoder.facade.network;

import com.pycoder.facade.FacadeByPycoder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FacadeByPycoder.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ServerboundGhostModePacket.class, ServerboundGhostModePacket::encode, ServerboundGhostModePacket::decode, ServerboundGhostModePacket::handle);
    }

    public static void sendGhostMode(boolean enabled) {
        CHANNEL.sendToServer(new ServerboundGhostModePacket(enabled));
    }
}
