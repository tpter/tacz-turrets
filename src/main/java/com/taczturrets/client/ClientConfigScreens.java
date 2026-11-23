package com.taczturrets.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Hooks the mod's own settings page into the mod list ("Config" button). */
public final class ClientConfigScreens {
    private ClientConfigScreens() {
    }

    public static void register(ModContainer container) {
        IConfigScreenFactory factory = (mod, parent) -> new TurretSettingsScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (java.util.function.Supplier<IConfigScreenFactory>) () -> factory);
    }
}
