package com.taczturrets.client;

import com.taczturrets.TaczTurrets;
import com.taczturrets.blockentity.AbstractTaczTurretBlockEntity;
import com.taczturrets.network.TurretActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Turns the key mappings into turret action packets for whichever turret the crosshair is on. */
@EventBusSubscriber(modid = TaczTurrets.MODID, value = Dist.CLIENT)
public final class ClientKeyHandler {
    private ClientKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ClientKeys.INSTALL_GUN.consumeClick()) {
            send(TurretActionPayload.INSTALL_GUN);
        }
        while (ClientKeys.OPEN_MENU.consumeClick()) {
            send(TurretActionPayload.OPEN_MENU);
        }
        while (ClientKeys.CYCLE_SCALE.consumeClick()) {
            send(TurretActionPayload.CYCLE_SCALE);
        }
    }

    private static void send(byte action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        BlockPos pos = targetedTurret(minecraft);
        if (pos == null) {
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("taczturrets.message.no_turret_targeted"), true);
            return;
        }
        PacketDistributor.sendToServer(new TurretActionPayload(pos, action));
    }

    private static BlockPos targetedTurret(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        var blockEntity = minecraft.level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractTaczTurretBlockEntity) {
            return pos;
        }
        // CBC cannon mount: the gun is stored in saved data, not in a block entity of ours.
        if (blockEntity instanceof rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity) {
            return pos;
        }
        return null;
    }
}
