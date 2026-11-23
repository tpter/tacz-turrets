package com.taczturrets.gun;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Anything that can carry a TACZ gun and supply it with ammo.
 * <p>
 * Implemented by our turret block entities and by a gun mounted directly on a Create: Big Cannons cannon mount.
 * {@link com.taczturrets.entity.TurretShooterEntity} binds to one of these: TACZ asks the shooter for its main hand
 * stack (the gun) and for its item handler capability (the ammo), and both are forwarded to the host.
 */
public interface GunHost {
    ItemStack getGunStack();

    void setGunStack(ItemStack stack);

    IItemHandler getAmmoHandler();
}
