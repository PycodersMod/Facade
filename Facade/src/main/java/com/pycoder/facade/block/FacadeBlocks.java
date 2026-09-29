package com.pycoder.facade.block;

import com.pycoder.facade.FacadeByPycoder;
import com.pycoder.facade.config.ModConfig;
import com.pycoder.facade.item.FacadeBlockItem;
import com.pycoder.facade.item.ProjectionToolItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

public final class FacadeBlocks {
    private static final Set<String> NON_CONTENT_NAMESPACES = Set.of(
            "forge",
            "neoforge",
            "fabric",
            "fabric_api",
            "architectury",
            "cloth_config",
            "configured",
            "catalogue",
            "jei",
            "jeresources",
            "emi",
            "rei",
            "jade",
            "theoneprobe",
            "wthit",
            "appleskin",
            "modmenu"
    );
    private static final Map<ResourceLocation, FacadeDefinition> BY_SOURCE_ID = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Block> BLOCKS_BY_SOURCE_ID = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Item> ITEMS_BY_SOURCE_ID = new LinkedHashMap<>();
    public static final Item PROJECTION_TOOL = new ProjectionToolItem(new Item.Properties());
    public static final FacadeExtensionBlock EXTENSION_BLOCK = new FacadeExtensionBlock(BlockBehaviour.Properties.of()
            .noOcclusion()
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)
            .noLootTable()
            .strength(0.0F, 3600000.0F));

    private FacadeBlocks() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegister(RegisterEvent event) {
        ResourceKey<?> registryKey = event.getRegistryKey();
        if (registryKey == Registries.BLOCK) {
            registerBlocks(event);
            return;
        }
        if (registryKey == Registries.ITEM) {
            registerItems(event);
            return;
        }
        if (registryKey == Registries.BLOCK_ENTITY_TYPE) {
            registerBlockEntityType(event);
        }
    }

    public static Collection<FacadeDefinition> definitions() {
        return BY_SOURCE_ID.values();
    }

    public static FacadeDefinition definition(ResourceLocation sourceId) {
        return BY_SOURCE_ID.get(resolveSourceId(sourceId));
    }

    public static Item item(ResourceLocation sourceId) {
        return ITEMS_BY_SOURCE_ID.get(resolveSourceId(sourceId));
    }

    public static boolean hasFacade(ResourceLocation sourceId) {
        return BY_SOURCE_ID.containsKey(resolveSourceId(sourceId));
    }

    public static ResourceLocation resolveSourceId(ResourceLocation sourceId) {
        ResourceLocation knownSourceId = knownSourceId(sourceId);
        if (knownSourceId != null) {
            return knownSourceId;
        }
        return canonicalSourceId(sourceId);
    }

    public static void refreshDefinitions() {
        int unresolved = 0;
        for (FacadeDefinition definition : BY_SOURCE_ID.values()) {
            Block sourceBlock = definition.sourceBlock();
            if (sourceBlock == null || sourceBlock == Blocks.AIR) {
                unresolved++;
            }
        }
        if (unresolved > 0) {
            FacadeByPycoder.LOGGER.warn("{} facade source blocks are still unresolved; their names and states will refresh when those blocks become available", unresolved);
        }
    }

    private static void registerBlocks(RegisterEvent event) {
        event.register(Registries.BLOCK, new ResourceLocation(FacadeByPycoder.MOD_ID, "facade_extension"), () -> EXTENSION_BLOCK);
        Set<String> detectedNamespaces = detectBlockProvidingNamespaces();
        Set<String> allowedNamespaces = FacadeClassifier.allowedNamespaces(ModConfig.autoConfigureAllowedModIds(new ArrayList<>(detectedNamespaces)));
        for (Map.Entry<ResourceLocation, Block> entry : collectSourceBlocks(allowedNamespaces).entrySet()) {
            registerFacadeBlock(event, entry.getKey(), entry.getValue());
        }
        FacadeByPycoder.LOGGER.info("Registered {} facade blocks from allowed namespaces {}", BLOCKS_BY_SOURCE_ID.size(), allowedNamespaces);
    }

    private static Set<String> detectBlockProvidingNamespaces() {
        Set<String> namespaces = new LinkedHashSet<>();
        namespaces.add("minecraft");
        for (Map.Entry<ResourceKey<Block>, Block> entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation sourceId = entry.getKey().location();
            if (isContentNamespace(sourceId.getNamespace()) && FacadeClassifier.canCreateFacade(entry.getValue())) {
                namespaces.add(sourceId.getNamespace());
            }
        }
        for (ResourceLocation sourceId : discoverBlockstateIds(Set.of())) {
            if (isContentNamespace(sourceId.getNamespace())) {
                namespaces.add(sourceId.getNamespace());
            }
        }
        return namespaces;
    }

    private static boolean isContentNamespace(String namespace) {
        return !namespace.equals(FacadeByPycoder.MOD_ID) && !NON_CONTENT_NAMESPACES.contains(namespace);
    }

    private static Map<ResourceLocation, Block> collectSourceBlocks(Set<String> allowedNamespaces) {
        Map<ResourceLocation, Block> sourceBlocks = new LinkedHashMap<>();
        for (Map.Entry<ResourceKey<Block>, Block> entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation sourceId = entry.getKey().location();
            if (allowedNamespaces.contains(sourceId.getNamespace())) {
                sourceBlocks.put(sourceId, entry.getValue());
            }
        }
        for (ResourceLocation sourceId : discoverBlockstateIds(allowedNamespaces)) {
            ResourceLocation canonicalSourceId = canonicalSourceId(sourceId);
            Block sourceBlock = registeredBlock(canonicalSourceId);
            sourceBlocks.putIfAbsent(canonicalSourceId, sourceBlock);
        }
        return sourceBlocks;
    }

    static ResourceLocation canonicalSourceId(ResourceLocation sourceId) {
        Block exactBlock = registeredBlock(sourceId);
        if (exactBlock != null && !exactBlock.defaultBlockState().isAir()) {
            return sourceId;
        }
        String path = sourceId.getPath();
        ResourceLocation underscoreCandidate = ResourceLocation.tryParse(sourceId.getNamespace() + ":" + path.replace('.', '_'));
        if (underscoreCandidate != null) {
            Block underscoreBlock = registeredBlock(underscoreCandidate);
            if (underscoreBlock != null && !underscoreBlock.defaultBlockState().isAir()) {
                return underscoreCandidate;
            }
        }
        ResourceLocation dottedCandidate = ResourceLocation.tryParse(sourceId.getNamespace() + ":" + path.replace('_', '.'));
        if (dottedCandidate != null) {
            Block dottedBlock = registeredBlock(dottedCandidate);
            if (dottedBlock != null && !dottedBlock.defaultBlockState().isAir()) {
                return dottedCandidate;
            }
        }
        return sourceId;
    }

    public static Block sourceBlockFor(ResourceLocation sourceId) {
        ResourceLocation knownSourceId = knownSourceId(sourceId);
        if (knownSourceId != null) {
            Block knownBlock = registeredBlock(knownSourceId);
            if (knownBlock != null && !knownBlock.defaultBlockState().isAir()) {
                return knownBlock;
            }
        }
        ResourceLocation canonicalSourceId = canonicalSourceId(sourceId);
        return registeredBlock(canonicalSourceId);
    }

    public static Item sourceItemFor(ResourceLocation sourceId) {
        ResourceLocation knownSourceId = knownSourceId(sourceId);
        Item knownItem = knownSourceId == null ? null : registeredItem(knownSourceId);
        if (knownItem != null && knownItem != Items.AIR) {
            return knownItem;
        }
        return registeredItem(canonicalSourceId(sourceId));
    }

    private static ResourceLocation knownSourceId(ResourceLocation sourceId) {
        if (BY_SOURCE_ID.containsKey(sourceId)) {
            return sourceId;
        }
        String path = sourceId.getPath();
        ResourceLocation underscoreCandidate = ResourceLocation.tryParse(sourceId.getNamespace() + ":" + path.replace('.', '_'));
        if (underscoreCandidate != null && BY_SOURCE_ID.containsKey(underscoreCandidate)) {
            return underscoreCandidate;
        }
        ResourceLocation dottedCandidate = ResourceLocation.tryParse(sourceId.getNamespace() + ":" + path.replace('_', '.'));
        if (dottedCandidate != null && BY_SOURCE_ID.containsKey(dottedCandidate)) {
            return dottedCandidate;
        }
        return null;
    }

    private static Block registeredBlock(ResourceLocation sourceId) {
        Block block = ForgeRegistries.BLOCKS.getValue(sourceId);
        if ((block == null || block.defaultBlockState().isAir()) && BuiltInRegistries.BLOCK.containsKey(sourceId)) {
            block = BuiltInRegistries.BLOCK.get(sourceId);
        }
        if (block == null || block.defaultBlockState().isAir()) {
            Item item = registeredItem(sourceId);
            if (item != null && item != Items.AIR) {
                Block itemBlock = Block.byItem(item);
                if (itemBlock != Blocks.AIR) {
                    block = itemBlock;
                }
            }
        }
        return block;
    }

    private static Item registeredItem(ResourceLocation sourceId) {
        Item item = ForgeRegistries.ITEMS.getValue(sourceId);
        if ((item == null || item == Items.AIR) && BuiltInRegistries.ITEM.containsKey(sourceId)) {
            item = BuiltInRegistries.ITEM.get(sourceId);
        }
        return item;
    }

    private static void registerFacadeBlock(RegisterEvent event, ResourceLocation sourceId, Block sourceBlock) {
        if (BY_SOURCE_ID.containsKey(sourceId)) {
            return;
        }
        boolean sourceAvailable = sourceBlock != null && FacadeClassifier.canCreateFacade(sourceBlock);
        FacadeDefinition definition = sourceAvailable ? FacadeClassifier.createDefinition(sourceId, sourceBlock) : FacadeClassifier.createLazyDefinition(sourceId);
        Block facadeBlock = definition.stateful()
                ? new StatefulFacadeBlock(definition, SimpleFacadeBlock.properties(ModConfig.HARDNESS.get(), ModConfig.EXPLOSION_RESISTANCE.get()))
                : new SimpleFacadeBlock(definition, SimpleFacadeBlock.properties(ModConfig.HARDNESS.get(), ModConfig.EXPLOSION_RESISTANCE.get()));
        BY_SOURCE_ID.put(sourceId, definition);
        BLOCKS_BY_SOURCE_ID.put(sourceId, facadeBlock);
        event.register(Registries.BLOCK, definition.facadeId(), () -> facadeBlock);
    }

    private static List<ResourceLocation> discoverBlockstateIds(Set<String> allowedNamespaces) {
        List<ResourceLocation> result = new ArrayList<>();
        for (Path modsPath : candidateModsPaths()) {
            discoverBlockstateIds(modsPath, allowedNamespaces, result);
        }
        return result;
    }

    private static List<Path> candidateModsPaths() {
        List<Path> paths = new ArrayList<>();
        addCandidateModsPath(paths, FMLPaths.MODSDIR.get());
        addCandidateModsPath(paths, Path.of("mods"));
        addCandidateModsPath(paths, Path.of("run", "mods"));
        return paths;
    }

    private static void addCandidateModsPath(List<Path> paths, Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!paths.contains(normalizedPath)) {
            paths.add(normalizedPath);
        }
    }

    private static void discoverBlockstateIds(Path modsPath, Set<String> allowedNamespaces, List<ResourceLocation> result) {
        if (!Files.isDirectory(modsPath)) {
            return;
        }
        FacadeByPycoder.LOGGER.info("Scanning {} for facade source blockstates", modsPath);
        try (Stream<Path> paths = Files.list(modsPath)) {
            paths.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .forEach(path -> discoverBlockstateIdsInJar(path, allowedNamespaces, result));
        } catch (IOException exception) {
            FacadeByPycoder.LOGGER.warn("Failed to scan mods directory {} for facade blockstates", modsPath, exception);
        }
    }

    private static void discoverBlockstateIdsInJar(Path jarPath, Set<String> allowedNamespaces, List<ResourceLocation> result) {
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            zipFile.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith("assets/") && name.contains("/blockstates/") && name.endsWith(".json"))
                    .map(FacadeBlocks::blockstateResourceToId)
                    .filter(sourceId -> sourceId != null && (allowedNamespaces.isEmpty() || allowedNamespaces.contains(sourceId.getNamespace())) && !result.contains(sourceId))
                    .forEach(result::add);
        } catch (IOException exception) {
            FacadeByPycoder.LOGGER.warn("Failed to scan {} for facade blockstates", jarPath, exception);
        }
    }

    private static ResourceLocation blockstateResourceToId(String name) {
        String prefix = "assets/";
        int namespaceStart = prefix.length();
        int namespaceEnd = name.indexOf("/blockstates/", namespaceStart);
        if (namespaceEnd < 0) {
            return null;
        }
        String namespace = name.substring(namespaceStart, namespaceEnd);
        String path = name.substring(namespaceEnd + "/blockstates/".length(), name.length() - ".json".length());
        return ResourceLocation.tryParse(namespace + ":" + path);
    }

    private static void registerItems(RegisterEvent event) {
        refreshDefinitions();
        event.register(Registries.ITEM, new ResourceLocation(FacadeByPycoder.MOD_ID, "stick"), () -> PROJECTION_TOOL);
        for (Map.Entry<ResourceLocation, Block> entry : BLOCKS_BY_SOURCE_ID.entrySet()) {
            FacadeDefinition definition = BY_SOURCE_ID.get(entry.getKey());
            BlockItem item = new FacadeBlockItem(entry.getValue(), new Item.Properties());
            ITEMS_BY_SOURCE_ID.put(entry.getKey(), item);
            event.register(Registries.ITEM, definition.facadeId(), () -> item);
        }
        FacadeByPycoder.LOGGER.info("Registered {} facade items", ITEMS_BY_SOURCE_ID.size());
    }

    private static void registerBlockEntityType(RegisterEvent event) {
        FacadeBlockEntityTypes.build(BLOCKS_BY_SOURCE_ID.values());
        event.register(Registries.BLOCK_ENTITY_TYPE, new ResourceLocation(FacadeByPycoder.MOD_ID, "facade"), () -> FacadeBlockEntityTypes.FACADE_BLOCK_ENTITY_TYPE);
        FacadeBlockEntityTypes.buildExtension(EXTENSION_BLOCK);
        event.register(Registries.BLOCK_ENTITY_TYPE, new ResourceLocation(FacadeByPycoder.MOD_ID, "facade_extension"), () -> FacadeBlockEntityTypes.FACADE_EXTENSION_BLOCK_ENTITY_TYPE);
    }
}
