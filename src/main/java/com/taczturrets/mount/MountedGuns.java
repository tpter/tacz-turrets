package com.taczturrets.mount;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Per-level store of the TACZ guns mounted on Create: Big Cannons cannon mounts, keyed by the mount block position.
 * <p>
 * CBC's {@code CannonMountBlockEntity} is not ours, so the gun and its ammo cannot live on the block entity itself;
 * they live in this saved data instead and are looked up by position. It is also the backing store for the extra
 * item handler capability we register for CBC's cannon mount block entity type, so Create mechanical arms can insert
 * TACZ ammo into the mount.
 */
public class MountedGuns extends SavedData {
    private static final String NAME = "taczturrets_mounted_guns";
    private final Map<BlockPos, MountedGun> guns = new HashMap<>();

    public static MountedGuns get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(MountedGuns::new, MountedGuns::load), NAME);
    }

    @Nullable
    public static MountedGun find(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            return get(serverLevel).guns.get(pos);
        }
        return null;
    }

    public MountedGun getOrCreate(BlockPos pos, float initialYaw) {
        MountedGun gun = this.guns.get(pos);
        if (gun == null) {
            gun = new MountedGun();
            gun.setYaw(initialYaw);
            gun.setPitch(0.0F);
            gun.ownerPos = pos;
            this.guns.put(pos, gun);
            this.setDirty();
        }
        return gun;
    }

    @Nullable
    public MountedGun get(BlockPos pos) {
        return this.guns.get(pos);
    }

    public void remove(BlockPos pos) {
        MountedGun gun = this.guns.remove(pos);
        if (gun != null) {
            gun.invalidateShooter();
            this.setDirty();
        }
    }

    /** Removes the entry and returns the items that have to be dropped into the world. */
    public java.util.List<ItemStack> removeAndCollectDrops(BlockPos pos) {
        MountedGun gun = this.guns.remove(pos);
        if (gun == null) {
            return java.util.List.of();
        }
        gun.invalidateShooter();
        this.setDirty();
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        if (!gun.getGunStack().isEmpty()) {
            drops.add(gun.getGunStack());
        }
        for (int slot = 0; slot < gun.getAmmo().getSlots(); slot++) {
            ItemStack stack = gun.getAmmo().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
        return drops;
    }

    public Map<BlockPos, MountedGun> view() {
        return Collections.unmodifiableMap(this.guns);
    }

    public void markChanged() {
        this.setDirty();
    }

    private static MountedGuns load(CompoundTag tag, HolderLookup.Provider registries) {
        MountedGuns data = new MountedGuns();
        CompoundTag guns = tag.getCompound("Guns");
        for (String key : guns.getAllKeys()) {
            try {
                BlockPos pos = BlockPos.of(Long.parseLong(key));
                MountedGun gun = MountedGun.load(guns.getCompound(key), registries);
                gun.ownerPos = pos;
                data.guns.put(pos, gun);
            } catch (NumberFormatException ignored) {
                // Skip malformed entries instead of failing the whole save.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag guns = new CompoundTag();
        // Entries without a gun are leftovers from a rejected interaction; never persist them.
        this.guns.forEach((pos, gun) -> {
            if (!gun.getGunStack().isEmpty()) {
                guns.put(Long.toString(pos.asLong()), gun.save(new CompoundTag(), registries));
            }
        });
        tag.put("Guns", guns);
        return tag;
    }
}
