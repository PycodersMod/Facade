package com.pycoder.facadebypycoder.item;

import com.pycoder.facadebypycoder.client.FacadeItemRenderer;
import com.pycoder.facadebypycoder.block.FacadeBlocks;
import com.pycoder.facadebypycoder.block.SimpleFacadeBlock;
import com.pycoder.facadebypycoder.config.ModConfig;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public final class FacadeBlockItem extends BlockItem {
    public static final String TAG_STATE = "FacadeState";
    private static final ThreadLocal<BlockPlaceContext> LAST_PLACE_CONTEXT = new ThreadLocal<>();

    public FacadeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static int getFacadeState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_STATE)) {
            return 0;
        }
        return Math.max(0, tag.getInt(TAG_STATE));
    }

    public static void setFacadeState(ItemStack stack, int stateIndex) {
        stack.getOrCreateTag().putInt(TAG_STATE, Math.max(0, stateIndex));
    }

    @Nullable
    public static BlockPlaceContext getLastPlaceContext() {
        return LAST_PLACE_CONTEXT.get();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        LAST_PLACE_CONTEXT.set(context);
        try {
            return placeFacade(context);
        } finally {
            LAST_PLACE_CONTEXT.remove();
        }
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (getBlock() instanceof SimpleFacadeBlock facadeBlock) {
            int stateIndex = getFacadeState(context.getItemInHand());
            BlockState displayState = facadeBlock.definition().placedDisplayState(context, stateIndex);
            return facadeBlock.canPlaceExtensions(context.getLevel(), context.getClickedPos(), displayState, stateIndex);
        }
        return true;
    }

    @Override
    protected boolean mustSurvive() {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
    }

    private static boolean shouldNotConsume(BlockPlaceContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return false;
        }
        if (player.isCreative()) {
            return false;
        }
        GameType gameType = player.gameMode.getGameModeForPlayer();
        return gameType == GameType.SURVIVAL && ModConfig.ENABLE_IN_SURVIVAL.get()
                || gameType == GameType.ADVENTURE && ModConfig.ENABLE_IN_ADVENTURE.get();
    }

    private InteractionResult placeFacade(BlockPlaceContext context) {
        if (!getBlock().isEnabled(context.getLevel().enabledFeatures())) {
            return InteractionResult.FAIL;
        }
        BlockPlaceContext placeContext = placeContextForFacade(context);
        if (placeContext == null) {
            return InteractionResult.FAIL;
        }
        LAST_PLACE_CONTEXT.set(placeContext);
        BlockState placementState = getPlacementState(placeContext);
        if (placementState == null || !(getBlock() instanceof SimpleFacadeBlock facadeBlock)) {
            return InteractionResult.FAIL;
        }
        Level level = placeContext.getLevel();
        BlockPos pos = placeContext.getClickedPos();
        int stateIndex = getFacadeState(placeContext.getItemInHand());
        BlockState displayState = facadeBlock.definition().placedDisplayState(placeContext, stateIndex);
        if (!canPlace(placeContext, placementState) || !facadeBlock.canPlaceExtensions(level, pos, displayState, stateIndex)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!level.setBlock(pos, placementState, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        Player player = placeContext.getPlayer();
        ItemStack stack = placeContext.getItemInHand();
        placementState.getBlock().setPlacedBy(level, pos, placementState, player, stack);
        SoundType soundType = placementState.getSoundType(level, pos, player);
        level.playSound(player, pos, getPlaceSound(placementState, level, pos, player), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placementState));
        boolean shouldNotConsume = shouldNotConsume(placeContext);
        if ((player == null || !player.getAbilities().instabuild) && !shouldNotConsume) {
            stack.shrink(1);
        } else if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    private static BlockPlaceContext placeContextForFacade(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.isAir() || clickedState.liquid() || clickedState.canBeReplaced()) {
            return context;
        }
        BlockPos adjacentPos = clickedPos.relative(context.getClickedFace());
        BlockState adjacentState = level.getBlockState(adjacentPos);
        if (adjacentState.isAir() || adjacentState.liquid() || adjacentState.canBeReplaced()) {
            BlockHitResult hitResult = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), adjacentPos, false);
            return new BlockPlaceContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(), hitResult);
        }
        return null;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new FacadeItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getBlock() instanceof SimpleFacadeBlock facadeBlock) {
            Block sourceBlock = facadeBlock.definition().sourceBlock();
            Component sourceName = sourceName(facadeBlock, sourceBlock);
            int stateIndex = facadeBlock.definition().normalizeStateIndex(getFacadeState(stack));
            if (facadeBlock.definition().stateful() && stateIndex > 0) {
                return Component.translatable("item.facadebypycoder.facade_stateful", sourceName, stateIndex);
            }
            return Component.translatable("item.facadebypycoder.facade_simple", sourceName);
        }
        return super.getName(stack);
    }

    private static Component sourceName(SimpleFacadeBlock facadeBlock, Block sourceBlock) {
        Item sourceItem = FacadeBlocks.sourceItemFor(facadeBlock.definition().sourceId());
        if (sourceItem != null && sourceItem != Items.AIR) {
            return sourceItem.getDescription();
        }
        if (sourceBlock == null || sourceBlock.defaultBlockState().isAir()) {
            return Component.literal(facadeBlock.definition().sourceId().toString());
        }
        return Component.translatable(sourceBlock.getDescriptionId());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (getBlock() instanceof SimpleFacadeBlock facadeBlock) {
            facadeBlock.definition().sourceBlock();
            tooltip.add(Component.translatable("item.facadebypycoder.source", facadeBlock.definition().sourceId().toString()));
            if (facadeBlock.definition().stateful()) {
                int stateIndex = facadeBlock.definition().normalizeStateIndex(getFacadeState(stack));
                tooltip.add(Component.translatable("item.facadebypycoder.state", stateIndex, facadeBlock.definition().displayStates().size() - 1));
            }
        }
    }
}
