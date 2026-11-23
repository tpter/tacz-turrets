package com.taczturrets.blockentity;

import com.taczturrets.TaczTurrets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

/**
 * Turret base #3: driven by a Create Big Cannons cannon mount.
 * <p>
 * The CBC mount is not modified and its cannon contraption is not required. The turret simply reads the
 * mount's public yaw/pitch shaft speeds and integrates them with exactly the same angular formula CBC
 * uses for mounted cannons ({@code convertToAngular(speed) * 0.125}, yaw negated, pitch signed by the
 * shaft axis), so the mount's yaw/pitch shafts aim the turret. Firing follows the mount's redstone
 * {@code FIRE_POWERED} input (or a redstone signal directly on the turret).
 */
public class CbcTurretBlockEntity extends AbstractTaczTurretBlockEntity {
    public CbcTurretBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this(TaczTurrets.CBC_TURRET_BE.get(), pos, state);
    }

    private static final float MAX_ELEVATION = 60.0F;
    private static final float MAX_DEPRESSION = -30.0F;
    private static final double AIM_PROJECTION = 64.0D;

    /** CBC work angles, in degrees, kept exactly like {@code CannonMountBlockEntity#cannonYaw/#cannonPitch}. */
    private float cbcYaw;
    private float cbcPitch;
    private boolean cbcInitialized;

    @Nullable
    private CannonMountBlockEntity cachedMount;
    private int mountScanTicker;

    public CbcTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void turretTick(Level level) {        CannonMountBlockEntity mount = this.findMount(level);
        if (mount == null) {
            this.lastPowered = false;
            this.returnToDefaultAim();
            return;
        }

        BlockState mountState = level.getBlockState(mount.getBlockPos());
        if (!this.cbcInitialized) {
            if (mountState.hasProperty(CannonMountBlock.HORIZONTAL_FACING)) {
                this.cbcYaw = mountState.getValue(CannonMountBlock.HORIZONTAL_FACING).toYRot();
            }
            this.cbcPitch = 0.0F;
            this.cbcInitialized = true;
        }

        // ---- replicate CBC's own rotation math on the mount's public shaft speeds
        float yawStep = mount.getAngularSpeed(-mount.getYawSpeed(), 0.0F);
        float pitchStep = mount.getAngularSpeed(mount.getPitchSpeed(), 0.0F);
        Direction contraptionDirection = mount.getContraptionDirection();
        boolean flag = (contraptionDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE)
                == (contraptionDirection.getAxis() == Direction.Axis.X);
        int pitchSign = flag ? 1 : -1;

        this.cbcYaw += yawStep;
        this.cbcPitch = Mth.clamp(this.cbcPitch + pitchStep * pitchSign, MAX_DEPRESSION, MAX_ELEVATION);

        // ---- turn the TACZ turret towards a point far along the CBC aim direction
        Vec3 mountCenter = Vec3.atCenterOf(mount.getBlockPos());
        Vec3 direction = Vec3.directionFromRotation((float) -this.cbcPitch, (float) this.cbcYaw);
        this.aimAt(mountCenter.add(direction.scale(AIM_PROJECTION)));

        // ---- fire on the mount's redstone fire input, or a direct redstone signal on this block
        boolean mountFire = mountState.hasProperty(CannonMountBlock.FIRE_POWERED)
                && mountState.getValue(CannonMountBlock.FIRE_POWERED);
        boolean powered = mountFire || level.hasNeighborSignal(this.getBlockPos());
        this.lastPowered = powered;
        if (powered && this.isAimed()) {
            this.shootWithTacz();
        }
    }

    /**
     * This turret is deliberately outside AeroIE's control-seat ecosystem: aiming and firing come from
     * the CBC cannon mount only, so an AeroIE control seat must not be able to drive it. Refusing the
     * stored link keeps AeroIE's linker from wiring this block to a seat.
     */
    @Override
    public void setLinkedControlSeatPos(BlockPos seatPos) {
        TaczTurrets.LOGGER.info("[taczturrets] cbc turret at {} ignores AeroIE control seat link {}", this.getBlockPos(), seatPos);
    }

    @Override
    public BlockPos getLinkedControlSeatPos() {
        return BlockPos.ZERO;
    }

    /**
     * Deliberately reports no AeroIE ammo slots: an AeroIE ammo box only refills turrets that report
     * slots, and this base is meant to be supplied by Create mechanical arms (or the turret GUI).
     * The nine item slots themselves still work; only the AeroIE ammo-box routing is disabled.
     */
    @Override
    public boolean hasAmmoInventorySlots() {
        return false;
    }

    /** Ammo still drops when the block is broken, even though {@link #hasAmmoInventorySlots()} is false. */
    @Override
    public void dropStoredAmmo(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.getSlots(); slot++) {
            net.minecraft.world.item.ItemStack stack = this.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack.copy());
                this.setStackInSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    private void returnToDefaultAim() {
        this.prevxrot = this.xRot0;
        this.prevyrot = this.yRot0;
        this.targetxrot = 0.0F;
        this.targetyrot = 0.0F;
        this.xRot0 = this.closestReachableX(this.xRot0, this.getMaxSpinSpeed(), 0.0F);
        this.yRot0 = this.closestReachableY(this.yRot0, this.getMaxSpinSpeed(), 0.0F);
    }

    @Nullable
    private CannonMountBlockEntity findMount(Level level) {
        if (this.cachedMount != null && !this.cachedMount.isRemoved() && this.cachedMount.getLevel() == level) {
            return this.cachedMount;
        }
        this.cachedMount = null;
        if (this.mountScanTicker++ % 20 != 0) {
            return null;
        }
        BlockPos origin = this.getBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (level.getBlockEntity(candidate) instanceof CannonMountBlockEntity mount) {
                        this.cachedMount = mount;
                        return mount;
                    }
                }
            }
        }
        return null;
    }
}
