package com.pycoder.facadebypycoder.item;

import com.pycoder.facadebypycoder.block.FacadeBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ProjectionToolEvents {
    private ProjectionToolEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(FacadeBlocks.PROJECTION_TOOL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(FacadeBlocks.PROJECTION_TOOL)) {
            event.setCanceled(true);
        }
    }
}
