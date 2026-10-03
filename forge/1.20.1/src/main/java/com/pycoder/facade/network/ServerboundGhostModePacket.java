package com.pycoder.facade.network;

import com.pycoder.facade.ghost.GhostModeState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundGhostModePacket(boolean enabled) {
    public static void encode(ServerboundGhostModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
    }

    public static ServerboundGhostModePacket decode(FriendlyByteBuf buffer) {
        return new ServerboundGhostModePacket(buffer.readBoolean());
    }

    public static void handle(ServerboundGhostModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                GhostModeState.setGhost(player, packet.enabled);
            }
        });
        context.setPacketHandled(true);
    }
}
