package com.pycoder.facadebypycoder.client;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class FacadeItemBakedModel implements BakedModel {
    private final BakedModel fallbackModel;

    public FacadeItemBakedModel(BakedModel fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return fallbackModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return fallbackModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return fallbackModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
