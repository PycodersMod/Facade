package com.pycoder.facade.client;

import com.pycoder.facade.block.FacadeExtensionBlock;
import com.pycoder.facade.block.SimpleFacadeBlock;
import com.pycoder.facade.block.entity.FacadeBlockEntity;
import com.pycoder.facade.block.entity.FacadeExtensionBlockEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class ProjectionExporter {
    private ProjectionExporter() {
    }

    public static String ensureKnownExtension(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".schem") || lowerName.endsWith(".schematic") || lowerName.endsWith(".nbt") || lowerName.endsWith(".litematic")) {
            return fileName;
        }
        return fileName + ".schem";
    }

    public static void export(ClientLevel level, BlockPos firstPos, BlockPos secondPos, Path path) throws IOException {
        ProjectionSnapshot snapshot = ProjectionSnapshot.create(level, firstPos, secondPos);
        Files.createDirectories(path.toAbsolutePath().getParent());
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".schematic")) {
            NbtIo.writeCompressed(createSchematic(snapshot), path.toFile());
            return;
        }
        if (fileName.endsWith(".nbt")) {
            NbtIo.writeCompressed(createCreateBlueprint(snapshot), path.toFile());
            return;
        }
        if (fileName.endsWith(".litematic")) {
            NbtIo.writeCompressed(createLitematic(snapshot), path.toFile());
            return;
        }
        NbtIo.writeCompressed(createSchem(snapshot), path.toFile());
    }

    private static CompoundTag createSchem(ProjectionSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 2);
        tag.putInt("DataVersion", dataVersion());
        tag.putShort("Width", (short) snapshot.width());
        tag.putShort("Height", (short) snapshot.height());
        tag.putShort("Length", (short) snapshot.length());
        tag.putIntArray("Offset", new int[]{0, 0, 0});
        tag.put("Palette", paletteTag(snapshot.palette()));
        tag.put("BlockData", new ByteArrayTag(writeVarInts(snapshot.blockData())));
        tag.put("BlockEntities", snapshot.blockEntities());
        return tag;
    }

    private static CompoundTag createSchematic(ProjectionSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putShort("Width", (short) snapshot.width());
        tag.putShort("Height", (short) snapshot.height());
        tag.putShort("Length", (short) snapshot.length());
        tag.putString("Materials", "Alpha");
        tag.putByteArray("Blocks", new byte[snapshot.blockData().length]);
        tag.putByteArray("Data", new byte[snapshot.blockData().length]);
        tag.put("Entities", new ListTag());
        tag.put("TileEntities", snapshot.blockEntities());
        tag.put("FBP_Palette", paletteTag(snapshot.palette()));
        tag.putIntArray("FBP_BlockData", snapshot.blockData());
        return tag;
    }

    private static CompoundTag createCreateBlueprint(ProjectionSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", dataVersion());
        tag.putIntArray("size", new int[]{snapshot.width(), snapshot.height(), snapshot.length()});
        tag.put("palette", paletteList(snapshot.palette()));
        tag.put("blocks", structureBlocks(snapshot));
        tag.put("blockentities", snapshot.blockEntities());
        tag.putString("author", "FacadeByPycoder");
        return tag;
    }

    private static CompoundTag createLitematic(ProjectionSnapshot snapshot) {
        CompoundTag root = new CompoundTag();
        root.putInt("MinecraftDataVersion", dataVersion());
        root.putInt("Version", 6);
        root.putInt("SubVersion", 1);
        CompoundTag metadata = new CompoundTag();
        metadata.putString("Name", "FacadeByPycoder Projection");
        metadata.putString("Author", "FacadeByPycoder");
        metadata.putInt("RegionCount", 1);
        metadata.putInt("TotalBlocks", snapshot.blockData().length);
        metadata.putInt("TotalVolume", snapshot.blockData().length);
        metadata.put("EnclosingSize", vecTag(snapshot.width(), snapshot.height(), snapshot.length()));
        root.put("Metadata", metadata);
        CompoundTag regions = new CompoundTag();
        CompoundTag region = new CompoundTag();
        region.putInt("DataVersion", dataVersion());
        region.put("Position", vecTag(0, 0, 0));
        region.put("Size", vecTag(snapshot.width(), snapshot.height(), snapshot.length()));
        region.put("BlockStatePalette", paletteList(snapshot.palette()));
        region.put("BlockStates", new LongArrayTag(packBlockStates(snapshot.blockData(), snapshot.palette().size())));
        region.put("TileEntities", snapshot.blockEntities());
        regions.put("FacadeProjection", region);
        root.put("Regions", regions);
        return root;
    }

    private static int dataVersion() {
        return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    private static CompoundTag paletteTag(List<BlockState> palette) {
        CompoundTag tag = new CompoundTag();
        for (int index = 0; index < palette.size(); index++) {
            tag.putInt(blockStateString(palette.get(index)), index);
        }
        return tag;
    }

    private static ListTag paletteList(List<BlockState> palette) {
        ListTag list = new ListTag();
        for (BlockState state : palette) {
            list.add(blockStateTag(state));
        }
        return list;
    }

    private static ListTag structureBlocks(ProjectionSnapshot snapshot) {
        ListTag list = new ListTag();
        int index = 0;
        for (int y = 0; y < snapshot.height(); y++) {
            for (int z = 0; z < snapshot.length(); z++) {
                for (int x = 0; x < snapshot.width(); x++) {
                    CompoundTag block = new CompoundTag();
                    block.putIntArray("pos", new int[]{x, y, z});
                    block.putInt("state", snapshot.blockData()[index++]);
                    list.add(block);
                }
            }
        }
        return list;
    }

    private static CompoundTag blockStateTag(BlockState state) {
        CompoundTag tag = new CompoundTag();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        tag.putString("Name", blockId.toString());
        CompoundTag properties = new CompoundTag();
        for (Property<?> property : state.getProperties()) {
            properties.putString(property.getName(), propertyValue(state, property));
        }
        if (!properties.isEmpty()) {
            tag.put("Properties", properties);
        }
        return tag;
    }

    private static String blockStateString(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (state.getProperties().isEmpty()) {
            return blockId.toString();
        }
        List<String> properties = new ArrayList<>();
        for (Property<?> property : state.getProperties()) {
            properties.add(property.getName() + "=" + propertyValue(state, property));
        }
        properties.sort(String::compareTo);
        return blockId + "[" + String.join(",", properties) + "]";
    }

    private static <T extends Comparable<T>> String propertyValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static CompoundTag vecTag(int x, int y, int z) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        return tag;
    }

    private static byte[] writeVarInts(int[] values) {
        List<Byte> bytes = new ArrayList<>();
        for (int value : values) {
            int remaining = value;
            while ((remaining & -128) != 0) {
                bytes.add((byte) (remaining & 127 | 128));
                remaining >>>= 7;
            }
            bytes.add((byte) remaining);
        }
        byte[] result = new byte[bytes.size()];
        for (int index = 0; index < bytes.size(); index++) {
            result[index] = bytes.get(index);
        }
        return result;
    }

    private static long[] packBlockStates(int[] states, int paletteSize) {
        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, paletteSize - 1)));
        long mask = (1L << bits) - 1L;
        long[] packed = new long[(states.length * bits + 63) / 64];
        for (int index = 0; index < states.length; index++) {
            long value = states[index] & mask;
            int bitIndex = index * bits;
            int longIndex = bitIndex / 64;
            int startBit = bitIndex % 64;
            packed[longIndex] |= value << startBit;
            if (startBit + bits > 64) {
                packed[longIndex + 1] |= value >>> (64 - startBit);
            }
        }
        return packed;
    }

    private record ProjectionSnapshot(int width, int height, int length, List<BlockState> palette, int[] blockData, ListTag blockEntities) {
        static ProjectionSnapshot create(ClientLevel level, BlockPos firstPos, BlockPos secondPos) {
            int minX = Math.min(firstPos.getX(), secondPos.getX());
            int minY = Math.min(firstPos.getY(), secondPos.getY());
            int minZ = Math.min(firstPos.getZ(), secondPos.getZ());
            int maxX = Math.max(firstPos.getX(), secondPos.getX());
            int maxY = Math.max(firstPos.getY(), secondPos.getY());
            int maxZ = Math.max(firstPos.getZ(), secondPos.getZ());
            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            int length = maxZ - minZ + 1;
            Map<BlockState, Integer> palette = new LinkedHashMap<>();
            int[] blockData = new int[width * height * length];
            ListTag blockEntities = new ListTag();
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        BlockPos pos = new BlockPos(minX + x, minY + y, minZ + z);
                        BlockState state = exportState(level, pos);
                        Integer paletteIndex = palette.get(state);
                        if (paletteIndex == null) {
                            paletteIndex = palette.size();
                            palette.put(state, paletteIndex);
                        }
                        blockData[index++] = paletteIndex;
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity != null && shouldExportBlockEntity(blockEntity)) {
                            CompoundTag blockEntityTag = blockEntity.saveWithFullMetadata();
                            blockEntityTag.putInt("x", x);
                            blockEntityTag.putInt("y", y);
                            blockEntityTag.putInt("z", z);
                            blockEntityTag.putIntArray("Pos", new int[]{x, y, z});
                            blockEntities.add(blockEntityTag);
                        }
                    }
                }
            }
            return new ProjectionSnapshot(width, height, length, List.copyOf(palette.keySet()), blockData, blockEntities);
        }

        private static BlockState exportState(ClientLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SimpleFacadeBlock facadeBlock) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof FacadeBlockEntity facadeBlockEntity) {
                    return facadeBlockEntity.getDisplayState();
                }
                return facadeBlock.definition().displayState(0);
            }
            if (state.getBlock() instanceof FacadeExtensionBlock) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof FacadeExtensionBlockEntity extensionBlockEntity && extensionBlockEntity.isVisible()) {
                    return extensionBlockEntity.getDisplayState();
                }
                return Blocks.AIR.defaultBlockState();
            }
            return state;
        }

        private static boolean shouldExportBlockEntity(BlockEntity blockEntity) {
            return !(blockEntity instanceof FacadeBlockEntity) && !(blockEntity instanceof FacadeExtensionBlockEntity);
        }
    }
}
