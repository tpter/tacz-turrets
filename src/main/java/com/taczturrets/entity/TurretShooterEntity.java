package com.taczturrets.entity;

import com.taczturrets.gun.GunHost;
import java.util.Collections;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible LivingEntity used as the TACZ "shooter" of a turret.
 * <p>
 * TACZ's shooting pipeline ({@code LivingEntityShoot}, {@code IGunOperator}, reload/bolt state machines) is injected into
 * {@link LivingEntity} by a TACZ mixin, so any LivingEntity can shoot. The owning turret (or CBC cannon mount) positions this
 * entity at the muzzle and delegates its item handler capability to the host's own slots so TACZ can pull matching ammo.
 * It is added to the level (invisible, noSave, noSummon) so its entity id is synced to the client — that id is what TACZ's
 * gun-sound packet uses to resolve the sound source, so without it the turret fires silently.
 */
public class TurretShooterEntity extends LivingEntity implements IItemHandler {
    @Nullable
    private GunHost host;

    public TurretShooterEntity(EntityType<? extends TurretShooterEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setSilent(true);
        this.setInvulnerable(true);
    }

    public TurretShooterEntity bindHost(GunHost host) {
        this.host = host;
        return this;
    }

    @Nullable
    public GunHost getHost() {
        return this.host;
    }

    /** TACZ requires the shooter's main hand stack to be the exact gun stack it is firing. */
    @Override
    public ItemStack getMainHandItem() {
        return this.host == null ? ItemStack.EMPTY : this.host.getGunStack();
    }

    @Override
    public ItemStack getOffhandItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getItemInHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? this.getMainHandItem() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.getMainHandItem() : ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND && this.host != null) {
            this.host.setGunStack(stack);
        }
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Must chain: LivingEntity registers its own entries (health etc.) here, and Entity.<init>
        // validates that every registered id exists when it builds the SynchedEntityData.
        super.defineSynchedData(builder);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }

    // ------------------------------------------------------------------ IItemHandler (TACZ ammo source)

    private IItemHandler handler() {
        return this.host == null ? null : this.host.getAmmoHandler();
    }

    @Override
    public int getSlots() {
        IItemHandler handler = this.handler();
        return handler == null ? 0 : handler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        IItemHandler handler = this.handler();
        return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        IItemHandler handler = this.handler();
        return handler == null ? stack : handler.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        IItemHandler handler = this.handler();
        return handler == null ? ItemStack.EMPTY : handler.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        IItemHandler handler = this.handler();
        return handler == null ? 64 : handler.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        IItemHandler handler = this.handler();
        return handler != null && handler.isItemValid(slot, stack);
    }
}
