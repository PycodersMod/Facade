package com.pycoder.facade.item;

import com.pycoder.facade.block.FacadeBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class FacadeEffectIsolationEvents {
    private FacadeEffectIsolationEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().is(FacadeBlocks.PROJECTION_TOOL)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.PASS);
        }
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getItemStack().is(FacadeBlocks.PROJECTION_TOOL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onApplicablePotion(MobEffectEvent.Applicable event) {
        if (event.getEntity().getMainHandItem().is(FacadeBlocks.PROJECTION_TOOL)
                || event.getEntity().getOffhandItem().is(FacadeBlocks.PROJECTION_TOOL)) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }
}
