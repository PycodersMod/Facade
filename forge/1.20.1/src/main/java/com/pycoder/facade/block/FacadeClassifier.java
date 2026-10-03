package com.pycoder.facade.block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

public final class FacadeClassifier {
    private static final ResourceLocation CREATE_HOSE_PULLEY = new ResourceLocation("create", "hose_pulley");
    private static final int HOSE_PULLEY_MAX_EXTENSION = 384;
    private static final Set<String> STATEFUL_PROPERTIES = Set.of(
            "lit",
            "level",
            "moisture",
            "age",
            "bites",
            "eggs",
            "honey_level",
            "delay",
            "note",
            "stage",
            "extended",
            "open"
    );
    private static final Set<String> EXCLUDED_PROPERTIES = Set.of(
            "facing",
            "axis",
            "horizontal_axis",
            "rotation",
            "half",
            "shape",
            "waterlogged",
            "powered",
            "hinge",
            "type",
            "part"
    );
    private static final Set<Block> EXCLUDED_BLOCKS = Set.of(
            Blocks.AIR,
            Blocks.CAVE_AIR,
            Blocks.VOID_AIR,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.FIRE,
            Blocks.SOUL_FIRE,
            Blocks.STRUCTURE_VOID,
            Blocks.LIGHT,
            Blocks.BARRIER,
            Blocks.MOVING_PISTON,
            Blocks.PISTON_HEAD,
            Blocks.BUBBLE_COLUMN,
            Blocks.SNOW
    );

    private FacadeClassifier() {
    }

    public static boolean canCreateFacade(Block block) {
        if (EXCLUDED_BLOCKS.contains(block) || block instanceof LiquidBlock) {
            return false;
        }
        if (block.defaultBlockState().isAir()) {
            return false;
        }
        if (block.defaultBlockState().getFluidState().isSource()) {
            return false;
        }
        return block.asItem() != net.minecraft.world.item.Items.AIR;
    }

    public static FacadeDefinition createDefinition(ResourceLocation sourceId, Block block) {
        List<Property<?>> stateProperties = stateProperties(block);
        boolean stateful = shouldBeStateful(sourceId, block, stateProperties);
        List<BlockState> displayStates = displayStates(block, stateful, stateProperties);
        ResourceLocation facadeId = facadeId(sourceId);
        return new FacadeDefinition(sourceId, facadeId, block, stateful, stateProperties, displayStates);
    }

    public static FacadeDefinition createLazyDefinition(ResourceLocation sourceId) {
        return new FacadeDefinition(sourceId, facadeId(sourceId), Blocks.AIR);
    }

    public static ResourceLocation facadeId(ResourceLocation sourceId) {
        return new ResourceLocation("facadebypycoder", sourceId.getNamespace() + "__" + sourceId.getPath().replace('/', '_'));
    }

    static boolean shouldBeStatefulBlock(ResourceLocation sourceId, Block block, List<Property<?>> properties) {
        return shouldBeStateful(sourceId, block, properties);
    }

    static List<Property<?>> statePropertiesFor(Block block) {
        return stateProperties(block);
    }

    static List<BlockState> displayStates(Block block, boolean stateful, List<Property<?>> stateProperties) {
        return stateful ? semanticStates(block, stateProperties) : List.of(block.defaultBlockState());
    }

    private static boolean shouldBeStateful(ResourceLocation sourceId, Block block, List<Property<?>> properties) {
        if (CREATE_HOSE_PULLEY.equals(sourceId)) {
            return true;
        }
        if (isPiston(sourceId)) {
            return true;
        }
        if (isDoor(sourceId, properties)) {
            return true;
        }
        int stateCount = block.getStateDefinition().getPossibleStates().size();
        if (stateCount < 2 || stateCount > 256) {
            return false;
        }
        if (!"minecraft".equals(sourceId.getNamespace()) && !properties.isEmpty()) {
            return true;
        }
        for (Property<?> property : properties) {
            String name = property.getName();
            if (STATEFUL_PROPERTIES.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPiston(ResourceLocation sourceId) {
        String path = sourceId.getPath();
        return path.contains("piston") && !path.contains("piston_head") && !path.contains("moving_piston");
    }

    private static boolean isDoor(ResourceLocation sourceId, List<Property<?>> properties) {
        if (!sourceId.getPath().endsWith("door")) {
            return false;
        }
        for (Property<?> property : properties) {
            if ("open".equals(property.getName())) {
                return true;
            }
        }
        return false;
    }

    private static List<Property<?>> stateProperties(Block block) {
        List<Property<?>> result = new ArrayList<>();
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            if (!EXCLUDED_PROPERTIES.contains(property.getName())) {
                result.add(property);
            }
        }
        return List.copyOf(result);
    }

    private static List<BlockState> sortedStates(Block block) {
        List<BlockState> states = new ArrayList<>(block.getStateDefinition().getPossibleStates());
        states.sort((left, right) -> left.toString().compareTo(right.toString()));
        return List.copyOf(states);
    }

    private static List<BlockState> semanticStates(Block block, List<Property<?>> stateProperties) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (CREATE_HOSE_PULLEY.equals(blockId)) {
            return repeatedStates(block.defaultBlockState(), HOSE_PULLEY_MAX_EXTENSION + 1);
        }
        if (block instanceof DoorBlock) {
            return openClosedStates(block.defaultBlockState());
        }
        if (baseStateHasProperty(block, BlockStateProperties.EXTENDED)) {
            return booleanStates(block.defaultBlockState(), BlockStateProperties.EXTENDED);
        }
        if (block instanceof RepeaterBlock) {
            return integerStates(block.defaultBlockState(), RepeaterBlock.DELAY);
        }
        List<BlockState> states = List.of(block.defaultBlockState());
        for (Property<?> property : stateProperties) {
            states = expandStates(states, property);
        }
        return states.size() > 1 ? states : sortedStates(block);
    }

    private static List<BlockState> repeatedStates(BlockState state, int count) {
        List<BlockState> states = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            states.add(state);
        }
        return List.copyOf(states);
    }

    private static List<BlockState> openClosedStates(BlockState baseState) {
        if (!baseState.hasProperty(BlockStateProperties.OPEN)) {
            return List.of(baseState);
        }
        return List.of(baseState.setValue(BlockStateProperties.OPEN, false), baseState.setValue(BlockStateProperties.OPEN, true));
    }

    private static boolean baseStateHasProperty(Block block, Property<?> property) {
        return block.defaultBlockState().hasProperty(property);
    }

    private static List<BlockState> booleanStates(BlockState baseState, BooleanProperty property) {
        if (!baseState.hasProperty(property)) {
            return List.of(baseState);
        }
        return List.of(baseState.setValue(property, false), baseState.setValue(property, true));
    }

    private static List<BlockState> integerStates(BlockState baseState, IntegerProperty property) {
        if (!baseState.hasProperty(property)) {
            return List.of(baseState);
        }
        List<BlockState> states = new ArrayList<>();
        for (Integer value : property.getPossibleValues()) {
            states.add(baseState.setValue(property, value));
        }
        return List.copyOf(states);
    }

    private static <T extends Comparable<T>> List<BlockState> expandStates(List<BlockState> baseStates, Property<T> property) {
        List<BlockState> states = new ArrayList<>();
        for (BlockState baseState : baseStates) {
            if (!baseState.hasProperty(property)) {
                states.add(baseState);
                continue;
            }
            for (T value : property.getPossibleValues()) {
                states.add(baseState.setValue(property, value));
            }
        }
        return List.copyOf(states);
    }

    public static Set<String> allowedNamespaces(List<? extends String> configured) {
        Set<String> result = new HashSet<>();
        for (String namespace : configured) {
            if (namespace != null && !namespace.isBlank()) {
                result.add(namespace.trim());
            }
        }
        return result;
    }
}
