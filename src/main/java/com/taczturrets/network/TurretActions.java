package com.taczturrets.network;

import com.taczturrets.TaczTurrets;
import com.taczturrets.blockentity.AbstractTaczTurretBlockEntity;
import com.taczturrets.gun.GunInstall;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Server side implementation of the key-bound turret actions. */
public final class TurretActions {
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private TurretActions() {
    }

    public static void run(Player player, BlockPos pos, byte action) {
        if (player.level().isClientSide()) {
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof AbstractTaczTurretBlockEntity turret)) {
            // A CBC cannon mount carries no block entity of ours; the mounted gun lives in saved data.
            if (player instanceof ServerPlayer serverPlayer
                    && player.level().getBlockEntity(pos) instanceof rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity) {
                com.taczturrets.mount.MountedGunHandler.handleAction(serverPlayer, pos, action);
            }
            return;
        }
        switch (action) {
            case TurretActionPayload.INSTALL_GUN -> installGun(player, turret);
            case TurretActionPayload.OPEN_MENU -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu((MenuProvider) turret, buffer -> buffer.writeBlockPos(pos));
                }
            }
            case TurretActionPayload.CYCLE_SCALE -> {
                float scale = turret.cycleGunScale();
                player.displayClientMessage(
                        Component.translatable("taczturrets.message.gun_scale", String.format("%.2f", scale)), true);
                turret.notifyUpdate();
            }
            default -> {
            }
        }
    }

    private static void installGun(Player player, AbstractTaczTurretBlockEntity turret) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof IGun)) {
            player.displayClientMessage(Component.translatable("taczturrets.message.no_gun_in_hand"), true);
            return;
        }
        ItemStack old = turret.getGunStack();
        turret.setGunStack(GunInstall.stripAmmo(held.copyWithCount(1)));
        if (!player.isCreative()) {
            held.shrink(1);
            if (!old.isEmpty()) {
                player.getInventory().placeItemBackInInventory(old);
            }
        }
        turret.notifyUpdate();
        TaczTurrets.LOGGER.info("[taczturrets] keybind installed gun {} on turret at {} (creative={})",
                held.getHoverName().getString(), turret.getBlockPos(), player.isCreative());
        player.displayClientMessage(
                Component.translatable("taczturrets.message.gun_installed", held.getHoverName()), true);
    }

    /** Applies a {@link TurretConfigPayload} from the turret screen to the target block entity. */
    public static void applyConfig(Player player, TurretConfigPayload payload) {
        if (player.level().isClientSide()) {
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > MAX_DISTANCE_SQR) {
            return;
        }
        if (player.level().getBlockEntity(payload.pos()) instanceof AbstractTaczTurretBlockEntity turret) {
            turret.applyConfig(payload.scale(), payload.offsetX(), payload.offsetY(), payload.offsetZ(),
                    payload.defaultYaw(), payload.defaultPitch(), payload.channel());
        }
    }
}
