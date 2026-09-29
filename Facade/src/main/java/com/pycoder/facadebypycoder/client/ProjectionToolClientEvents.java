package com.pycoder.facadebypycoder.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pycoder.facadebypycoder.FacadeByPycoder;
import com.pycoder.facadebypycoder.block.FacadeBlocks;
import java.io.File;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

@Mod.EventBusSubscriber(modid = FacadeByPycoder.MOD_ID, value = Dist.CLIENT)
public final class ProjectionToolClientEvents {
    private static BlockPos leftPos;
    private static BlockPos rightPos;
    private static long lastHandledGameTime = Long.MIN_VALUE;
    private static boolean lastHandledAttack;

    private ProjectionToolClientEvents() {
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        ItemStack stack = minecraft.player.getItemInHand(event.getHand());
        if (!isProjectionTool(stack)) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(true);
        if (alreadyHandledThisTick(minecraft, event.isAttack())) {
            return;
        }
        if (event.isAttack()) {
            setLeftPos(minecraft.player, event.getHand(), hitResult.getBlockPos());
            return;
        }
        if (!event.isUseItem()) {
            return;
        }
        if (minecraft.player.isShiftKeyDown() && leftPos != null && rightPos != null) {
            openSaveDialogAndExport(leftPos, rightPos);
            return;
        }
        setRightPos(minecraft.player, event.getHand(), hitResult.getBlockPos());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide() || !isProjectionTool(event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || alreadyHandledThisTick(minecraft, true)) {
            return;
        }
        setLeftPos(event.getEntity(), event.getHand(), event.getPos());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || !isProjectionTool(event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || alreadyHandledThisTick(minecraft, false)) {
            return;
        }
        if (event.getEntity().isShiftKeyDown() && leftPos != null && rightPos != null) {
            event.getEntity().swing(event.getHand());
            openSaveDialogAndExport(leftPos, rightPos);
            return;
        }
        setRightPos(event.getEntity(), event.getHand(), event.getPos());
    }

    private static boolean alreadyHandledThisTick(Minecraft minecraft, boolean attack) {
        long gameTime = minecraft.level == null ? Long.MIN_VALUE : minecraft.level.getGameTime();
        if (lastHandledGameTime == gameTime && lastHandledAttack == attack) {
            return true;
        }
        lastHandledGameTime = gameTime;
        lastHandledAttack = attack;
        return false;
    }

    private static void setLeftPos(Player player, InteractionHand hand, BlockPos pos) {
        leftPos = pos.immutable();
        player.swing(hand);
        player.displayClientMessage(Component.translatable("message.facadebypycoder.projection.pos1", format(leftPos)), true);
    }

    private static void setRightPos(Player player, InteractionHand hand, BlockPos pos) {
        rightPos = pos.immutable();
        player.swing(hand);
        player.displayClientMessage(Component.translatable("message.facadebypycoder.projection.pos2", format(rightPos)), true);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            leftPos = null;
            rightPos = null;
            return;
        }
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 cameraPosition = event.getCamera().getPosition();
        RenderSystem.disableDepthTest();
        if (leftPos != null && rightPos != null) {
            renderSelectionArea(event.getPoseStack(), bufferSource, cameraPosition, leftPos, rightPos);
        }
        if (leftPos != null) {
            renderSelectionBox(event.getPoseStack(), bufferSource, cameraPosition, leftPos, 1.0F, 0.0F, 0.0F, 0.9F);
        }
        if (rightPos != null) {
            renderSelectionBox(event.getPoseStack(), bufferSource, cameraPosition, rightPos, 0.0F, 0.35F, 1.0F, 0.9F);
        }
        bufferSource.endBatch(RenderType.debugFilledBox());
        bufferSource.endBatch(RenderType.lines());
        RenderSystem.enableDepthTest();
    }

    private static boolean isProjectionTool(ItemStack stack) {
        return stack.is(FacadeBlocks.PROJECTION_TOOL);
    }

    private static String format(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static void renderSelectionBox(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPosition, BlockPos pos, float red, float green, float blue, float alpha) {
        AABB box = new AABB(pos).move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z).inflate(0.004D);
        LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), box, red, green, blue, alpha);
    }

    private static void renderSelectionArea(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPosition, BlockPos firstPos, BlockPos secondPos) {
        int minX = Math.min(firstPos.getX(), secondPos.getX());
        int minY = Math.min(firstPos.getY(), secondPos.getY());
        int minZ = Math.min(firstPos.getZ(), secondPos.getZ());
        int maxX = Math.max(firstPos.getX(), secondPos.getX()) + 1;
        int maxY = Math.max(firstPos.getY(), secondPos.getY()) + 1;
        int maxZ = Math.max(firstPos.getZ(), secondPos.getZ()) + 1;
        AABB area = new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z).inflate(0.002D);
        DebugRenderer.renderFilledBox(poseStack, bufferSource, area, 0.2F, 0.8F, 1.0F, 0.16F);
        LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), area, 0.2F, 0.8F, 1.0F, 0.65F);
    }

    private static void openSaveDialogAndExport(BlockPos firstPos, BlockPos secondPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        File file = chooseExportFile();
        if (file == null) {
            return;
        }
        try {
            ProjectionExporter.export(minecraft.level, firstPos, secondPos, file.toPath());
            minecraft.player.displayClientMessage(Component.translatable("message.facadebypycoder.projection.export_success", file.getAbsolutePath()), false);
        } catch (IOException exception) {
            minecraft.player.displayClientMessage(Component.translatable("message.facadebypycoder.projection.export_failed", exception.getMessage()), false);
            FacadeByPycoder.LOGGER.error("Failed to export projection", exception);
        }
    }

    private static File chooseExportFile() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer patterns = stack.mallocPointer(4);
            patterns.put(stack.UTF8("*.schem"));
            patterns.put(stack.UTF8("*.schematic"));
            patterns.put(stack.UTF8("*.nbt"));
            patterns.put(stack.UTF8("*.litematic"));
            patterns.flip();
            String selectedPath = TinyFileDialogs.tinyfd_saveFileDialog(
                    Component.translatable("dialog.facadebypycoder.projection_export").getString(),
                    "facade_projection.schem",
                    patterns,
                    "Projection files (*.schem; *.schematic; *.nbt; *.litematic)"
            );
            return selectedPath == null ? null : new File(ProjectionExporter.ensureKnownExtension(selectedPath));
        }
    }
}
