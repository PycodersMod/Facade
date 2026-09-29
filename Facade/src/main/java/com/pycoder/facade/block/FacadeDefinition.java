package com.pycoder.facade.block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FacadeDefinition {
    private static final ResourceLocation CREATE_HOSE_PULLEY = new ResourceLocation("create", "hose_pulley");
    private static final ResourceLocation CREATE_PULLEY_MAGNET = new ResourceLocation("create", "pulley_magnet");
    private final ResourceLocation sourceId;
    private final ResourceLocation facadeId;
    private final Block fallbackSourceBlock;
    private Block cachedSourceBlock;
    private boolean refreshPending;
    private boolean stateful;
    private List<Property<?>> stateProperties;
    private List<BlockState> displayStates;

    public FacadeDefinition(ResourceLocation sourceId, ResourceLocation facadeId, Block sourceBlock) {
        this(sourceId, facadeId, sourceBlock, false, List.of(), List.of());
    }

    public FacadeDefinition(ResourceLocation sourceId, ResourceLocation facadeId, Block sourceBlock, boolean stateful, List<Property<?>> stateProperties, List<BlockState> displayStates) {
        this.sourceId = sourceId;
        this.facadeId = facadeId;
        this.fallbackSourceBlock = sourceBlock;
        this.refreshPending = sourceBlock == null || sourceBlock == Blocks.AIR || displayStates.isEmpty();
        refresh(sourceBlock, stateful, stateProperties, displayStates);
    }

    public ResourceLocation sourceId() {
        return sourceId;
    }

    public ResourceLocation facadeId() {
        return facadeId;
    }

    public Block sourceBlock() {
        Block resolvedBlock = FacadeBlocks.sourceBlockFor(sourceId);
        if (resolvedBlock == null || resolvedBlock == Blocks.AIR) {
            resolvedBlock = fallbackSourceBlock;
        }
        if ((resolvedBlock == null || resolvedBlock == Blocks.AIR) && cachedSourceBlock != null && cachedSourceBlock != Blocks.AIR) {
            resolvedBlock = cachedSourceBlock;
        }
        if (resolvedBlock != null && resolvedBlock != Blocks.AIR && (resolvedBlock != cachedSourceBlock || refreshPending)) {
            refresh(resolvedBlock);
        }
        return cachedSourceBlock;
    }

    public boolean stateful() {
        sourceBlock();
        return stateful;
    }

    public List<Property<?>> stateProperties() {
        sourceBlock();
        return stateProperties;
    }

    public List<BlockState> displayStates() {
        sourceBlock();
        return displayStates;
    }

    public BlockState displayState(int stateIndex) {
        sourceBlock();
        if (displayStates.isEmpty()) {
            return cachedSourceBlock.defaultBlockState();
        }
        return displayStates.get(normalizeStateIndex(stateIndex));
    }

    public BlockState placedDisplayState(BlockPlaceContext context, int stateIndex) {
        Block sourceBlock = sourceBlock();
        BlockState placedState = sourceBlock.getStateForPlacement(context);
        if (placedState == null) {
            placedState = sourceBlock.defaultBlockState();
        }
        BlockState semanticState = displayState(stateIndex);
        for (Property<?> property : stateProperties) {
            placedState = copyProperty(semanticState, placedState, property);
        }
        placedState = applyPlacementOrientation(placedState, context);
        if (placedState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            placedState = placedState.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        }
        return placedState;
    }

    public BlockState applyStateToDisplay(BlockState currentDisplayState, int stateIndex) {
        BlockState semanticState = displayState(stateIndex);
        BlockState displayState = currentDisplayState;
        for (Property<?> property : stateProperties) {
            displayState = copyProperty(semanticState, displayState, property);
        }
        if (displayState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            displayState = displayState.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        }
        return displayState;
    }

    public boolean hasExtendedPistonHead(BlockState displayState) {
        return sourceBlock().defaultBlockState().hasProperty(BlockStateProperties.EXTENDED)
                && displayState.hasProperty(BlockStateProperties.EXTENDED)
                && displayState.getValue(BlockStateProperties.EXTENDED);
    }

    public Direction pistonHeadDirection(BlockState displayState) {
        if (displayState.hasProperty(BlockStateProperties.FACING)) {
            return displayState.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    public BlockState pistonHeadState(BlockState displayState) {
        PistonType pistonType = sourceId.getPath().contains("sticky") ? PistonType.STICKY : PistonType.DEFAULT;
        return Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(PistonHeadBlock.FACING, pistonHeadDirection(displayState))
                .setValue(PistonHeadBlock.TYPE, pistonType)
                .setValue(PistonHeadBlock.SHORT, false);
    }

    public List<ExtensionPart> extensionParts(BlockState displayState) {
        return extensionParts(displayState, 0);
    }

    public List<ExtensionPart> extensionParts(BlockState displayState, int stateIndex) {
        Map<BlockPos, ExtensionPart> extensionParts = new LinkedHashMap<>();
        if (CREATE_HOSE_PULLEY.equals(sourceId) && stateIndex > 0) {
            addHosePulleyExtensions(extensionParts, stateIndex);
            return new ArrayList<>(extensionParts.values());
        }
        if (hasExtendedPistonHead(displayState)) {
            Direction direction = pistonHeadDirection(displayState);
            BlockPos offset = new BlockPos(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            extensionParts.put(offset, new ExtensionPart(offset, pistonHeadState(displayState), true));
        }
        if (displayState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockState upperState = displayState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
            BlockPos offset = BlockPos.ZERO.above();
            extensionParts.put(offset, new ExtensionPart(offset, upperState, true));
        }
        for (BlockPos offset : shapeExtensionOffsets(displayState)) {
            extensionParts.putIfAbsent(offset, new ExtensionPart(offset, Blocks.AIR.defaultBlockState(), false));
        }
        return new ArrayList<>(extensionParts.values());
    }

    private static void addHosePulleyExtensions(Map<BlockPos, ExtensionPart> extensionParts, int stateIndex) {
        BlockState terminalState = hosePulleyTerminalState();
        for (int index = 1; index <= stateIndex; index++) {
            BlockPos offset = BlockPos.ZERO.below(index);
            extensionParts.put(offset, new ExtensionPart(offset, terminalState, true));
        }
    }

    private static BlockState hosePulleyTerminalState() {
        Block block = FacadeBlocks.sourceBlockFor(CREATE_PULLEY_MAGNET);
        if (block == null || block.defaultBlockState().isAir()) {
            return Blocks.CHAIN.defaultBlockState();
        }
        return block.defaultBlockState();
    }

    public int defaultStateIndex() {
        return 0;
    }

    public int normalizeStateIndex(int stateIndex) {
        sourceBlock();
        if (stateIndex < 0 || stateIndex >= displayStates.size()) {
            return defaultStateIndex();
        }
        return stateIndex;
    }

    public boolean supportsStateIndex(int stateIndex) {
        sourceBlock();
        return stateIndex >= 0 && stateIndex < displayStates.size();
    }

    private void refresh(Block sourceBlock) {
        if (sourceBlock == null || sourceBlock.defaultBlockState().isAir()) {
            refresh(Blocks.AIR, false, List.of(), List.of(Blocks.AIR.defaultBlockState()));
            return;
        }
        List<Property<?>> properties = FacadeClassifier.statePropertiesFor(sourceBlock);
        boolean isStateful = FacadeClassifier.shouldBeStatefulBlock(sourceId, sourceBlock, properties);
        refresh(sourceBlock, isStateful, properties, FacadeClassifier.displayStates(sourceBlock, isStateful, properties));
    }

    private void refresh(Block sourceBlock, boolean stateful, List<Property<?>> stateProperties, List<BlockState> displayStates) {
        this.cachedSourceBlock = sourceBlock;
        this.refreshPending = sourceBlock == null || sourceBlock == Blocks.AIR;
        this.stateful = stateful;
        this.stateProperties = List.copyOf(stateProperties);
        this.displayStates = displayStates.isEmpty() ? List.of(sourceBlock.defaultBlockState()) : List.copyOf(displayStates);
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState source, BlockState target, Property<T> property) {
        if (!source.hasProperty(property) || !target.hasProperty(property)) {
            return target;
        }
        return target.setValue(property, source.getValue(property));
    }

    private static BlockState applyPlacementOrientation(BlockState state, BlockPlaceContext context) {
        Direction horizontalFacing = context.getHorizontalDirection().getOpposite();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, horizontalFacing);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, context.getClickedFace());
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_AXIS, horizontalFacing.getAxis());
        } else if (state.hasProperty(BlockStateProperties.AXIS)) {
            state = state.setValue(BlockStateProperties.AXIS, context.getClickedFace().getAxis());
        }
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            int rotation = Math.floorMod(Math.round(context.getRotation() * 16.0F / 360.0F), 16);
            state = state.setValue(BlockStateProperties.ROTATION_16, rotation);
        }
        return state;
    }

    private static List<BlockPos> shapeExtensionOffsets(BlockState displayState) {
        VoxelShape shape = Shapes.or(
                displayState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()),
                displayState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty())
        );
        if (shape.isEmpty()) {
            return List.of();
        }
        List<BlockPos> offsets = new ArrayList<>();
        for (AABB box : shape.toAabbs()) {
            int minX = (int) Math.floor(box.minX);
            int minY = (int) Math.floor(box.minY);
            int minZ = (int) Math.floor(box.minZ);
            int maxX = (int) Math.ceil(box.maxX) - 1;
            int maxY = (int) Math.ceil(box.maxY) - 1;
            int maxZ = (int) Math.ceil(box.maxZ) - 1;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos offset = new BlockPos(x, y, z);
                        if (!offset.equals(BlockPos.ZERO) && !offsets.contains(offset)) {
                            offsets.add(offset);
                        }
                    }
                }
            }
        }
        return offsets;
    }

    public record ExtensionPart(BlockPos offset, BlockState displayState, boolean visible) {
    }

}
