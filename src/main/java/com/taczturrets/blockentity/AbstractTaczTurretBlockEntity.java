package com.taczturrets.blockentity;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.Initialize;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.taczturrets.TaczTurrets;
import com.taczturrets.entity.TurretShooterEntity;
import com.taczturrets.gun.GunHost;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Common base for the three TACZ turret bases.
 * <p>
 * Extends AeroIE's turret block entity so that the turret gets, for free:
 * control-seat linking through the AeroIE linker (peripheral type 3), Sable physics transforms
 * (aim origin and barrel direction are transformed by the sub-level pose), the 9-slot ammo inventory
 * (so an AeroIE ammo box can refill it), the fire cooldown bookkeeping and the HUD values.
 * <p>
 * The AeroIE auto-aim tick is intentionally <b>not</b> used: every subclass implements its own
 * {@link #turretTick(Level)} and the TACZ gun is fired through an invisible {@link TurretShooterEntity}.
 */
public abstract class AbstractTaczTurretBlockEntity extends AbstractTurretBlockEntity implements GunHost {
    public static final int AMMO_SLOTS = 9;
    private static final String TAG_GUN = "TaczGun";
    private static final String TAG_GUN_SCALE = "TaczGunScale";
    private static final String TAG_PITCH = "TaczPitch";
    private static final String TAG_YAW = "TaczYaw";
    private static final String TAG_POWERED = "TaczPowered";
    private static final String TAG_OFFSET_X = "TaczOffsetX";
    private static final String TAG_OFFSET_Y = "TaczOffsetY";
    private static final String TAG_OFFSET_Z = "TaczOffsetZ";
    private static final String TAG_DEFAULT_YAW = "TaczDefaultYaw";
    private static final String TAG_DEFAULT_PITCH = "TaczDefaultPitch";
    private static final String TAG_CHANNEL = "TaczChannel";

    private ItemStack gunStack = ItemStack.EMPTY;
    private float gunScale = 1.0F;
    // Render offset (in the gun's local frame, applied after the model rotation) to re-centre gun models
    // whose origin is not at their visual centre - e.g. very large gun packs such as "次元彼方".
    private float gunOffsetX = 0.0F;
    private float gunOffsetY = 0.0F;
    private float gunOffsetZ = 0.0F;
    // Idle aim direction (degrees) the rotating turret returns to when its weapon channel is not selected.
    private float defaultYawDeg = 0.0F;
    private float defaultPitchDeg = 0.0F;
    // Weapon channel bitmask (bits 1/2/4/8 = channels 1..4), like AeroIE. 15 = all channels, the default.
    private int channel = 15;
    private boolean gunDrawn;
    protected boolean lastPowered;
    private float lastSyncedPitch = Float.NaN;
    private float lastSyncedYaw = Float.NaN;
    private int syncTicker;
    // Sable sub-level lookup is not free (region/chunk scan), so it is refreshed once a second and cached;
    // the sub-level's logicalPose() is still queried fresh on every aim so rotation follows the ship.
    private int shipLookupCooldown;
    @Nullable
    private SubLevel cachedSubLevel;
    @Nullable
    private TurretShooterEntity shooter;

    protected AbstractTaczTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------ AeroIE abstract contract

    @Override
    public String getturrettype() {
        return "tacz_turret";
    }

    @Override
    public boolean isEnergyTurret() {
        // Ammo comes from the 9 item slots, not from AeroIE energy.
        return false;
    }

    @Override
    public boolean hasAmmoInventorySlots() {
        return true;
    }

    @Override
    public double getYAxisOffset() {
        return 0.55D;
    }

    @Override
    public double getcannonlength() {
        return 0.8D * this.gunScale;
    }

    @Override
    public float getMaxSpinSpeed() {
        return 0.35F;
    }

    @Override
    public int getCoolDown() {
        // 1 tick base cooldown; the real rate limit is TACZ's own shoot cooldown.
        return 1;
    }

    @Override
    public void shootentity() {
    }

    @Override
    public void shootship() {
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
            net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        // Our own menu adds the gun slot that AeroIE's TurretContainerMenu does not have.
        return new com.taczturrets.menu.TurretMenu(containerId, playerInventory, this);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        // No GeckoLib animations; the turret pose is driven by its own aim angles.
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void tick() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!this.hasInitialized) {
            Initialize.initialize(level, this.getBlockPos(), this.getBlockState(), pivotPoint);
            this.hasInitialized = true;
            return;
        }

        if (--this.shipLookupCooldown <= 0) {
            this.cachedSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
            this.onShip = this.cachedSubLevel != null;
            this.shipLookupCooldown = 20;
        }
        if (this.muzzleFlashTicks > 0) {
            this.muzzleFlashTicks--;
        }
        this.currentworldpos = this.getTurretAimOriginWorld();

        this.turretTick(level);

        // The shooter is registered in the level (see getShooter), so the server ticks it during its own
        // entity tick pass; ticking it here too would run its TACZ state machines (draw/shoot cooldown,
        // reload, bolt) twice per game tick.
        // Sync is throttled: at most once every 3 ticks while the barrel is moving (a 0.02 rad threshold
        // filters sub-visual noise), with a 40-tick heartbeat. Serialising the whole block entity every
        // tick (gun + 9 ammo slots + tuning) was a real cost with many turrets.
        boolean anglesChanged = Math.abs(this.lastSyncedPitch - this.xRot0) > 0.02F
                || Math.abs(this.lastSyncedYaw - this.yRot0) > 0.02F;
        this.syncTicker++;
        if ((anglesChanged && this.syncTicker >= 3) || this.syncTicker >= 40) {
            this.lastSyncedPitch = this.xRot0;
            this.lastSyncedYaw = this.yRot0;
            this.syncTicker = 0;
            // Send the block entity payload (gun, scale, angles) to the client. AeroIE's markUpdated()
            // only flags the block state, which never carried our gun data - that made the installed
            // gun invisible on the client.
            this.notifyUpdate();
        }
    }

    /** Called every server tick after the engine fields are refreshed. */
    protected abstract void turretTick(Level level);

    // ------------------------------------------------------------------ aiming helpers

    /** Rotates the turret towards a world-space target; the step is limited by {@link #getMaxSpinSpeed()}. */
    protected final void aimAt(@Nullable Vec3 target) {
        if (target == null) {
            return;
        }
        double[] angles = this.computeTargetAimAngles(target);
        if (angles == null) {
            return;
        }
        angles = this.clampAimAnglesToConfiguredWindow(angles);
        if (angles == null) {
            return;
        }
        this.prevxrot = this.xRot0;
        this.prevyrot = this.yRot0;
        this.targetxrot = (float) angles[0];
        this.targetyrot = (float) angles[1];
        this.xRot0 = this.closestReachableX(this.xRot0, this.getMaxSpinSpeed(), this.targetxrot);
        this.yRot0 = this.closestReachableY(this.yRot0, this.getMaxSpinSpeed(), this.targetyrot);
    }

    protected final boolean isAimed() {
        return this.xOK && this.yOK;
    }

    /**
     * Resolves the AeroIE control seat this turret is linked to (through the AeroIE linker, peripheral type 3).
     * Returns {@code null} when unlinked, or when the seat is not loaded at the linked position.
     */
    @Nullable
    protected ControlSeatServerData resolveLinkedSeat(Level level) {
        BlockPos seatPos = this.getLinkedControlSeatPos();
        if (seatPos == null) {
            return null;
        }
        if (level.getBlockEntity(seatPos) instanceof AbstractControlSeatBlockEntity seatBlockEntity) {
            return seatBlockEntity.getControlSeatData();
        }
        return null;
    }

    /**
     * Barrel direction in block/sub-level space (no Sable world transform), used by the renderer.
     */
    public Vec3 getLocalBarrelDirection() {
        return this.getLocalBarrelDirection(this.xRot0, this.yRot0);
    }

    public Vec3 getLocalBarrelDirection(float pitch, float yaw) {
        double horizontal = Math.cos(pitch);
        Vec3 direction = new Vec3(Math.sin(-yaw) * horizontal, Math.sin(pitch), Math.cos(-yaw) * horizontal);
        return direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    // ------------------------------------------------------------------ aim frame

    /**
     * The turrets stand upright on top of their base, so the aim pivot is simply one block up
     * (0.55 blocks above the block centre) instead of AeroIE's hull-normal direction.
     */
    @Override
    public Vec3 getTurretLocalUpDirection() {
        return new Vec3(0.0D, 1.0D, 0.0D);
    }

    /**
     * Same as AeroIE's aim origin, but using the cached sub-level instead of a fresh
     * {@link ServerShipUtils#getSubLevelAtBlockPos} scan every call (this runs once per tick per turret).
     */
    @Override
    protected Vec3 getTurretAimOriginWorld() {
        Vec3 localOrigin = Vec3.atCenterOf(this.getBlockPos())
                .add(this.getTurretLocalUpDirection().scale(this.getYAxisOffset()));
        SubLevel ship = this.cachedSubLevel;
        return ship == null ? localOrigin : ship.logicalPose().transformPosition(localOrigin);
    }

    /**
     * AeroIE derives its aim frame from the block's FACING, which does not match how the gun is
     * rendered (or how vanilla aims). That mismatch made the seat turret fire at odd angles and the
     * fixed turret fire into its own hull. We use the plain Minecraft basis instead:
     * +Z = south (yaw 0), +Y = up (positive aim pitch), +X = east (so yaw 90 looks west).
     * On a Sable sub-level the axes are rotated by the ship pose, so all three bases follow the ship.
     */
    @Override
    public void updateWorldControlAxes() {
        Vec3 forward = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = new Vec3(1.0D, 0.0D, 0.0D);
        SubLevel ship = this.cachedSubLevel;
        if (ship != null) {
            this.worldXDirection = ship.logicalPose()
                    .transformNormal(new org.joml.Vector3d(forward.x, forward.y, forward.z)).normalize();
            this.worldYDirection = ship.logicalPose()
                    .transformNormal(new org.joml.Vector3d(up.x, up.y, up.z)).normalize();
            this.worldZDirection = ship.logicalPose()
                    .transformNormal(new org.joml.Vector3d(right.x, right.y, right.z)).normalize();
        } else {
            this.worldXDirection = new org.joml.Vector3d(forward.x, forward.y, forward.z);
            this.worldYDirection = new org.joml.Vector3d(up.x, up.y, up.z);
            this.worldZDirection = new org.joml.Vector3d(right.x, right.y, right.z);
        }
    }

    // ------------------------------------------------------------------ TACZ firing

    /** Fires one shot through TACZ if a gun is installed and its own cooldown allows it. */
    protected void shootWithTacz() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        ItemStack gun = this.gunStack;
        if (gun.isEmpty() || !(gun.getItem() instanceof IGun)) {
            return;
        }
        Vec3 direction = this.getCurrentBarrelDirectionWorld();
        if (direction == null) {
            return;
        }
        TurretShooterEntity shooterEntity = this.getShooter(level);
        // Keep the muzzle outside the turret block itself; the old 0.8-block offset spawned bullets
        // inside the hull, which is why most seat-turret shots hit blocks immediately. 1.5 blocks puts
        // the spawn point clear of thin hull plates too, so a turret bolted to a one-block wall still
        // fires out the other side instead of dying inside it.
        double muzzleDistance = Math.max(this.getcannonlength(), 1.5D);
        Vec3 muzzle = this.getTurretAimOriginWorld().add(direction.scale(muzzleDistance));
        shooterEntity.setPos(muzzle.x, muzzle.y - shooterEntity.getEyeHeight(), muzzle.z);
        shooterEntity.setOldPosAndRot();

        IGunOperator operator = IGunOperator.fromLivingEntity(shooterEntity);
        ShooterDataHolder data = operator.getDataHolder();
        if (!this.gunDrawn || data.currentGunItem == null || data.currentGunItem.get().isEmpty()) {
            operator.draw(() -> this.gunStack);
            this.gunDrawn = true;
            return;
        }

        // Belt-and-suspenders ammo gate: refuse to fire (and try a reload) when the gun has no round in
        // its magazine or chamber AND no matching ammo in the turret slots. TACZ's own gate already checks
        // this, but this guarantees the turret can never fire with empty ammo regardless of gun NBT state.
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun.getCurrentAmmoCount(gun) <= 0 && !iGun.hasBulletInBarrel(gun)
                && !iGun.hasInventoryAmmo(shooterEntity, gun, true)) {
            operator.reload();
            return;
        }

        float mcPitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D)));
        float mcYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        ShootResult result = operator.shoot(() -> mcPitch, () -> mcYaw);
        if (level.getGameTime() % 20L == 0L) {
            // Rate-limited diagnostic: tells us whether TACZ accepted the shot and along which axis.
            TaczTurrets.LOGGER.info("[taczturrets] {} shot result={} dir=({}, {}, {}) muzzle=({}, {}, {})",
                    this.getBlockState().getBlock(), result,
                    String.format(java.util.Locale.ROOT, "%.3f", direction.x),
                    String.format(java.util.Locale.ROOT, "%.3f", direction.y),
                    String.format(java.util.Locale.ROOT, "%.3f", direction.z),
                    String.format(java.util.Locale.ROOT, "%.3f", muzzle.x),
                    String.format(java.util.Locale.ROOT, "%.3f", muzzle.y),
                    String.format(java.util.Locale.ROOT, "%.3f", muzzle.z));
        }
        switch (result) {
            case SUCCESS, COOL_DOWN, IS_DRAWING, IS_RELOADING, IS_BOLTING, OVERHEATED, UNKNOWN_FAIL -> {
            }
            case NO_AMMO -> operator.reload();
            case NEED_BOLT -> operator.bolt();
            case NOT_DRAW, NOT_GUN, ID_NOT_EXIST -> {
                this.gunDrawn = false;
                operator.draw(() -> this.gunStack);
                this.gunDrawn = true;
            }
            default -> {
            }
        }
    }

    protected TurretShooterEntity getShooter(Level level) {
        if (this.shooter == null || this.shooter.isRemoved() || this.shooter.level() != level || this.shooter.getHost() != this) {
            Vec3 origin = this.getTurretAimOriginWorld();
            this.shooter = new TurretShooterEntity(TaczTurrets.TURRET_SHOOTER.get(), level).bindHost(this);
            this.shooter.setPos(origin.x, origin.y - 0.5D, origin.z);
            this.shooter.setOldPosAndRot();
            // Register the shooter in the level so it gets a real entity id that is synced to the client.
            // TACZ's SoundManager.sendSoundToNearby(...) sends that id to nearby players, and the client's
            // SoundPlayManager.playMessageSound(...) only plays the gun sound when level.getEntity(id)
            // resolves to a LivingEntity. A world-less shooter was never findable, so the turret fired
            // silently. The entity is invisible (empty renderer + shouldRenderAtSqrDistance=false) and
            // noSave/noSummon, so it leaves no trace.
            level.addFreshEntity(this.shooter);
            this.gunDrawn = false;
        }
        return this.shooter;
    }

    public void invalidateShooter() {
        if (this.shooter != null) {
            this.shooter.discard();
            this.shooter = null;
        }
    }

    // ------------------------------------------------------------------ gun / ammo handling

    @Override
    public net.neoforged.neoforge.items.IItemHandler getAmmoHandler() {
        return this.getItemHandler();
    }

    /**
     * Diagnostic wrapper around AeroIE's ammo inventory: mechanical arms insert through the
     * {@code Capabilities.ItemHandler.BLOCK} capability, so a log line here proves whether an arm reached
     * this block entity at all. Silent for simulated (probe) inserts.
     */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack remainder = super.insertItem(slot, stack, simulate);
        if (!simulate && !stack.isEmpty() && remainder.getCount() != stack.getCount()) {
            TaczTurrets.LOGGER.info("[taczturrets] ammo inserted into {} at {}: {} x{} (slot {})",
                    this.getBlockState().getBlock(), this.getBlockPos(), stack.getHoverName().getString(),
                    stack.getCount() - remainder.getCount(), slot);
        }
        return remainder;
    }

    /**
     * Model correction around the turret-local Y axis, applied inside the aim rotation, in degrees.
     * The gun models are authored pointing the other way than the barrel vector, so the turrets
     * override this with 180.
     */
    public float getGunModelYawDegrees() {
        return 0.0F;
    }

    /**
     * Model correction around the turret-local X axis, applied after {@link #getGunModelYawDegrees()},
     * in degrees. The rotating (seat) turret needs 90 degrees here, the fixed turret 0.
     */
    public float getGunModelPitchDegrees() {
        return 0.0F;
    }

    public ItemStack getGunStack() {
        return this.gunStack;
    }

    public void setGunStack(ItemStack stack) {
        // Strip the gun's own magazine/chambered round here (the single choke point every install path
        // flows through). Without this, a gun that already carries internal ammo would let the turret fire
        // "for free" with empty ammo slots - TACZ legitimately spends the gun's magazine first. Zeroing it
        // makes the 9 ammo slots the sole ammo source.
        this.gunStack = com.taczturrets.gun.GunInstall.stripAmmo(stack);
        this.gunDrawn = false;
        this.setChanged();
        this.notifyUpdate();
    }

    public float getGunScale() {
        return this.gunScale;
    }

    public void setGunScale(float scale) {
        this.gunScale = Mth.clamp(scale, 0.25F, 3.0F);
        this.setChanged();
        this.notifyUpdate();
    }

    public float cycleGunScale() {
        float next = this.gunScale + 0.25F;
        if (next > 1.75F + 1.0E-4F) {
            next = 0.5F;
        }
        this.setGunScale(next);
        return this.gunScale;
    }

    @Override
    protected boolean acceptsAmmoStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.gunStack.isEmpty() || !(this.gunStack.getItem() instanceof IGun)) {
            return stack.getItem() instanceof IAmmo || stack.getItem() instanceof IAmmoBox;
        }
        IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
        if (ammo != null) {
            return ammo.isAmmoOfGun(this.gunStack, stack);
        }
        if (stack.getItem() instanceof IAmmoBox box) {
            return box.isAmmoBoxOfGun(this.gunStack, stack);
        }
        return false;
    }

    /** Inserts the given stack into the internal ammo inventory; returns the remainder. */
    public ItemStack storeAmmo(ItemStack stack) {
        ItemStack rest = stack;
        for (int slot = 0; slot < this.getSlots() && !rest.isEmpty(); slot++) {
            rest = this.insertItem(slot, rest, false);
        }
        return rest;
    }

    public double getAimPitchRadians() {
        return this.xRot0;
    }

    public double getAimYawRadians() {
        return this.yRot0;
    }

    public boolean isPowered() {
        return this.lastPowered;
    }

    public void setPowered(boolean powered) {
        this.lastPowered = powered;
    }

    // ------------------------------------------------------------------ tuning fields

    public float getGunOffsetX() {
        return this.gunOffsetX;
    }

    public float getGunOffsetY() {
        return this.gunOffsetY;
    }

    public float getGunOffsetZ() {
        return this.gunOffsetZ;
    }

    public float getDefaultYawDegrees() {
        return this.defaultYawDeg;
    }

    public float getDefaultPitchDegrees() {
        return this.defaultPitchDeg;
    }

    public int getChannel() {
        return this.channel;
    }

    public boolean isChannelOn(int bit) {
        return (this.channel & bit) != 0;
    }

    /**
     * Applies the full tuning set coming from the GUI ({@code /tczmodel} or the turret screen) in one
     * write so a slider drag sends one packet instead of six.
     */
    public void applyConfig(float scale, float offsetX, float offsetY, float offsetZ, float defaultYaw,
            float defaultPitch, int channel) {
        this.gunScale = Mth.clamp(scale, 0.25F, 3.0F);
        this.gunOffsetX = offsetX;
        this.gunOffsetY = offsetY;
        this.gunOffsetZ = offsetZ;
        this.defaultYawDeg = defaultYaw;
        this.defaultPitchDeg = defaultPitch;
        this.channel = channel & 15;
        this.setChanged();
        this.notifyUpdate();
    }

    /** Rotates the turret towards its settable idle angle, limited by {@link #getMaxSpinSpeed()}. */
    protected void aimAtDefaultAngles() {
        this.prevxrot = this.xRot0;
        this.prevyrot = this.yRot0;
        this.targetxrot = (float) Math.toRadians(this.defaultPitchDeg);
        this.targetyrot = (float) Math.toRadians(this.defaultYawDeg);
        this.xRot0 = this.closestReachableX(this.xRot0, this.getMaxSpinSpeed(), this.targetxrot);
        this.yRot0 = this.closestReachableY(this.yRot0, this.getMaxSpinSpeed(), this.targetyrot);
    }

    /**
     * True when this turret's weapon channel intersects the linked seat's currently selected channel(s).
     * A null seat counts as matching (an unlinked turret behaves as before, e.g. the fixed turret's
     * channel gate only applies once a seat is actually linked).
     */
    protected boolean channelMatches(@Nullable ControlSeatServerData seat) {
        return seat == null || (this.channel & seat.channelencode) != 0;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!this.gunStack.isEmpty()) {
            tag.put(TAG_GUN, this.gunStack.save(registries));
        }
        tag.putFloat(TAG_GUN_SCALE, this.gunScale);
        tag.putFloat(TAG_PITCH, this.xRot0);
        tag.putFloat(TAG_YAW, this.yRot0);
        tag.putBoolean(TAG_POWERED, this.lastPowered);
        tag.putFloat(TAG_OFFSET_X, this.gunOffsetX);
        tag.putFloat(TAG_OFFSET_Y, this.gunOffsetY);
        tag.putFloat(TAG_OFFSET_Z, this.gunOffsetZ);
        tag.putFloat(TAG_DEFAULT_YAW, this.defaultYawDeg);
        tag.putFloat(TAG_DEFAULT_PITCH, this.defaultPitchDeg);
        tag.putInt(TAG_CHANNEL, this.channel);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.gunStack = tag.contains(TAG_GUN)
                ? com.taczturrets.gun.GunInstall.stripAmmo(ItemStack.parseOptional(registries, tag.getCompound(TAG_GUN)))
                : ItemStack.EMPTY;
        this.gunScale = tag.contains(TAG_GUN_SCALE) ? tag.getFloat(TAG_GUN_SCALE) : 1.0F;
        this.lastPowered = tag.getBoolean(TAG_POWERED);
        this.gunOffsetX = tag.getFloat(TAG_OFFSET_X);
        this.gunOffsetY = tag.getFloat(TAG_OFFSET_Y);
        this.gunOffsetZ = tag.getFloat(TAG_OFFSET_Z);
        this.defaultYawDeg = tag.getFloat(TAG_DEFAULT_YAW);
        this.defaultPitchDeg = tag.getFloat(TAG_DEFAULT_PITCH);
        this.channel = tag.contains(TAG_CHANNEL) ? tag.getInt(TAG_CHANNEL) : 15;
        if (clientPacket) {
            this.prevxrot = this.xRot0;
            this.prevyrot = this.yRot0;
            this.xRot0 = tag.getFloat(TAG_PITCH);
            this.yRot0 = tag.getFloat(TAG_YAW);
        } else {
            this.xRot0 = tag.getFloat(TAG_PITCH);
            this.yRot0 = tag.getFloat(TAG_YAW);
            this.prevxrot = this.xRot0;
            this.prevyrot = this.yRot0;
        }
    }

    @Override
    public void remove() {
        this.invalidateShooter();
        super.remove();
    }
}
