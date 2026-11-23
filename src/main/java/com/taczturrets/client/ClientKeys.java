package com.taczturrets.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.taczturrets.TaczTurrets;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Freely rebindable key mappings (options -> controls -> "TACZ Turrets").
 * <p>
 * They default to unbound on purpose: the same actions are also reachable with the mouse
 * (right click with a gun / empty hand), so no key is taken away from other mods.
 */
@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.MOD, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ClientKeys {
    public static final String CATEGORY = "key.categories.taczturrets";

    public static final KeyMapping INSTALL_GUN = new KeyMapping(
            "key.taczturrets.install_gun", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.taczturrets.open_menu", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping CYCLE_SCALE = new KeyMapping(
            "key.taczturrets.cycle_scale", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private ClientKeys() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(INSTALL_GUN);
        event.register(OPEN_MENU);
        event.register(CYCLE_SCALE);
    }
}
