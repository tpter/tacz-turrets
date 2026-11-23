package com.taczturrets.client;

import com.taczturrets.blockentity.AbstractTaczTurretBlockEntity;
import com.taczturrets.menu.TurretMenu;
import com.taczturrets.network.TurretConfigPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Turret inventory + tuning screen. The gun slot and nine ammo slots let the player load/unload by hand,
 * and the six numeric fields below tune the gun model (render scale, re-centring offset, idle angles) plus
 * four channel toggles. Each field sends a {@link TurretConfigPayload} only while a valid value is typed,
 * so the change is visible immediately - no restart and no drag-flood.
 */
public class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int SLOT_BORDER = 0xFF8B8B8B;
    private static final int SLOT_INNER = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int HINT = 0xFF707070;

    private static final int BOX_X = 70;
    private static final int BOX_W = 96;
    private static final int BOX_H = 16;
    private static final int CHANNEL_Y = 182;

    private final Button[] channelButtons = new Button[4];
    private final EditBox[] boxes = new EditBox[6];
    private final String[] labels = new String[]{"大小 Scale", "偏移 X", "偏移 Y", "偏移 Z", "初始偏转 Y", "初始俯仰 X"};
    private float scale = 1.0F;
    private float offX;
    private float offY;
    private float offZ;
    private float defYaw;
    private float defPitch;
    private int channel = 15;

    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 290;
        this.inventoryLabelY = 206;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();
        AbstractTaczTurretBlockEntity turret = this.menu.getTurret();
        if (turret != null) {
            this.scale = turret.getGunScale();
            this.offX = turret.getGunOffsetX();
            this.offY = turret.getGunOffsetY();
            this.offZ = turret.getGunOffsetZ();
            this.defYaw = turret.getDefaultYawDegrees();
            this.defPitch = turret.getDefaultPitchDegrees();
            this.channel = turret.getChannel();
        }
        // Numeric fields, top to bottom; each parses on the fly and only sends when the text is a valid
        // number inside [min, max]. No sliders, so no drag-flood of packets.
        this.boxes[0] = this.addField(0, this.scale, 0.25F, 3.0F, 58, v -> this.scale = v);
        this.boxes[1] = this.addField(1, this.offX, -2.0F, 2.0F, 78, v -> this.offX = v);
        this.boxes[2] = this.addField(2, this.offY, -2.0F, 2.0F, 98, v -> this.offY = v);
        this.boxes[3] = this.addField(3, this.offZ, -2.0F, 2.0F, 118, v -> this.offZ = v);
        this.boxes[4] = this.addField(4, this.defYaw, -180.0F, 180.0F, 138, v -> this.defYaw = v);
        this.boxes[5] = this.addField(5, this.defPitch, -90.0F, 90.0F, 158, v -> this.defPitch = v);

        for (int i = 0; i < 4; i++) {
            int bit = 1 << i;
            int bx = this.leftPos + 8 + i * 40;
            Button button = Button.builder(Component.literal(this.channelLabel(bit)), b -> {
                this.channel ^= bit;
                this.channel &= 15;
                this.sendConfig();
                this.refreshChannelButtons();
            }).bounds(bx, this.topPos + CHANNEL_Y, 36, 18).build();
            this.channelButtons[i] = button;
            this.addRenderableWidget(button);
        }
    }

    private EditBox addField(int index, float initial, float min, float max, int y,
            java.util.function.Consumer<Float> setter) {
        EditBox box = new EditBox(this.font, this.leftPos + BOX_X, this.topPos + y, BOX_W, BOX_H,
                Component.literal(this.labels[index]));
        box.setMaxLength(8);
        box.setValue(String.format(Locale.ROOT, "%.2f", initial));
        // Only allow a partial decimal number while typing; commit on every valid parsed value.
        box.setFilter(s -> s == null || s.isEmpty() || s.matches("-?[0-9]*\\.?[0-9]*"));
        box.setResponder(s -> {
            Float parsed = parse(s);
            if (parsed != null) {
                float clamped = Mth.clamp(parsed, min, max);
                setter.accept(clamped);
                this.sendConfig();
            }
        });
        this.addRenderableWidget(box);
        return box;
    }

    @Nullable
    private static Float parse(String s) {
        if (s == null || s.isEmpty() || s.equals("-") || s.equals(".") || s.equals("-.")) {
            return null;
        }
        try {
            return Float.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String channelLabel(int bit) {
        int number = Integer.numberOfTrailingZeros(bit) + 1;
        return (this.channel & bit) != 0 ? number + "\u2713" : Integer.toString(number);
    }

    private void refreshChannelButtons() {
        for (int i = 0; i < 4; i++) {
            this.channelButtons[i].setMessage(Component.literal(this.channelLabel(1 << i)));
        }
    }

    private void sendConfig() {
        AbstractTaczTurretBlockEntity turret = this.menu.getTurret();
        if (turret == null) {
            return;
        }
        PacketDistributor.sendToServer(new TurretConfigPayload(turret.getBlockPos(), this.scale, this.offX, this.offY,
                this.offZ, this.defYaw, this.defPitch, this.channel));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);
        graphics.fill(x, y, x + this.imageWidth, y + 2, PANEL_LIGHT);
        graphics.fill(x, y + this.imageHeight - 2, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_BORDER);
            graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_INNER);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        graphics.drawString(this.font, Component.translatable("taczturrets.menu.gun"), 8, 14, TEXT, false);
        graphics.drawString(this.font, Component.translatable("taczturrets.menu.ammo"), 8, 30, TEXT, false);
        // Field labels, vertically aligned with their EditBox (box is 16 px tall starting at topPos + y).
        int[] ys = new int[]{58, 78, 98, 118, 138, 158};
        for (int i = 0; i < ys.length; i++) {
            graphics.drawString(this.font, Component.literal(this.labels[i]), 8, ys[i] + 4, TEXT, false);
        }
        graphics.drawString(this.font, Component.translatable("taczturrets.menu.channel"), 8, CHANNEL_Y - 10, TEXT, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT, false);
    }
}
