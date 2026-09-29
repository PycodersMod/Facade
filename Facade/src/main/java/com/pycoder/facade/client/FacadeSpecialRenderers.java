package com.pycoder.facade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class FacadeSpecialRenderers {
    private static Map<SkullBlock.Type, SkullModelBase> skullModels;

    private FacadeSpecialRenderers() {
    }

    public static boolean renderSpecialBlock(BlockState displayState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (displayState.getBlock() instanceof AbstractSkullBlock skullBlock) {
            renderSkull(displayState, skullBlock, poseStack, bufferSource, packedLight);
            return true;
        }
        return false;
    }

    private static void renderSkull(BlockState displayState, AbstractSkullBlock skullBlock, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Map<SkullBlock.Type, SkullModelBase> models = skullModels();
        SkullModelBase model = models.get(skullBlock.getType());
        if (model == null) {
            return;
        }
        Direction direction = null;
        float rotation = 0.0F;
        if (displayState.getBlock() instanceof WallSkullBlock && displayState.hasProperty(WallSkullBlock.FACING)) {
            direction = displayState.getValue(WallSkullBlock.FACING);
        } else if (displayState.hasProperty(SkullBlock.ROTATION)) {
            rotation = displayState.getValue(SkullBlock.ROTATION) * 22.5F;
        } else if (displayState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            direction = displayState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        RenderType renderType = SkullBlockRenderer.getRenderType(skullBlock.getType(), null);
        SkullBlockRenderer.renderSkull(direction, rotation, 0.0F, poseStack, bufferSource, packedLight, model, renderType);
    }

    private static Map<SkullBlock.Type, SkullModelBase> skullModels() {
        if (skullModels == null) {
            skullModels = SkullBlockRenderer.createSkullRenderers(Minecraft.getInstance().getEntityModels());
        }
        return skullModels;
    }
}
