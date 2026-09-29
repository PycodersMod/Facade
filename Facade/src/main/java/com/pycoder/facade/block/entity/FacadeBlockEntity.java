package com.pycoder.facade.block.entity;

import com.pycoder.facade.block.SimpleFacadeBlock;
import com.pycoder.facade.block.FacadeBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class FacadeBlockEntity extends BlockEntity {
    private static final String TAG_STATE = "FacadeState";
    private static final String TAG_DISPLAY_STATE = "DisplayState";
    private int facadeState;
    private BlockState displayState;

    public FacadeBlockEntity(BlockPos pos, BlockState state) {
        super(FacadeBlockEntityTypes.FACADE_BLOCK_ENTITY_TYPE, pos, state);
    }

    public int getFacadeState() {
        return facadeState;
    }

    public BlockState getDisplayState() {
        if (displayState != null) {
            return displayState;
        }
        if (getBlockState().getBlock() instanceof SimpleFacadeBlock facadeBlock) {
            return facadeBlock.definition().displayState(facadeState);
        }
        return getBlockState();
    }

    public void setFacadeState(int facadeState) {
        this.facadeState = normalizeFacadeState(facadeState);
        syncChanged();
    }

    public void setFacadeState(int facadeState, BlockState displayState) {
        this.facadeState = normalizeFacadeState(facadeState);
        this.displayState = displayState;
        syncChanged();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_STATE, facadeState);
        if (displayState != null) {
            tag.put(TAG_DISPLAY_STATE, NbtUtils.writeBlockState(displayState));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        facadeState = normalizeFacadeState(tag.getInt(TAG_STATE));
        if (tag.contains(TAG_DISPLAY_STATE, CompoundTag.TAG_COMPOUND)) {
            displayState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(TAG_DISPLAY_STATE));
        } else {
            displayState = null;
        }
    }

    private int normalizeFacadeState(int stateIndex) {
        if (getBlockState().getBlock() instanceof SimpleFacadeBlock facadeBlock) {
            return facadeBlock.definition().normalizeStateIndex(stateIndex);
        }
        return Math.max(0, stateIndex);
    }

    private void syncChanged() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
}
