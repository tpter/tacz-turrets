package com.taczturrets.compat;

import com.taczturrets.TaczTurrets;
import com.taczturrets.blockentity.AbstractTaczTurretBlockEntity;
import com.taczturrets.entity.TurretShooterEntity;
import com.taczturrets.gun.GunHost;
import com.taczturrets.mount.MountedGun;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Friendly-fire guard: a turret's TACZ bullets must never destroy the ship (Sable sub-level) the turret itself is
 * standing on.
 * <p>
 * TACZ explosive bullets explode through {@link ExplodeUtil#createExplosion}; the bullet's owner is our invisible
 * {@link TurretShooterEntity}, whose {@link GunHost} tells us where the turret lives. We compare that turret's
 * sub-level with the sub-level of the impact point (the same {@code ServerShipUtils.getSubLevelAtBlockPos} comparison
 * AeroIE itself uses) and cancel the hit/explosion when they are the same ship.
 */
@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SelfShipProtection {
    private SelfShipProtection() {
    }

    /**
     * Primary guard: a bullet that hits a block in the turret's own sub-level. Cancelling this prevents the block
     * hit effect, the bullet-hole decal and the contact explosion (impact-fuse ammo), so the bullet passes through
     * the friendly ship harmlessly.
     */
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        BlockPos turretPos = turretPos(ownerShooterHost(event.getAmmo()));
        if (turretPos == null) {
            return;
        }
        SubLevel own = ServerShipUtils.getSubLevelAtBlockPos(event.getLevel(), turretPos);
        if (own == null) {
            return;
        }
        SubLevel hit = ServerShipUtils.getSubLevelAtBlockPos(event.getLevel(), event.getHitResult().getBlockPos());
        if (own == hit) {
            event.setCanceled(true);
        }
    }

    /**
     * Secondary guard: an explosion that is triggered without a block hit (proximity/air-burst fuse, or an entity
     * hit) while the bullet is still inside the turret's own sub-level. Cancelling the whole explosion stops the
     * {@link com.tacz.guns.util.block.ProjectileExplosion} from tracing and destroying the friendly ship's blocks.
     */
    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        Explosion explosion = event.getExplosion();
        Entity direct = explosion.getDirectSourceEntity();
        if (!(direct instanceof EntityKineticBullet bullet)) {
            return;
        }
        BlockPos turretPos = turretPos(ownerShooterHost(bullet));
        if (turretPos == null) {
            return;
        }
        Level level = event.getLevel();
        SubLevel own = ServerShipUtils.getSubLevelAtBlockPos(level, turretPos);
        if (own == null) {
            return;
        }
        Vec3 center = explosion.center();
        SubLevel hit = ServerShipUtils.getSubLevelAtBlockPos(level, BlockPos.containing(center));
        if (own == hit) {
            event.setCanceled(true);
        }
    }

    /** Resolves the shooter entity (bullet owner) to its host, if it is one of our turrets. */
    @Nullable
    private static GunHost ownerShooterHost(EntityKineticBullet bullet) {
        Entity owner = bullet.getOwner();
        if (!(owner instanceof TurretShooterEntity shooter)) {
            return null;
        }
        return shooter.getHost();
    }

    @Nullable
    private static BlockPos turretPos(@Nullable GunHost host) {
        if (host instanceof AbstractTaczTurretBlockEntity turret) {
            return turret.getBlockPos();
        }
        if (host instanceof MountedGun mounted) {
            return mounted.ownerPos;
        }
        return null;
    }
}
