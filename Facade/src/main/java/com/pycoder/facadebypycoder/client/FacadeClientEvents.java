package com.pycoder.facadebypycoder.client;

import com.pycoder.facadebypycoder.FacadeByPycoder;
import com.pycoder.facadebypycoder.block.FacadeBlockEntityTypes;
import com.pycoder.facadebypycoder.block.FacadeBlocks;
import com.pycoder.facadebypycoder.block.FacadeDefinition;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FacadeByPycoder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FacadeClientEvents {
    private FacadeClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FacadeBlockEntityTypes.FACADE_BLOCK_ENTITY_TYPE, FacadeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(FacadeBlockEntityTypes.FACADE_EXTENSION_BLOCK_ENTITY_TYPE, FacadeExtensionBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModel fallbackModel = event.getModels().get(ModelBakery.MISSING_MODEL_LOCATION);
        if (fallbackModel == null) {
            return;
        }
        for (FacadeDefinition definition : FacadeBlocks.definitions()) {
            ModelResourceLocation modelLocation = new ModelResourceLocation(definition.facadeId(), "inventory");
            event.getModels().put(modelLocation, new FacadeItemBakedModel(fallbackModel));
        }
    }
}
