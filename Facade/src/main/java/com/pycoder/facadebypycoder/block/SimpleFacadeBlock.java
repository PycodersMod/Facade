package com.pycoder.facadebypycoder.block;

import com.pycoder.facadebypycoder.block.entity.FacadeBlockEntity;
import com.pycoder.facadebypycoder.block.entity.FacadeExtensionBlockEntity;
import com.pycoder.facadebypycoder.ghost.GhostModeState;
import com.pycoder.facadebypycoder.item.FacadeBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SimpleFacadeBlock extends Block implements EntityBlock {
    private final FacadeDefinition definition;

    public SimpleFacadeBlock(FacadeDefinition definition, Properties properties) {
        super(properties);
        this.definition = definition;
    }

    public FacadeDefinition definition() {
        return definition;
    }

    public Block getSourceBlock() {
        return definition.sourceBlock();
    }

    public BlockState getDisplayState(BlockGetter level, BlockPos pos, BlockState facadeState) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FacadeBlockEntity facadeBlockEntity) {
            return facadeBlockEntity.getDisplayState();
        }
        return definition.displayState(0);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FacadeBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FacadeBlockEntity facadeBlockEntity) {
            int stateIndex = definition.normalizeStateIndex(FacadeBlockItem.getFacadeState(stack));
            BlockPlaceContext placeContext = FacadeBlockItem.getLastPlaceContext();
            BlockState displayState = placeContext == null ? definition.displayState(stateIndex) : definition.placedDisplayState(placeContext, stateIndex);
            facadeBlockEntity.setFacadeState(stateIndex, displayState);
            placeExtensionPart(level, pos, displayState, stateIndex);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fullDisplayShape(level, pos, state, false, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player
                && GhostModeState.isGhost(player)) {
            return Shapes.empty();
        }
        if (context == CollisionContext.empty()) {
            return Shapes.block();
        }
        return fullDisplayShape(level, pos, state, true, context);
    }

    private VoxelShape fullDisplayShape(BlockGetter level, BlockPos pos, BlockState facadeState, boolean collision, CollisionContext context) {
        BlockState displayState = getDisplayState(level, pos, facadeState);
        VoxelShape shape = collision ? displayState.getCollisionShape(level, pos, context) : displayState.getShape(level, pos, context);
        if (definition.hasExtendedPistonHead(displayState)) {
            BlockState pistonHeadState = definition.pistonHeadState(displayState);
            Direction direction = definition.pistonHeadDirection(displayState);
            VoxelShape pistonHeadShape = collision ? pistonHeadState.getCollisionShape(level, pos.relative(direction), context) : pistonHeadState.getShape(level, pos.relative(direction), context);
            return Shapes.or(shape, pistonHeadShape.move(direction.getStepX(), direction.getStepY(), direction.getStepZ()));
        }
        if (!displayState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return shape;
        }
        BlockState lowerState = displayState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upperState = displayState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        VoxelShape lowerShape = collision ? lowerState.getCollisionShape(level, pos, context) : lowerState.getShape(level, pos, context);
        VoxelShape upperShape = collision ? upperState.getCollisionShape(level, pos.above(), context) : upperState.getShape(level, pos.above(), context);
        return Shapes.or(lowerShape, upperShape.move(0.0D, 1.0D, 0.0D));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return 0;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).isEmpty()) {
            return cycleFacadeState(level, pos, player.isShiftKeyDown() ? -1 : 1);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult cycleFacadeState(Level level, BlockPos pos, int step) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof FacadeBlockEntity facadeBlockEntity) || !definition.stateful()) {
            return InteractionResult.PASS;
        }
        int currentState = facadeBlockEntity.getFacadeState();
        StateChange stateChange = findNextAvailableState(level, pos, facadeBlockEntity.getDisplayState(), currentState, step);
        if (stateChange.stateIndex() == currentState) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            removeExtensionPart(level, pos);
            facadeBlockEntity.setFacadeState(stateChange.stateIndex(), stateChange.displayState());
            placeExtensionPart(level, pos, stateChange.displayState(), stateChange.stateIndex());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private StateChange findNextAvailableState(Level level, BlockPos pos, BlockState currentDisplayState, int currentState, int step) {
        int stateCount = definition.displayStates().size();
        if (stateCount <= 1) {
            return new StateChange(currentState, currentDisplayState);
        }
        for (int checked = 0; checked < stateCount; checked++) {
            int stateIndex = Math.floorMod(currentState + step * (checked + 1), stateCount);
            BlockState displayState = definition.applyStateToDisplay(currentDisplayState, stateIndex);
            if (canPlaceExtensionsForState(level, pos, displayState, stateIndex)) {
                return new StateChange(stateIndex, displayState);
            }
        }
        return new StateChange(currentState, currentDisplayState);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            removeExtensionPart(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    public static Properties properties(double hardness, double resistance) {
        return BlockBehaviour.Properties.of()
                .strength((float) hardness, (float) resistance)
                .noOcclusion()
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .noLootTable();
    }

    public boolean canPlaceExtensions(Level level, BlockPos pos, BlockState displayState) {
        return canPlaceExtensions(level, pos, displayState, 0);
    }

    public boolean canPlaceExtensions(Level level, BlockPos pos, BlockState displayState, int stateIndex) {
        return canPlaceExtensionsForState(level, pos, displayState, stateIndex);
    }

    private boolean canPlaceExtensionsForState(Level level, BlockPos pos, BlockState displayState, int stateIndex) {
        for (FacadeDefinition.ExtensionPart extensionPart : definition.extensionParts(displayState, stateIndex)) {
            BlockPos extensionPos = pos.offset(extensionPart.offset());
            if (level.isOutsideBuildHeight(extensionPos)) {
                return false;
            }
            BlockState currentState = level.getBlockState(extensionPos);
            if (currentState.is(FacadeBlocks.EXTENSION_BLOCK)) {
                BlockEntity blockEntity = level.getBlockEntity(extensionPos);
                if (blockEntity instanceof FacadeExtensionBlockEntity extensionBlockEntity && extensionBlockEntity.getMainPos().equals(pos)) {
                    continue;
                }
            }
            if (!currentState.isAir() && !currentState.liquid() && !currentState.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    private void placeExtensionPart(Level level, BlockPos pos, BlockState displayState, int stateIndex) {
        for (FacadeDefinition.ExtensionPart extensionPart : definition.extensionParts(displayState, stateIndex)) {
            BlockPos extensionPos = pos.offset(extensionPart.offset());
            if (level.isOutsideBuildHeight(extensionPos)) {
                continue;
            }
            BlockState extensionState = FacadeBlocks.EXTENSION_BLOCK.defaultBlockState();
            level.setBlock(extensionPos, extensionState, Block.UPDATE_ALL);
            BlockEntity extensionBlockEntity = level.getBlockEntity(extensionPos);
            if (extensionBlockEntity instanceof FacadeExtensionBlockEntity facadeExtensionBlockEntity) {
                facadeExtensionBlockEntity.setData(pos, extensionPart.displayState(), extensionPart.visible());
            }
        }
    }

    private void removeExtensionPart(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FacadeBlockEntity facadeBlockEntity) {
            for (FacadeDefinition.ExtensionPart extensionPart : definition.extensionParts(facadeBlockEntity.getDisplayState(), facadeBlockEntity.getFacadeState())) {
                BlockPos extensionPos = pos.offset(extensionPart.offset());
                if (level.getBlockState(extensionPos).is(FacadeBlocks.EXTENSION_BLOCK)) {
                    level.removeBlock(extensionPos, false);
                }
            }
        }
    }

    private record StateChange(int stateIndex, BlockState displayState) {
    }
}
