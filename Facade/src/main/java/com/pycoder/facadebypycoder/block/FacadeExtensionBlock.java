package com.pycoder.facadebypycoder.block;

import com.pycoder.facadebypycoder.block.entity.FacadeExtensionBlockEntity;
import com.pycoder.facadebypycoder.ghost.GhostModeState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class FacadeExtensionBlock extends Block implements EntityBlock {
    public FacadeExtensionBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FacadeExtensionBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return extensionShape(level, pos, false, context);
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
        return extensionShape(level, pos, true, context);
    }

    private static VoxelShape extensionShape(BlockGetter level, BlockPos pos, boolean collision, CollisionContext context) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FacadeExtensionBlockEntity extensionBlockEntity) {
            if (!extensionBlockEntity.isVisible()) {
                return Shapes.block();
            }
            BlockState displayState = extensionBlockEntity.getDisplayState();
            return collision ? displayState.getCollisionShape(level, pos, context) : displayState.getShape(level, pos, context);
        }
        return Shapes.block();
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
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeMain(level, pos);
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos mainPos = mainPos(level, pos);
        if (!mainPos.equals(pos)) {
            BlockState mainState = level.getBlockState(mainPos);
            return mainState.use(level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), mainPos, hit.isInside()));
        }
        return InteractionResult.PASS;
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
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
    }

    private static void removeMain(Level level, BlockPos pos) {
        BlockPos mainPos = mainPos(level, pos);
        if (!mainPos.equals(pos) && level.getBlockState(mainPos).getBlock() instanceof SimpleFacadeBlock) {
            level.removeBlock(mainPos, false);
        }
    }

    private static BlockPos mainPos(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FacadeExtensionBlockEntity extensionBlockEntity) {
            return extensionBlockEntity.getMainPos();
        }
        return pos;
    }
}
