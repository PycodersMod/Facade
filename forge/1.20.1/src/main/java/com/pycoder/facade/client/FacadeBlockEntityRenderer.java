package com.pycoder.facade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pycoder.facade.block.SimpleFacadeBlock;
import com.pycoder.facade.block.entity.FacadeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class FacadeBlockEntityRenderer implements BlockEntityRenderer<FacadeBlockEntity> {
    public FacadeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FacadeBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(blockEntity.getBlockState().getBlock() instanceof SimpleFacadeBlock facadeBlock)) {
            return;
        }
        BlockState displayState = blockEntity.getDisplayState();
        if (FacadeSpecialRenderers.renderSpecialBlock(displayState, poseStack, bufferSource, packedLight)) {
            return;
        }
        if (facadeBlock.definition().hasExtendedPistonHead(displayState)) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(displayState, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (displayState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockState lowerState = displayState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(lowerState, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(displayState, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
