package com.pycoder.facadebypycoder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pycoder.facadebypycoder.FacadeByPycoder;
import com.pycoder.facadebypycoder.block.FacadeExtensionBlock;
import com.pycoder.facadebypycoder.block.FacadeBlocks;
import com.pycoder.facadebypycoder.block.FacadeDefinition;
import com.pycoder.facadebypycoder.block.SimpleFacadeBlock;
import com.pycoder.facadebypycoder.block.entity.FacadeBlockEntity;
import com.pycoder.facadebypycoder.block.entity.FacadeExtensionBlockEntity;
import com.pycoder.facadebypycoder.item.FacadeBlockItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class FacadeDebugCommands {
    private FacadeDebugCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("fbpdebug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summary")
                        .executes(FacadeDebugCommands::summary))
                .then(Commands.literal("selftest")
                        .executes(FacadeDebugCommands::selftest))
                .then(Commands.literal("source")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .executes(FacadeDebugCommands::source)))
                .then(Commands.literal("held")
                        .executes(FacadeDebugCommands::held))
                .then(Commands.literal("nearby")
                        .executes(context -> nearby(context, 6))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 24))
                                .executes(context -> nearby(context, IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("pos")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(FacadeDebugCommands::pos))));
    }

    private static int summary(CommandContext<CommandSourceStack> context) {
        FacadeBlocks.refreshDefinitions();
        int definitions = FacadeBlocks.definitions().size();
        int createDefinitions = 0;
        int unresolvedDefinitions = 0;
        for (FacadeDefinition definition : FacadeBlocks.definitions()) {
            if ("create".equals(definition.sourceId().getNamespace())) {
                createDefinitions++;
            }
            Block sourceBlock = definition.sourceBlock();
            if (sourceBlock == null || sourceBlock == Blocks.AIR) {
                unresolvedDefinitions++;
            }
        }
        int createBlocks = countBlocks("create");
        int createItems = countItems("create");
        send(context, "FacadeByPycoder build marker: " + FacadeByPycoder.BUILD_MARKER);
        send(context, "Create loaded: " + ModList.get().isLoaded("create") + ", registry blocks=" + createBlocks + ", items=" + createItems);
        send(context, "Facade definitions: total=" + definitions + ", create=" + createDefinitions + ", unresolved=" + unresolvedDefinitions);
        send(context, "If this marker is not visible, the game is not loading the jar built by this workspace.");
        return 1;
    }

    private static int selftest(CommandContext<CommandSourceStack> context) {
        send(context, "=== FacadeByPycoder selftest begin ===");
        summary(context);
        reportSource(context, new ResourceLocation("minecraft", "oak_door"));
        reportSource(context, new ResourceLocation("minecraft", "piston"));
        reportSource(context, new ResourceLocation("create", "andesite_door"));
        reportSource(context, new ResourceLocation("create", "haunted_bell"));
        if (context.getSource().getEntity() instanceof ServerPlayer) {
            held(context);
            nearby(context, 6);
        } else {
            send(context, "No player entity for held/nearby checks.");
        }
        send(context, "=== FacadeByPycoder selftest end ===");
        return 1;
    }

    private static int source(CommandContext<CommandSourceStack> context) {
        String rawId = StringArgumentType.getString(context, "id").trim();
        ResourceLocation sourceId = ResourceLocation.tryParse(rawId);
        if (sourceId == null) {
            send(context, "Invalid source id: " + rawId);
            return 0;
        }
        reportSource(context, sourceId);
        return 1;
    }

    private static void reportSource(CommandContext<CommandSourceStack> context, ResourceLocation sourceId) {
        FacadeBlocks.refreshDefinitions();
        ResourceLocation resolvedId = FacadeBlocks.resolveSourceId(sourceId);
        FacadeDefinition definition = FacadeBlocks.definition(sourceId);
        Block registryBlock = FacadeBlocks.sourceBlockFor(sourceId);
        Item facadeItem = FacadeBlocks.item(sourceId);
        send(context, "requested=" + sourceId + ", resolved=" + resolvedId);
        send(context, "registryBlock=" + blockId(registryBlock) + ", facadeRegistered=" + (definition != null) + ", facadeItem=" + itemId(facadeItem));
        if (definition == null) {
            send(context, "No facade definition exists. Check allowedModIds and whether this source appeared during registration.");
            return;
        }
        Block definitionSource = definition.sourceBlock();
        BlockState displayState = definition.displayState(0);
        send(context, "definitionSource=" + blockId(definitionSource) + ", stateful=" + definition.stateful()
                + ", stateProperties=" + definition.stateProperties().size() + ", displayStates=" + definition.displayStates().size());
        send(context, "state0=" + BuiltInRegistries.BLOCK.getKey(displayState.getBlock()) + " " + displayState.getValues());
    }

    private static int held(CommandContext<CommandSourceStack> context) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            send(context, "This command needs a player.");
            return 0;
        }
        send(context, "mainHand:");
        reportStack(context, player.getMainHandItem());
        send(context, "offHand:");
        reportStack(context, player.getOffhandItem());
        return 1;
    }

    private static int nearby(CommandContext<CommandSourceStack> context, int radius) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            send(context, "This command needs a player.");
            return 0;
        }
        Level level = player.level();
        BlockPos center = player.blockPosition();
        int facadeBlocks = 0;
        int extensionBlocks = 0;
        int reported = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SimpleFacadeBlock) {
                facadeBlocks++;
                if (reported < 8) {
                    reportBlock(context, level, pos.immutable(), state);
                    reported++;
                }
            } else if (state.getBlock() instanceof FacadeExtensionBlock) {
                extensionBlocks++;
                if (reported < 8) {
                    reportBlock(context, level, pos.immutable(), state);
                    reported++;
                }
            }
        }
        send(context, "nearby radius=" + radius + ", facadeBlocks=" + facadeBlocks + ", extensionBlocks=" + extensionBlocks + ", reported=" + reported);
        return 1;
    }

    private static int pos(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        reportBlock(context, context.getSource().getLevel(), pos, context.getSource().getLevel().getBlockState(pos));
        return 1;
    }

    private static void reportStack(CommandContext<CommandSourceStack> context, ItemStack stack) {
        send(context, "  item=" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + ", count=" + stack.getCount() + ", name=" + stack.getHoverName().getString() + ", tag=" + stack.getTag());
        if (stack.getItem() instanceof FacadeBlockItem facadeBlockItem && facadeBlockItem.getBlock() instanceof SimpleFacadeBlock facadeBlock) {
            int stateIndex = FacadeBlockItem.getFacadeState(stack);
            FacadeDefinition definition = facadeBlock.definition();
            definition.sourceBlock();
            send(context, "  facade source=" + definition.sourceId() + ", state=" + stateIndex + ", stateful=" + definition.stateful()
                    + ", displayStates=" + definition.displayStates().size() + ", sourceBlock=" + blockId(definition.sourceBlock()));
            send(context, "  display=" + BuiltInRegistries.BLOCK.getKey(definition.displayState(stateIndex).getBlock()) + " " + definition.displayState(stateIndex).getValues());
        }
    }

    private static void reportBlock(CommandContext<CommandSourceStack> context, Level level, BlockPos pos, BlockState state) {
        send(context, "block@" + pos.toShortString() + "=" + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " " + state.getValues());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (state.getBlock() instanceof SimpleFacadeBlock facadeBlock) {
            FacadeDefinition definition = facadeBlock.definition();
            definition.sourceBlock();
            send(context, "  facade source=" + definition.sourceId() + ", sourceBlock=" + blockId(definition.sourceBlock())
                    + ", stateful=" + definition.stateful() + ", displayStates=" + definition.displayStates().size());
            if (blockEntity instanceof FacadeBlockEntity facadeBlockEntity) {
                BlockState displayState = facadeBlockEntity.getDisplayState();
                send(context, "  beState=" + facadeBlockEntity.getFacadeState() + ", display=" + BuiltInRegistries.BLOCK.getKey(displayState.getBlock()) + " " + displayState.getValues());
            } else {
                send(context, "  missing FacadeBlockEntity, actualBE=" + (blockEntity == null ? "null" : blockEntity.getType()));
            }
        } else if (state.getBlock() instanceof FacadeExtensionBlock) {
            if (blockEntity instanceof FacadeExtensionBlockEntity extensionBlockEntity) {
                BlockState displayState = extensionBlockEntity.getDisplayState();
                send(context, "  extension main=" + extensionBlockEntity.getMainPos().toShortString() + ", visible=" + extensionBlockEntity.isVisible()
                        + ", display=" + BuiltInRegistries.BLOCK.getKey(displayState.getBlock()) + " " + displayState.getValues());
            } else {
                send(context, "  missing FacadeExtensionBlockEntity, actualBE=" + (blockEntity == null ? "null" : blockEntity.getType()));
            }
        }
    }

    private static int countBlocks(String namespace) {
        int count = 0;
        for (ResourceLocation key : ForgeRegistries.BLOCKS.getKeys()) {
            if (namespace.equals(key.getNamespace())) {
                count++;
            }
        }
        return count;
    }

    private static int countItems(String namespace) {
        int count = 0;
        for (ResourceLocation key : ForgeRegistries.ITEMS.getKeys()) {
            if (namespace.equals(key.getNamespace())) {
                count++;
            }
        }
        return count;
    }

    private static String blockId(Block block) {
        if (block == null) {
            return "null";
        }
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static String itemId(Item item) {
        if (item == null) {
            return "null";
        }
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        FacadeByPycoder.LOGGER.info("[fbpdebug] {}", message);
    }
}
