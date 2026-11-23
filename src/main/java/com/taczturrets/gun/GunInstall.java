package com.taczturrets.gun;

import com.tacz.guns.api.item.IGun;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for turning a TACZ gun item into a turret-mounted gun.
 */
public final class GunInstall {
    private GunInstall() {
    }

    /**
     * Returns a gun stack whose internal magazine ammo and chambered round are stripped to zero.
     * <p>
     * A gun item given to a player usually carries its own loaded magazine, so a turret would fire "for free"
     * even with empty ammo slots (TACZ legitimately spends the gun's own magazine first). Zeroing both here makes
     * the host's ammo slots the sole ammo source: the first shot returns {@code NO_AMMO} and the turret's
     * {@code reload()} pulls matching ammo from the slots into the magazine.
     */
    public static ItemStack stripAmmo(ItemStack gun) {
        if (!gun.isEmpty()) {
            IGun iGun = IGun.getIGunOrNull(gun);
            if (iGun != null) {
                iGun.setCurrentAmmoCount(gun, 0);
                iGun.setBulletInBarrel(gun, false);
            }
        }
        return gun;
    }
}
