package com.pycoder.facadebypycoder.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pycoder.facadebypycoder.FacadeByPycoder;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FacadeByPycoder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GhostModeKeyEvents {
    public static final KeyMapping GHOST_MODE_KEY = new KeyMapping(
            "key.facadebypycoder.ghost_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.facadebypycoder");

    private GhostModeKeyEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GHOST_MODE_KEY);
    }
}
