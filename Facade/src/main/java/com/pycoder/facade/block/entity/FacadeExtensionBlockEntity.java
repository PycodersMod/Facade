package com.pycoder.facade.block.entity;

import com.pycoder.facade.block.FacadeBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class FacadeExtensionBlockEntity extends BlockEntity {
    private static final String TAG_MAIN_POS = "MainPos";
    private static final String TAG_DISPLAY_STATE = "DisplayState";
    private static final String TAG_VISIBLE = "Visible";
    private BlockPos mainPos;
    private BlockState displayState = Blocks.AIR.defaultBlockState();
    private boolean visible;

    public FacadeExtensionBlockEntity(BlockPos pos, BlockState state) {
        super(FacadeBlockEntityTypes.FACADE_EXTENSION_BLOCK_ENTITY_TYPE, pos, state);
    }

    public BlockPos getMainPos() {
        return mainPos == null ? getBlockPos() : mainPos;
    }

    public BlockState getDisplayState() {
        return displayState;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setData(BlockPos mainPos, BlockState displayState, boolean visible) {
        this.mainPos = mainPos.immutable();
        this.displayState = displayState;
        this.visible = visible;
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
        if (mainPos != null) {
            tag.putLong(TAG_MAIN_POS, mainPos.asLong());
        }
        tag.put(TAG_DISPLAY_STATE, NbtUtils.writeBlockState(displayState));
        tag.putBoolean(TAG_VISIBLE, visible);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mainPos = tag.contains(TAG_MAIN_POS) ? BlockPos.of(tag.getLong(TAG_MAIN_POS)) : null;
        if (tag.contains(TAG_DISPLAY_STATE, CompoundTag.TAG_COMPOUND)) {
            displayState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(TAG_DISPLAY_STATE));
        } else {
            displayState = Blocks.AIR.defaultBlockState();
        }
        visible = tag.getBoolean(TAG_VISIBLE);
    }

    private void syncChanged() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
}
