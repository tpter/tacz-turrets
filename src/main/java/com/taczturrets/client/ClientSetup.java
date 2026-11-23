package com.taczturrets.client;

import com.taczturrets.TaczTurrets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TaczTurrets.FIXED_TURRET_BE.get(), TurretGunRenderer::new);
        event.registerBlockEntityRenderer(TaczTurrets.SEAT_TURRET_BE.get(), TurretGunRenderer::new);
        event.registerBlockEntityRenderer(TaczTurrets.CBC_TURRET_BE.get(), TurretGunRenderer::new);
        // The shooter entity must have a renderer registered or the render dispatcher NPEs when it
        // resolves the entity type; it renders nothing (see TurretShooterRenderer).
        event.registerEntityRenderer(TaczTurrets.TURRET_SHOOTER.get(), TurretShooterRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TaczTurrets.TURRET_MENU.get(), TurretScreen::new);
    }
}
