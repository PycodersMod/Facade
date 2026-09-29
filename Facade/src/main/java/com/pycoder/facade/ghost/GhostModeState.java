package com.pycoder.facade.ghost;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

public final class GhostModeState {
    private static final Set<UUID> GHOST_PLAYERS = ConcurrentHashMap.newKeySet();

    private GhostModeState() {
    }

    public static boolean isGhost(Player player) {
        return GHOST_PLAYERS.contains(player.getUUID());
    }

    public static void setGhost(Player player, boolean ghost) {
        if (ghost) {
            GHOST_PLAYERS.add(player.getUUID());
        } else {
            GHOST_PLAYERS.remove(player.getUUID());
        }
    }

    public static void clear(Player player) {
        GHOST_PLAYERS.remove(player.getUUID());
    }
}
