package com.pycoder.facadebypycoder.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pycoder.facadebypycoder.FacadeByPycoder;
import com.pycoder.facadebypycoder.block.FacadeExtensionBlock;
import com.pycoder.facadebypycoder.block.SimpleFacadeBlock;
import com.pycoder.facadebypycoder.config.ModConfig;
import com.pycoder.facadebypycoder.ghost.GhostModeState;
import com.pycoder.facadebypycoder.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FacadeByPycoder.MOD_ID, value = Dist.CLIENT)
public final class GhostModeClientEvents {
    private static boolean ghostMode;

    private GhostModeClientEvents() {
    }

    public static boolean isGhostMode() {
        return ghostMode;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            setGhostMode(false, false);
            return;
        }
        while (GhostModeKeyEvents.GHOST_MODE_KEY.consumeClick()) {
            setGhostMode(!ghostMode, true);
            minecraft.player.displayClientMessage(Component.translatable(ghostMode ? "message.facadebypycoder.ghost_mode.enabled" : "message.facadebypycoder.ghost_mode.disabled"), true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!ghostMode || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        RenderSystem.disableDepthTest();
        renderNearbyFacadeOutlines(level, player, poseStack, cameraPosition, bufferSource);
        bufferSource.endBatch(RenderType.debugFilledBox());
        bufferSource.endBatch(RenderType.lines());
        RenderSystem.enableDepthTest();
    }

    private static void setGhostMode(boolean enabled, boolean sendToServer) {
        if (ghostMode == enabled) {
            return;
        }
        ghostMode = enabled;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            GhostModeState.setGhost(minecraft.player, enabled);
        }
        if (sendToServer) {
            ModNetwork.sendGhostMode(enabled);
        }
    }

    private static void renderNearbyFacadeOutlines(BlockGetter level, Player player, PoseStack poseStack, Vec3 cameraPosition, MultiBufferSource bufferSource) {
        int radius = 6;
        BlockPos playerPos = player.blockPosition();
        CollisionContext context = CollisionContext.of(player);
        float alpha = (float) ModConfig.GHOST_ALPHA.get().doubleValue();
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-radius, -radius, -radius), playerPos.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SimpleFacadeBlock) && !(state.getBlock() instanceof FacadeExtensionBlock)) {
                continue;
            }
            VoxelShape shape = state.getShape(level, pos, context);
            if (shape.isEmpty()) {
                continue;
            }
            for (AABB box : shape.toAabbs()) {
                AABB moved = box.move(pos).move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z).inflate(0.002D);
                DebugRenderer.renderFilledBox(poseStack, bufferSource, moved, 0.0F, 1.0F, 0.0F, alpha * 0.35F);
                LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), moved, 0.0F, 1.0F, 0.0F, Math.max(alpha, 0.15F));
            }
        }
    }
}
