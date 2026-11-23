package com.taczturrets.mount;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.taczturrets.TaczTurrets;
import com.taczturrets.gun.GunInstall;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

/**
 * Glue between a Create: Big Cannons cannon mount and a TACZ gun.
 * <p>
 * No block is placed on the mount and CBC is not modified: the gun lives in {@link MountedGuns}, is installed by
 * right clicking the mount while holding a TACZ gun, is rotated by the mount's own yaw/pitch shaft speeds (the same
 * {@code convertToAngular(speed) * 0.125} math CBC applies) and fires while the mount's redstone fire input is on.
 * A Create mechanical arm fills the nine ammo slots through our extra item handler capability.
 */
@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MountedGunHandler {
    private MountedGunHandler() {
    }

    // ------------------------------------------------------------------ capability

    /** Extra item handler for CBC's cannon mount, used only when CBC itself returns none (no cannon contraption). */
    @Nullable
    public static IItemHandler getItemHandler(@Nullable BlockEntity blockEntity, @Nullable Direction side) {
        if (!(blockEntity instanceof CannonMountBlockEntity mount)) {
            return null;
        }
        Level level = mount.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        MountedGuns data = MountedGuns.get(serverLevel);
        MountedGun gun = data.get(mount.getBlockPos());
        if (gun == null) {
            // Create an entry on the first inventory probe instead of requiring a mounted gun first: a
            // mechanical arm that looks at the mount before the gun is installed used to see "no inventory"
            // and never retried, which made the mount impossible to fill automatically. Empty entries are
            // skipped when saving (MountedGuns#save), so this cannot corrupt the save.
            gun = data.getOrCreate(mount.getBlockPos(), initialYaw(mount));
        }
        return gun.getAmmo();
    }

    // ------------------------------------------------------------------ interactions

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // The event is fired once per hand; only the main hand drives install / ammo / removal.
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof CannonMountBlockEntity mount)) {
            return;
        }
        ItemStack held = event.getItemStack();
        boolean hasGun = held.getItem() instanceof IGun;
        boolean hasAmmo = held.getItem() instanceof IAmmo || held.getItem() instanceof IAmmoBox;
        boolean removing = held.isEmpty() && event.getEntity().isShiftKeyDown();
        boolean inspecting = held.isEmpty() && !event.getEntity().isShiftKeyDown();
        if (!hasGun && !hasAmmo && !removing && !inspecting) {
            return;
        }

        MountedGuns data = MountedGuns.get(serverLevel);
        BlockPos pos = mount.getBlockPos();
        MountedGun gun = data.get(pos);
        if (gun == null || gun.getGunStack().isEmpty()) {
            if (inspecting) {
                // Empty hand: report what the mount currently holds, so the player can tell whether ammo arrived
                // (for example from a mechanical arm) without opening any menu.
                showStatus(event.getEntity(), gun);
                return;
            }
            if (!hasGun) {
                // Ammo or removal without a mounted gun must not create an empty entry - an entry with an
                // empty gun used to crash the world save with "Cannot encode empty ItemStack".
                event.getEntity().displayClientMessage(
                        Component.translatable("taczturrets.message.mount_no_gun").withStyle(ChatFormatting.RED), true);
                return;
            }
            gun = data.getOrCreate(pos, initialYaw(mount));
        }
        boolean handled;
        if (hasGun) {
            handled = installGun(serverLevel, event.getEntity(), event.getHand(), pos, gun, held);
        } else if (hasAmmo) {
            handled = insertAmmo(event.getEntity(), event.getHand(), pos, gun, held);
        } else if (inspecting) {
            showStatus(event.getEntity(), gun);
            return;
        } else {
            handled = removeGun(serverLevel, data, pos, gun, event.getEntity());
        }
        if (!handled) {
            return;
        }
        serverLevel.invalidateCapabilities(pos);
        data.markChanged();
        send(serverLevel, pos, gun);
        event.setCanceled(true);
        event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
        event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
    }

    /**
     * Entry point for the rebindable turret key actions when the player's crosshair is on a CBC cannon mount.
     * Without this the key-bound install silently did nothing on a mount (only block turrets were handled).
     *
     * @return {@code true} when a cannon mount handled the action
     */
    public static boolean handleAction(net.minecraft.server.level.ServerPlayer player, BlockPos pos, byte action) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof CannonMountBlockEntity mount)) {
            return false;
        }
        MountedGuns data = MountedGuns.get(level);
        MountedGun gun = data.get(pos);
        switch (action) {
            case com.taczturrets.network.TurretActionPayload.INSTALL_GUN -> {
                ItemStack held = player.getMainHandItem();
                if (held.isEmpty() || !(held.getItem() instanceof IGun)) {
                    player.displayClientMessage(Component.translatable("taczturrets.message.no_gun_in_hand"), true);
                    return true;
                }
                if (gun == null) {
                    gun = data.getOrCreate(pos, initialYaw(mount));
                }
                if (installGun(level, player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, gun, held)) {
                    data.markChanged();
                    send(level, pos, gun);
                }
                return true;
            }
            case com.taczturrets.network.TurretActionPayload.CYCLE_SCALE -> {
                if (gun == null || gun.getGunStack().isEmpty()) {
                    player.displayClientMessage(
                            Component.translatable("taczturrets.message.mount_no_gun").withStyle(ChatFormatting.RED), true);
                    return true;
                }
                float scale = gun.cycleScale();
                player.displayClientMessage(
                        Component.translatable("taczturrets.message.gun_scale", String.format(java.util.Locale.ROOT, "%.2f", scale)), true);
                data.markChanged();
                send(level, pos, gun);
                return true;
            }
            case com.taczturrets.network.TurretActionPayload.OPEN_MENU -> {
                if (gun == null) {
                    showStatus(player, null);
                } else {
                    showStatus(player, gun);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Prints the current gun and ammo state of a mount into the action bar. Used by an empty-hand click and by the
     * "open turret inventory" key so the player can check whether a mechanical arm actually delivered ammo.
     */
    public static void showStatus(net.minecraft.world.entity.player.Player player, @Nullable MountedGun gun) {
        if (gun == null || gun.getGunStack().isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("taczturrets.message.mount_no_gun").withStyle(ChatFormatting.RED), true);
            return;
        }
        String gunName = gun.getGunStack().getHoverName().getString();
        player.displayClientMessage(Component.translatable("taczturrets.message.mount_status", gunName, totalAmmoText(gun)),
                true);
    }

    /** Total rounds stored in the nine slots; an infinite (creative mode) ammo box is shown as an infinity symbol. */
    private static String totalAmmoText(MountedGun gun) {
        int loose = 0;
        boolean infinite = false;
        for (int slot = 0; slot < gun.getAmmo().getSlots(); slot++) {
            ItemStack stack = gun.getAmmo().getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof IAmmoBox box) {
                if (box.isCreative(stack) || box.isAllTypeCreative(stack)) {
                    infinite = true;
                } else {
                    loose += box.getAmmoCount(stack);
                }
            } else if (stack.getItem() instanceof IAmmo) {
                loose += stack.getCount();
            }
        }
        return infinite ? "\u221e" : Integer.toString(loose);
    }

    private static boolean installGun(ServerLevel level, net.minecraft.world.entity.player.Player player,
                                      net.minecraft.world.InteractionHand hand, BlockPos pos, MountedGun gun, ItemStack held) {
        ItemStack previous = gun.getGunStack();
        gun.setGunStack(GunInstall.stripAmmo(held.copyWithCount(1)));
        ItemStack picked = gun.getGunStack();
        if (!player.isCreative()) {
            if (held.getCount() <= 1) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            } else {
                held.shrink(1);
            }
            if (!previous.isEmpty()) {
                player.getInventory().placeItemBackInInventory(previous);
            }
        }
        player.getInventory().setChanged();
        TaczTurrets.LOGGER.info("[taczturrets] mounted gun {} on CBC cannon mount at {}", picked.getHoverName().getString(), pos);
        player.displayClientMessage(
                Component.translatable("taczturrets.message.gun_installed", picked.getHoverName()), true);
        level.invalidateCapabilities(pos);
        return true;
    }

    private static boolean insertAmmo(net.minecraft.world.entity.player.Player player,
                                      net.minecraft.world.InteractionHand hand, BlockPos pos, MountedGun gun, ItemStack held) {
        ItemStack before = held.copy();
        ItemStack remainder = held.copy();
        for (int slot = 0; slot < gun.getAmmo().getSlots() && !remainder.isEmpty(); slot++) {
            remainder = gun.getAmmo().insertItem(slot, remainder, false);
        }
        int moved = before.getCount() - remainder.getCount();
        if (moved <= 0) {
            player.displayClientMessage(Component.translatable("taczturrets.message.ammo_rejected")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!player.isCreative()) {
            player.setItemInHand(hand, remainder);
        }
        TaczTurrets.LOGGER.info("[taczturrets] loaded {} x{} into CBC cannon mount at {}", before.getHoverName().getString(), moved, pos);
        player.displayClientMessage(
                Component.translatable("taczturrets.message.ammo_added_total", moved, totalAmmoText(gun)), true);
        return true;
    }

    private static boolean removeGun(ServerLevel level, MountedGuns data, BlockPos pos, MountedGun gun,
                                     net.minecraft.world.entity.player.Player player) {
        if (gun.getGunStack().isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("taczturrets.message.mount_no_gun").withStyle(ChatFormatting.RED), true);
            return false;
        }
        for (ItemStack drop : data.removeAndCollectDrops(pos)) {
            Block.popResource(level, pos, drop);
        }
        player.displayClientMessage(Component.translatable("taczturrets.message.gun_removed"), true);
        return true;
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            MountedGuns data = MountedGuns.get(level);
            if (data.view().isEmpty()) {
                continue;
            }
            // Copy the keys: removing an entry (broken mount) mutates the map while iterating.
            for (BlockPos pos : java.util.List.copyOf(data.view().keySet())) {
                MountedGun gun = data.get(pos);
                if (gun == null) {
                    continue;
                }
                if (!level.isLoaded(pos)) {
                    continue;
                }
                if (!(level.getBlockEntity(pos) instanceof CannonMountBlockEntity mount)) {
                    for (ItemStack drop : data.removeAndCollectDrops(pos)) {
                        Block.popResource(level, pos, drop);
                    }
                    continue;
                }
                tickMount(level, mount, gun);
            }
        }
    }

    private static void tickMount(ServerLevel level, CannonMountBlockEntity mount, MountedGun gun) {
        if (!gun.getGunStack().isEmpty()) {
            float yawStep = mount.getAngularSpeed(-mount.getYawSpeed(), 0.0F);
            float pitchStep = mount.getAngularSpeed(mount.getPitchSpeed(), 0.0F);
            int pitchSign = pitchSign(mount);
            if (yawStep != 0.0F || pitchStep != 0.0F) {
                gun.applyMountRotation(yawStep, pitchStep, pitchSign);
            }
            boolean fire = mount.getBlockState().getValue(CannonMountBlock.FIRE_POWERED)
                    || level.hasNeighborSignal(mount.getBlockPos());
            if (fire) {
                // Pass the WORLD direction so bullets fly along the ship-rotated barrel, not the local axis.
                BlockPos pos = mount.getBlockPos();
                Vec3 worldDir = worldDirection(level, pos, gun);
                gun.shoot(level, worldGunPosition(level, pos).add(worldDir.scale(1.2D)), worldDir);
            }
        }
        gun.tickShooter();
        if (gun.isDirty() || level.getGameTime() % 20L == 0L) {
            send(level, mount.getBlockPos(), gun);
            gun.markSynced();
        }
    }

    private static int pitchSign(CannonMountBlockEntity mount) {
        Direction dir = mount.getContraptionDirection();
        boolean positiveAxis = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        boolean xAxis = dir.getAxis() == Direction.Axis.X;
        return positiveAxis == xAxis ? 1 : -1;
    }

    private static float initialYaw(CannonMountBlockEntity mount) {
        if (mount.getBlockState().hasProperty(CannonMountBlock.HORIZONTAL_FACING)) {
            return mount.getBlockState().getValue(CannonMountBlock.HORIZONTAL_FACING).toYRot();
        }
        return 180.0F;
    }

    /**
     * WORLD position of the gun pivot (local mount center + 1.55 up). When the mount is on a Sable sub-level the
     * local position is rotated/translated by the ship pose, otherwise it is used as-is.
     */
    private static Vec3 worldGunPosition(Level level, BlockPos pos) {
        Vec3 local = Vec3.atCenterOf(pos).add(0.0D, 1.55D, 0.0D);
        SubLevel ship = ServerShipUtils.getSubLevelAtBlockPos(level, pos);
        return ship == null ? local : ship.logicalPose().transformPosition(local);
    }

    /** WORLD aim direction (the mount's local {@code gun.direction()} rotated by the ship pose). */
    private static Vec3 worldDirection(Level level, BlockPos pos, MountedGun gun) {
        Vec3 dir = gun.direction();
        SubLevel ship = ServerShipUtils.getSubLevelAtBlockPos(level, pos);
        if (ship == null) {
            return dir;
        }
        org.joml.Vector3d world = ship.logicalPose()
                .transformNormal(new org.joml.Vector3d(dir.x, dir.y, dir.z))
                .normalize();
        return new Vec3(world.x, world.y, world.z);
    }

    private static void send(ServerLevel level, BlockPos pos, MountedGun gun) {
        Vec3 worldPos = worldGunPosition(level, pos);
        Vec3 worldDir = worldDirection(level, pos, gun);
        // Convert the world direction back to renderer yaw/pitch (the same convention the client uses:
        // directionFromRotation(-pitch, yaw)). These are WORLD angles so a ship-mounted gun follows the ship.
        float worldYaw = (float) Math.toDegrees(Math.atan2(-worldDir.x, worldDir.z));
        float worldPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(worldDir.y, -1.0D, 1.0D)));
        MountedGunSyncPayload payload = new MountedGunSyncPayload(pos, gun.getGunStack(), worldYaw, worldPitch,
                gun.getScale(), gun.getGunStack().isEmpty(), worldPos.x, worldPos.y, worldPos.z);
        PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(pos), payload);
    }
}
