package com.pycoder.facade.block;

import com.pycoder.facade.block.entity.FacadeBlockEntity;
import com.pycoder.facade.block.entity.FacadeExtensionBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class FacadeBlockEntityTypes {
    public static BlockEntityType<FacadeBlockEntity> FACADE_BLOCK_ENTITY_TYPE;
    public static BlockEntityType<FacadeExtensionBlockEntity> FACADE_EXTENSION_BLOCK_ENTITY_TYPE;

    private FacadeBlockEntityTypes() {
    }

    public static void build(Iterable<Block> facadeBlocks) {
        FACADE_BLOCK_ENTITY_TYPE = BlockEntityType.Builder.of(FacadeBlockEntity::new, toArray(facadeBlocks)).build(null);
    }

    public static void buildExtension(Block extensionBlock) {
        FACADE_EXTENSION_BLOCK_ENTITY_TYPE = BlockEntityType.Builder.of(FacadeExtensionBlockEntity::new, extensionBlock).build(null);
    }

    private static Block[] toArray(Iterable<Block> facadeBlocks) {
        java.util.ArrayList<Block> blocks = new java.util.ArrayList<>();
        for (Block block : facadeBlocks) {
            blocks.add(block);
        }
        return blocks.toArray(Block[]::new);
    }
}
