package com.pycoder.facade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pycoder.facade.block.entity.FacadeExtensionBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class FacadeExtensionBlockEntityRenderer implements BlockEntityRenderer<FacadeExtensionBlockEntity> {
    public FacadeExtensionBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FacadeExtensionBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.isVisible()) {
            return;
        }
        if (FacadeSpecialRenderers.renderSpecialBlock(blockEntity.getDisplayState(), poseStack, bufferSource, packedLight)) {
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockEntity.getDisplayState(), poseStack, bufferSource, packedLight, packedOverlay);
    }
}
