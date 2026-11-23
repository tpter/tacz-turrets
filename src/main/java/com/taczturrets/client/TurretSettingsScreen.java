package com.taczturrets.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.taczturrets.TaczTurrets;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The mod's own settings page, reachable from the mod list ("Config").
 * <p>
 * The three turret actions can be bound here directly, so they do not have to be found in the long
 * global key binding list. Assigning a key saves {@code options.txt} immediately.
 */
public class TurretSettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 26;

    private final Screen parent;
    @Nullable
    private KeyMapping listening;
    private final Button[] keyButtons = new Button[3];

    public TurretSettingsScreen(Screen parent) {
        super(Component.translatable("taczturrets.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 150;
        int buttonX = this.width / 2 + 10;
        int y = 76;
        this.keyButtons[0] = this.addRenderableWidget(Button.builder(label(ClientKeys.INSTALL_GUN), b -> this.listen(ClientKeys.INSTALL_GUN))
                .bounds(buttonX, y, 140, 20).build());
        y += ROW_HEIGHT;
        this.keyButtons[1] = this.addRenderableWidget(Button.builder(label(ClientKeys.OPEN_MENU), b -> this.listen(ClientKeys.OPEN_MENU))
                .bounds(buttonX, y, 140, 20).build());
        y += ROW_HEIGHT;
        this.keyButtons[2] = this.addRenderableWidget(Button.builder(label(ClientKeys.CYCLE_SCALE), b -> this.listen(ClientKeys.CYCLE_SCALE))
                .bounds(buttonX, y, 140, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 60, this.height - 30, 120, 20).build());
    }

    private Component label(KeyMapping mapping) {
        if (this.listening == mapping) {
            return Component.translatable("taczturrets.screen.press_key");
        }
        return mapping.isUnbound()
                ? Component.translatable("taczturrets.screen.unbound")
                : mapping.getTranslatedKeyMessage();
    }

    private void listen(KeyMapping mapping) {
        this.listening = mapping;
        this.refreshLabels();
    }

    private void refreshLabels() {
        KeyMapping[] mappings = {ClientKeys.INSTALL_GUN, ClientKeys.OPEN_MENU, ClientKeys.CYCLE_SCALE};
        for (int i = 0; i < mappings.length; i++) {
            this.keyButtons[i].setMessage(this.label(mappings[i]));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.listening != null) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.listening = null;
                this.refreshLabels();
                return true;
            }
            this.listening.setKey(InputConstants.getKey(keyCode, scanCode));
            KeyMapping.resetMapping();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options != null) {
                minecraft.options.save();
            }
            TaczTurrets.LOGGER.info("[taczturrets] bound {} to {}", this.listening.getName(), this.listening.getTranslatedKeyMessage().getString());
            this.listening = null;
            this.refreshLabels();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("taczturrets.screen.hint"), this.width / 2 - 150, 44, 0xA0A0A0, false);
        int left = this.width / 2 - 150;
        int y = 76;
        graphics.drawString(this.font, Component.translatable("key.taczturrets.install_gun"), left, y + 6, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("key.taczturrets.open_menu"), left, y + ROW_HEIGHT + 6, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("key.taczturrets.cycle_scale"), left, y + ROW_HEIGHT * 2 + 6, 0xFFFFFF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
