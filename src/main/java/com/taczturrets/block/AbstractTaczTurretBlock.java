package com.taczturrets.block;

import com.taczturrets.TaczTurrets;
import com.taczturrets.blockentity.AbstractTaczTurretBlockEntity;
import com.taczturrets.gun.GunInstall;
import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.Direction;

/**
 * Shared block behaviour of the three TACZ turret bases.
 * <p>
 * Right click with a TACZ gun installs it (the old gun is given back); right click with TACZ ammo or an
 * ammo box fills the internal ammo slots; shift + right click with a gun cycles the render scale;
 * right click with an empty hand opens the turret inventory menu. Right click with a Create wrench
 * re-points the turret at the clicked face (see {@link #getRotatedBlockState}).
 */
public abstract class AbstractTaczTurretBlock extends AbstractTurretBlock implements IWrenchable {
    protected AbstractTaczTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Create wrench rotation: right-clicking a face with the wrench points the turret barrel at that
     * face, giving full 6-direction control (including up/down) without re-placing the block. Shift +
     * wrench keeps the default {@link IWrenchable#onSneakWrenched} behaviour (pick the block up).
     */
    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        return state.setValue(FACING, targetedFace);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof AbstractTaczTurretBlockEntity turret) {
                turret.tick();
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AbstractTaczTurretBlockEntity turret)) {
            TaczTurrets.LOGGER.warn("[taczturrets] right click at {} had no TACZ turret block entity", pos);
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (stack.getItem() instanceof IGun) {
            if (!level.isClientSide()) {
                if (player.isShiftKeyDown()) {
                    float scale = turret.cycleGunScale();
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("taczturrets.message.gun_scale", String.format("%.2f", scale)), true);
                    turret.notifyUpdate();
                } else {
                    ItemStack old = turret.getGunStack();
                    turret.setGunStack(GunInstall.stripAmmo(stack.copyWithCount(1)));
                    if (!player.isCreative()) {
                        if (stack.getCount() <= 1) {
                            player.setItemInHand(hand, ItemStack.EMPTY);
                        } else {
                            stack.shrink(1);
                        }
                        if (!old.isEmpty()) {
                            player.getInventory().placeItemBackInInventory(old);
                        }
                    }
                    turret.notifyUpdate();
                    TaczTurrets.LOGGER.info("[taczturrets] installed gun {} on turret at {} (creative={})",
                            stack.getHoverName().getString(), pos, player.isCreative());
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("taczturrets.message.gun_installed",
                                    stack.getHoverName()), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.getItem() instanceof IAmmo || stack.getItem() instanceof IAmmoBox) {
            if (!level.isClientSide()) {
                ItemStack before = stack.copy();
                ItemStack rest = turret.storeAmmo(player.isCreative() ? stack.copy() : stack);
                int moved = before.getCount() - rest.getCount();
                if (!player.isCreative()) {
                    player.setItemInHand(hand, rest);
                }
                turret.notifyUpdate();
                TaczTurrets.LOGGER.info("[taczturrets] ammo {} on turret at {}: moved={} remainder={}",
                        before.getHoverName().getString(), pos, moved, rest.getCount());
                if (moved > 0) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("taczturrets.message.ammo_added", moved), true);
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("taczturrets.message.ammo_rejected"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        // Any other non-empty hand (Create wrench, block to place, ...) must reach the ITEM's own useOn
        // instead of AeroIE's PASS_TO_DEFAULT_BLOCK_INTERACTION, which routes to useWithoutItem and would
        // open the turret menu. SKIP_DEFAULT_BLOCK_INTERACTION skips useWithoutItem and lets the item act,
        // so a Create wrench rotates the FACING (IWrenchable) instead of opening the screen.
        if (!stack.isEmpty()) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Sneak + right click with an empty hand resizes the rendered gun; a plain empty-hand click
        // opens the AeroIE turret inventory (ammo slots).
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof AbstractTaczTurretBlockEntity turret) {
            if (!level.isClientSide()) {
                float scale = turret.cycleGunScale();
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("taczturrets.message.gun_scale", String.format("%.2f", scale)), true);
                turret.notifyUpdate();
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // In-game guidance, so the interaction rules are discoverable without reading the docs.
        if (!level.isClientSide() && placer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("taczturrets.message.placement_hint"), false);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AbstractTaczTurretBlockEntity turret) {
            ItemStack gun = turret.getGunStack();
            if (!gun.isEmpty()) {
                turret.setGunStack(ItemStack.EMPTY);
                Block.popResource(level, pos, gun);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
