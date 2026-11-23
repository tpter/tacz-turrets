package com.taczturrets.blockentity;

import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.taczturrets.TaczTurrets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Turret base #1: the fixed AeroIE turret.
 * <p>
 * The barrel is bolted to the physics block: its pitch is always zero and its yaw is the direction the
 * block was placed with, so the gun keeps pointing away from the hull and only turns when the ship itself
 * turns. It fires while the player in a linked AeroIE control seat holds the fire input (no redstone).
 */
public class FixedTurretBlockEntity extends AbstractTaczTurretBlockEntity {
    public FixedTurretBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this(TaczTurrets.FIXED_TURRET_BE.get(), pos, state);
    }

    public FixedTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------ gun model angles (edit here)
    // Rotation of the rendered gun model around the turret origin, in degrees. Changing these two numbers
    // is all that is needed to turn / tilt the fixed turret's gun model.
    @Override
    public float getGunModelYawDegrees() {
        return 180.0F;
    }

    @Override
    public float getGunModelPitchDegrees() {
        return 0.0F;
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    protected void turretTick(Level level) {
        this.prevxrot = this.xRot0;
        this.prevyrot = this.yRot0;

        // Aim along the direction the block was placed with, in the plain MC angle convention used by this
        // mod (Direction#toYRot: south = 0, west = 90, north = 180, east = -90). The gun therefore points
        // away from the hull instead of always pointing south, which used to put the muzzle inside the ship
        // and made every shot die inside a block.
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        float baseYaw = facing.getAxis().isHorizontal() ? facing.toYRot() : 0.0F;
        // UP / DOWN placement: point the barrel straight up / down (pitch is the X rotation in our frame).
        float basePitch = 0.0F;
        if (facing == Direction.UP) {
            basePitch = 90.0F;
        } else if (facing == Direction.DOWN) {
            basePitch = -90.0F;
        }
        this.targetxrot = (float) Math.toRadians(basePitch);
        this.targetyrot = (float) Math.toRadians(baseYaw);
        this.xRot0 = this.targetxrot;
        this.yRot0 = this.targetyrot;
        this.xOK = true;
        this.yOK = true;

        ControlSeatServerData seat = this.resolveLinkedSeat(level);
        // Weapon channel gate: the fixed turret only fires while its channel intersects the seat's
        // selected channel(s). Its barrel stays on FACING regardless (it is a fixed turret).
        boolean firing = seat != null && seat.player != null && seat.isfiring && this.channelMatches(seat);
        this.lastPowered = firing;
        if (firing) {
            this.shootWithTacz();
        } else if (level.getGameTime() % 100L == 0L) {
            this.hint(level, this.getGunStack().isEmpty()
                    ? "taczturrets.message.fixed_no_gun"
                    : "taczturrets.message.fixed_needs_seat");
        }
    }

    /** Action-bar guidance for players standing next to this turret (at most once per 5 seconds). */
    private void hint(Level level, String key) {
        Vec3 center = Vec3.atCenterOf(this.getBlockPos());
        for (Player player : level.players()) {
            if (player.distanceToSqr(center) <= 256.0D) {
                player.displayClientMessage(Component.translatable(key), true);
            }
        }
    }
}
