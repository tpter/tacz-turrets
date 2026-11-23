package com.taczturrets.network;

import com.taczturrets.TaczTurrets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the mod's custom payloads on the mod event bus. */
@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class TurretNetwork {
    private TurretNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                TurretActionPayload.TYPE,
                TurretActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> TurretActions.run(context.player(), payload.pos(), payload.action())));
        event.registrar("1").playToServer(
                TurretConfigPayload.TYPE,
                TurretConfigPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> TurretActions.applyConfig(context.player(), payload)));
    }
}
