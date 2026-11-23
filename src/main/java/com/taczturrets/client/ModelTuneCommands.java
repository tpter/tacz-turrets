package com.taczturrets.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import com.taczturrets.TaczTurrets;

import java.util.Locale;

/**
 * Client-only command {@code /tczmodel} to tune the turret gun models while playing.
 * <p>
 * Values are applied on the next frame by the block entity renderers, so there is no restart, no resource reload and
 * nothing to reinstall: stand in front of the turret, run e.g. {@code /tczmodel add seat yaw 15}, and it moves.
 * Use {@code /tczmodel save} to keep the numbers in {@code config/taczturrets-model.json}.
 */
@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ModelTuneCommands {
    private ModelTuneCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        TurretModelConfig.load();
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tczmodel")
                .executes(context -> list(context.getSource()))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("save").executes(context -> {
                    TurretModelConfig.save();
                    context.getSource().sendSuccess(
                            () -> Component.literal("model angles saved to config/taczturrets-model.json"), false);
                    return 1;
                }))
                .then(Commands.literal("reload").executes(context -> {
                    TurretModelConfig.reload();
                    context.getSource().sendSuccess(() -> Component.literal(TurretModelConfig.describe()), false);
                    return 1;
                }))
                .then(Commands.literal("set")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.List.of("fixed", "seat", "mount"), builder))
                                .then(Commands.argument("axis", StringArgumentType.word())
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                java.util.List.of("yaw", "pitch", "roll"), builder))
                                        .then(Commands.argument("degrees", FloatArgumentType.floatArg(-360.0F, 360.0F))
                                                .executes(context -> apply(context.getSource(),
                                                        StringArgumentType.getString(context, "target"),
                                                        StringArgumentType.getString(context, "axis"),
                                                        FloatArgumentType.getFloat(context, "degrees"), true))))))
                .then(Commands.literal("add")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.List.of("fixed", "seat", "mount"), builder))
                                .then(Commands.argument("axis", StringArgumentType.word())
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                java.util.List.of("yaw", "pitch", "roll"), builder))
                                        .then(Commands.argument("delta", FloatArgumentType.floatArg(-180.0F, 180.0F))
                                                .executes(context -> apply(context.getSource(),
                                                        StringArgumentType.getString(context, "target"),
                                                        StringArgumentType.getString(context, "axis"),
                                                        FloatArgumentType.getFloat(context, "delta"), false)))))));
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("turrets: " + TurretModelConfig.describe()
                + "  (/tczmodel set <fixed|seat|mount> <yaw|pitch|roll> <deg>, /tczmodel save)"), false);
        return 1;
    }

    private static int apply(CommandSourceStack source, String target, String axis, float value, boolean absolute) {
        if (!absolute) {
            float current = TurretModelConfig.get(target, axis);
            if (Float.isNaN(current)) {
                source.sendFailure(Component.literal("unknown target/axis: " + target + " " + axis
                        + " (targets: fixed, seat, mount / axes: yaw, pitch, roll)"));
                return 0;
            }
            value = current + value;
        }
        if (!TurretModelConfig.set(target, axis, value)) {
            source.sendFailure(Component.literal("unknown target/axis: " + target + " " + axis
                    + " (targets: fixed, seat, mount / axes: yaw, pitch, roll)"));
            return 0;
        }
        float applied = TurretModelConfig.get(target, axis);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "%s %s = %.1f", target, axis, applied)), false);
        return 1;
    }
}
