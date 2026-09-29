package com.pycoder.facadebypycoder.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pycoder.facadebypycoder.block.SimpleFacadeBlock;
import com.pycoder.facadebypycoder.item.FacadeBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class FacadeItemRenderer extends BlockEntityWithoutLevelRenderer {
    public FacadeItemRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof FacadeBlockItem facadeItem) || !(facadeItem.getBlock() instanceof SimpleFacadeBlock facadeBlock)) {
            return;
        }
        BlockState displayState = facadeBlock.definition().displayState(FacadeBlockItem.getFacadeState(stack));
        if (FacadeSpecialRenderers.renderSpecialBlock(displayState, poseStack, bufferSource, packedLight)) {
            return;
        }
        if (facadeBlock.definition().hasExtendedPistonHead(displayState)) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(displayState, poseStack, bufferSource, packedLight, packedOverlay);
            net.minecraft.core.Direction direction = facadeBlock.definition().pistonHeadDirection(displayState);
            poseStack.pushPose();
            poseStack.translate(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(facadeBlock.definition().pistonHeadState(displayState), poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(displayState, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
