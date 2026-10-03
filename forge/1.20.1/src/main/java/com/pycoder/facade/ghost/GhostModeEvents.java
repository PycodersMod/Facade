package com.pycoder.facade.ghost;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class GhostModeEvents {
    private GhostModeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        GhostModeState.clear(event.getEntity());
    }
}
