package com.taczturrets.blockentity;

import com.taczturrets.TaczTurrets;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Turret base #2: linkable to an AeroIE control seat.
 * <p>
 * When a player sits in the linked seat the turret follows the seat's manual aim point (the player's
 * mouse crosshair) and fires while the seat's fire input is held. It can be refilled by an AeroIE ammo
 * box (or any item handler) because its slots only accept ammo matching the installed TACZ gun.
 */
public class SeatTurretBlockEntity extends AbstractTaczTurretBlockEntity {
    public SeatTurretBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this(TaczTurrets.SEAT_TURRET_BE.get(), pos, state);
    }

    private Vec3 lastAim = Vec3.ZERO;
    private int linkTicker;

    public SeatTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------ gun model angles (edit here)
    // Rotation of the rendered gun model around the turret origin, in degrees: yaw first (Y), then pitch (X).
    // Changing only these two numbers turns / tilts the rotating turret's gun model.
    @Override
    public float getGunModelYawDegrees() {
        return 90.0F;
    }

    @Override
    public float getGunModelPitchDegrees() {
        return 180.0F;
    }

    @Override
    protected void turretTick(Level level) {
        ControlSeatServerData seat = this.resolveLinkedSeat(level);
        if (seat == null || seat.player == null) {
            this.aimAtDefaultAngles();
            this.lastPowered = false;
            return;
        }
        // Weapon channel: when the player selects a channel this turret is not on, it returns to its
        // settable idle angle and stops following the mouse (AeroIE-style channel behaviour).
        if (!this.channelMatches(seat)) {
            this.aimAtDefaultAngles();
            this.lastPowered = false;
            return;
        }

        double x = seat.manualAimTargetX;
        double y = seat.manualAimTargetY;
        double z = seat.manualAimTargetZ;
        if (x == 0.0D && y == 0.0D && z == 0.0D) {
            this.aimAtDefaultAngles();
            return;
        }

        this.lastAim = new Vec3(x, y, z);
        this.aimAt(this.lastAim);

        boolean firing = seat.isfiring;
        this.lastPowered = firing;
        if (firing && this.isAimed()) {
            this.shootWithTacz();
        }
    }

}
