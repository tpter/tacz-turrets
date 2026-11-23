package com.taczturrets.network;

import com.taczturrets.TaczTurrets;
import com.taczturrets.mount.MountedGunSyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the client-bound payload that keeps the mounted-gun render state in sync. */
@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class TurretPayloads {
    private TurretPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(MountedGunSyncPayload.TYPE, MountedGunSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> com.taczturrets.client.ClientMountedGuns.update(payload)));
    }
}
