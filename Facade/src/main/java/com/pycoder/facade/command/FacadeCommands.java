package com.pycoder.facade.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.pycoder.facade.block.FacadeBlocks;
import com.pycoder.facade.block.FacadeDefinition;
import com.pycoder.facade.config.ModConfig;
import com.pycoder.facade.item.FacadeBlockItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class FacadeCommands {
    private static final DynamicCommandExceptionType UNKNOWN_FACADE = new DynamicCommandExceptionType(
            value -> Component.translatable("commands.facadebypycoder.fbpget.unknown", value)
    );
    private FacadeCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("fbpget")
                .requires(FacadeCommands::canUseFbpget)
                .then(Commands.argument("target", StringArgumentType.greedyString())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(suggestions(), builder))
                        .executes(FacadeCommands::giveTarget)));
    }

    private static boolean canUseFbpget(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return false;
        }
        GameType gameType = player.gameMode.getGameModeForPlayer();
        return gameType == GameType.SURVIVAL && ModConfig.ENABLE_IN_SURVIVAL.get()
                || gameType == GameType.ADVENTURE && ModConfig.ENABLE_IN_ADVENTURE.get();
    }

    private static int giveTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String target = StringArgumentType.getString(context, "target");
        ItemStack stack = createStack(target);
        MutableComponent stackName = stack.getHoverName().copy();
        boolean added = player.getInventory().add(stack);
        if (!added) {
            player.drop(stack, false);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.facadebypycoder.fbpget.success", stackName), true);
        return 1;
    }

    private static ItemStack createStack(String target) throws CommandSyntaxException {
        if ("stick".equals(target)) {
            return new ItemStack(FacadeBlocks.PROJECTION_TOOL);
        }
        ParsedTarget parsedTarget = parseTarget(target);
        FacadeBlocks.refreshDefinitions();
        FacadeDefinition definition = FacadeBlocks.definition(parsedTarget.sourceId());
        Item item = FacadeBlocks.item(parsedTarget.sourceId());
        if (definition == null || item == null) {
            throw UNKNOWN_FACADE.create(parsedTarget.sourceId().toString());
        }
        definition.sourceBlock();
        ItemStack stack = new ItemStack(item);
        FacadeBlockItem.setFacadeState(stack, 0);
        return stack;
    }

    private static ParsedTarget parseTarget(String rawTarget) throws CommandSyntaxException {
        String target = rawTarget.trim();
        int stateStart = target.indexOf("[state:");
        String stateTerminator = "]";
        if (stateStart < 0) {
            stateStart = target.indexOf("(state:");
            stateTerminator = ")";
        }
        if (stateStart >= 0) {
            if (!target.endsWith(stateTerminator)) {
                throw UNKNOWN_FACADE.create(rawTarget);
            }
            target = target.substring(0, stateStart);
        }
        ResourceLocation sourceId = ResourceLocation.tryParse(target);
        if (sourceId == null) {
            throw UNKNOWN_FACADE.create(rawTarget);
        }
        return new ParsedTarget(sourceId);
    }

    private static Iterable<String> suggestions() {
        FacadeBlocks.refreshDefinitions();
        List<String> values = new ArrayList<>();
        values.add("stick");
        for (FacadeDefinition definition : FacadeBlocks.definitions()) {
            String sourceId = definition.sourceId().toString();
            values.add(sourceId);
        }
        values.sort(String::compareTo);
        return values;
    }

    private record ParsedTarget(ResourceLocation sourceId) {
    }
}
