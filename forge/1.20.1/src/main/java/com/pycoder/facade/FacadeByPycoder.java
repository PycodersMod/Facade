package com.pycoder.facade;

import com.mojang.logging.LogUtils;
import com.pycoder.facade.block.FacadeBlocks;
import com.pycoder.facade.command.FacadeCommands;
import com.pycoder.facade.config.ModConfig;
import com.pycoder.facade.ghost.GhostModeEvents;
import com.pycoder.facade.item.FacadeEffectIsolationEvents;
import com.pycoder.facade.item.ProjectionToolEvents;
import com.pycoder.facade.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FacadeByPycoder.MOD_ID)
public final class FacadeByPycoder {
    public static final String MOD_ID = "facadebypycoder";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FacadeByPycoder() {
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.COMMON_SPEC);
        ModNetwork.register();
        FMLJavaModLoadingContext.get().getModEventBus().register(FacadeBlocks.class);
        MinecraftForge.EVENT_BUS.register(FacadeCommands.class);
        MinecraftForge.EVENT_BUS.register(GhostModeEvents.class);
        MinecraftForge.EVENT_BUS.register(FacadeEffectIsolationEvents.class);
        MinecraftForge.EVENT_BUS.register(ProjectionToolEvents.class);
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Initializing FacadeByPycoder");
    }
}
