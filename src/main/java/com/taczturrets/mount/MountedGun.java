package com.taczturrets.mount;

import com.taczturrets.TaczTurrets;
import com.taczturrets.entity.TurretShooterEntity;
import com.taczturrets.gun.GunHost;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A TACZ gun that is mounted directly on a Create: Big Cannons cannon mount.
 * <p>
 * The mount itself is the breech: there is no extra turret block. The gun is installed by right clicking the
 * mount while holding a TACZ gun (before or after assembling the cannon mount), the nine ammo slots are filled
 * by right clicking with TACZ ammo or by Create mechanical arms (the mount exposes this inventory through our
 * extra item handler capability), the mount's yaw/pitch shafts rotate the gun and its redstone fire input fires it.
 */
public class MountedGun implements GunHost {
    public static final int AMMO_SLOTS = 9;
    private static final String TAG_GUN = "Gun";
    private static final String TAG_AMMO = "Ammo";
    private static final String TAG_YAW = "Yaw";
    private static final String TAG_PITCH = "Pitch";
    private static final String TAG_SCALE = "Scale";

    private final ItemStackHandler ammo = new ItemStackHandler(AMMO_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MountedGun.this.acceptsAmmo(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack remainder = super.insertItem(slot, stack, simulate);
            if (!simulate && remainder.getCount() != stack.getCount()) {
                // Diagnostic: proves a mechanical arm (or any other pipe) actually reached this handler.
                TaczTurrets.LOGGER.info("[taczturrets] ammo inserted into mount at {}: {} x{} (slot {}, remainder {})",
                        MountedGun.this.ownerPos, stack.getHoverName().getString(), stack.getCount() - remainder.getCount(),
                        slot, remainder.getCount());
            }
            return remainder;
        }

        @Override
        protected void onContentsChanged(int slot) {
            MountedGun.this.dirty = true;
        }
    };
    private ItemStack gunStack = ItemStack.EMPTY;
    private float yaw = 180.0F;
    private float pitch = 0.0F;
    private float scale = 1.0F;
    private boolean gunDrawn;
    private boolean dirty = true;
    @Nullable
    private transient TurretShooterEntity shooter;

    // ------------------------------------------------------------------ persistence

    public static MountedGun load(CompoundTag tag, HolderLookup.Provider registries) {
        MountedGun gun = new MountedGun();
        gun.gunStack = com.taczturrets.gun.GunInstall.stripAmmo(ItemStack.parseOptional(registries, tag.getCompound(TAG_GUN)));
        gun.ammo.deserializeNBT(registries, tag.getCompound(TAG_AMMO));
        gun.yaw = tag.getFloat(TAG_YAW);
        gun.pitch = tag.getFloat(TAG_PITCH);
        gun.scale = tag.contains(TAG_SCALE) ? tag.getFloat(TAG_SCALE) : 1.0F;
        return gun;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // An empty stack cannot be encoded (ItemStack#save throws "Cannot encode empty ItemStack"), and an
        // entry without a gun carries nothing useful anyway - a mount whose gun was never installed used
        // to crash the whole world save here.
        if (!this.gunStack.isEmpty()) {
            tag.put(TAG_GUN, this.gunStack.save(registries));
        }
        tag.put(TAG_AMMO, this.ammo.serializeNBT(registries));
        tag.putFloat(TAG_YAW, this.yaw);
        tag.putFloat(TAG_PITCH, this.pitch);
        tag.putFloat(TAG_SCALE, this.scale);
        return tag;
    }

    // ------------------------------------------------------------------ GunHost

    @Override
    public ItemStack getGunStack() {
        return this.gunStack;
    }

    @Override
    public void setGunStack(ItemStack stack) {
        // Strip the gun's own magazine/chambered round so the mount's ammo slots are the sole ammo source.
        this.gunStack = com.taczturrets.gun.GunInstall.stripAmmo(stack);
        this.gunDrawn = false;
        this.dirty = true;
    }

    @Override
    public IItemHandler getAmmoHandler() {
        return this.ammo;
    }

    public ItemStackHandler getAmmo() {
        return this.ammo;
    }

    public boolean acceptsAmmo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.gunStack.isEmpty() || !(this.gunStack.getItem() instanceof IGun)) {
            return stack.getItem() instanceof IAmmo || stack.getItem() instanceof IAmmoBox;
        }
        IAmmo ammoItem = IAmmo.getIAmmoOrNull(stack);
        if (ammoItem != null) {
            return ammoItem.isAmmoOfGun(this.gunStack, stack);
        }
        if (stack.getItem() instanceof IAmmoBox box) {
            return box.isAmmoBoxOfGun(this.gunStack, stack);
        }
        return false;
    }

    // ------------------------------------------------------------------ aiming / firing

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = Mth.wrapDegrees(yaw);
        this.dirty = true;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = Mth.clamp(pitch, -30.0F, 60.0F);
        this.dirty = true;
    }

    public float getScale() {
        return this.scale;
    }

    public float cycleScale() {
        float next = this.scale + 0.25F;
        if (next > 1.75F + 1.0E-4F) {
            next = 0.5F;
        }
        this.scale = Mth.clamp(next, 0.25F, 3.0F);
        this.dirty = true;
        return this.scale;
    }

    /** Unit aim direction in level space. {@code pitch} is positive upwards, {@code yaw} follows MC degrees. */
    public Vec3 direction() {
        return Vec3.directionFromRotation(-this.pitch, this.yaw);
    }

    /** Applies the cannon mount's own shaft math (same as CBC does for a mounted cannon). */
    public void applyMountRotation(float yawStep, float pitchStep, int pitchSign) {
        if (yawStep != 0.0F) {
            this.setYaw(this.yaw + yawStep);
        }
        if (pitchStep != 0.0F) {
            this.setPitch(this.pitch + pitchStep * pitchSign);
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markSynced() {
        this.dirty = false;
    }

    /**
     * Fires one shot through TACZ if the mounted gun has ammo and its own cooldown allows it.
     *
     * @param worldDir the WORLD aim direction (already rotated by the Sable sub-level pose when on a ship); the
     *                 local {@link #direction()} is only correct for a mount that is not physicalized.
     */
    public void shoot(Level level, Vec3 muzzle, Vec3 worldDir) {
        if (level.isClientSide() || this.gunStack.isEmpty() || !(this.gunStack.getItem() instanceof IGun)) {
            return;
        }
        Vec3 direction = worldDir;
        TurretShooterEntity shooterEntity = this.getShooter(level, muzzle);
        shooterEntity.setPos(muzzle.x, muzzle.y - shooterEntity.getEyeHeight(), muzzle.z);
        shooterEntity.setOldPosAndRot();

        IGunOperator operator = IGunOperator.fromLivingEntity(shooterEntity);
        ShooterDataHolder data = operator.getDataHolder();
        if (!this.gunDrawn || data.currentGunItem == null || data.currentGunItem.get().isEmpty()) {
            operator.draw(() -> this.gunStack);
            this.gunDrawn = true;
            this.dirty = true;
            return;
        }

        // Belt-and-suspenders ammo gate: refuse to fire (and try a reload) when the gun has no round in
        // its magazine or chamber AND no matching ammo in the mount's slots.
        IGun iGun = IGun.getIGunOrNull(this.gunStack);
        if (iGun.getCurrentAmmoCount(this.gunStack) <= 0 && !iGun.hasBulletInBarrel(this.gunStack)
                && !iGun.hasInventoryAmmo(shooterEntity, this.gunStack, true)) {
            operator.reload();
            this.dirty = true;
            return;
        }

        float mcPitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D)));
        float mcYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        ShootResult result = operator.shoot(() -> mcPitch, () -> mcYaw);
        this.dirty = true;
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

    private TurretShooterEntity getShooter(Level level, Vec3 muzzle) {
        if (this.shooter == null || this.shooter.isRemoved() || this.shooter.level() != level || this.shooter.getHost() != this) {
            this.shooter = new TurretShooterEntity(TaczTurrets.TURRET_SHOOTER.get(), level).bindHost(this);
            this.shooter.setPos(muzzle.x, muzzle.y, muzzle.z);
            this.shooter.setOldPosAndRot();
            // Register in the level so its entity id is synced to the client; that id is what TACZ's
            // gun-sound packet uses to resolve the sound source (see AbstractTaczTurretBlockEntity#getShooter).
            level.addFreshEntity(this.shooter);
            this.gunDrawn = false;
        }
        return this.shooter;
    }

    public void tickShooter() {
        // The shooter is a registered level entity now, so the server ticks it during its own entity
        // pass; ticking it here would run its TACZ state machines twice per game tick.
    }

    public void invalidateShooter() {
        if (this.shooter != null) {
            this.shooter.discard();
            this.shooter = null;
            this.gunDrawn = false;
        }
    }

    public BlockPos ownerPos = BlockPos.ZERO;
}
