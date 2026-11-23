package com.taczturrets.compat;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.taczturrets.TaczTurrets;
import com.taczturrets.mount.MountedGun;
import com.taczturrets.mount.MountedGuns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

/**
 * Makes a Create mechanical arm able to feed TACZ ammo into a CBC cannon mount.
 * <p>
 * Create's arm can only interact with positions whose {@link ArmInteractionPointType#canCreatePoint} returns true and
 * all of its built-in types are hard-coded to Create's own blocks, so a CBC cannon mount was never a valid arm target -
 * this is why no capability alone could make an arm fill it. Create's {@code arm_interaction_point_type} registry is a
 * normal (synced) registry, so registering one more type here adds the mount as an arm target without touching Create.
 */
@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ArmInteractionPoints {
    /** Registered type instance; the registry only stores the type, points are created per position. */
    private static final ArmInteractionPointType CANNON_MOUNT_TYPE = new CannonMountType();

    private ArmInteractionPoints() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(CreateRegistries.ARM_INTERACTION_POINT_TYPE)) {
            return;
        }
        event.register(CreateRegistries.ARM_INTERACTION_POINT_TYPE,
                ResourceLocation.fromNamespaceAndPath(TaczTurrets.MODID, "cannon_mount_gun"),
                () -> CANNON_MOUNT_TYPE);
    }

    private static final class CannonMountType extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return level.getBlockEntity(pos) instanceof CannonMountBlockEntity;
        }

        @Override
        @Nullable
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return level.getBlockEntity(pos) instanceof CannonMountBlockEntity ? new CannonMountPoint(this, level, pos, state) : null;
        }
    }

    private static final class CannonMountPoint extends ArmInteractionPoint {
        private CannonMountPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
            super(type, level, pos, state);
        }

        @Override
        protected Vec3 getInteractionPositionVector() {
            return Vec3.atLowerCornerOf(this.pos).add(0.5D, 1.0D, 0.5D);
        }

        @Override
        protected Direction getInteractionDirection() {
            return Direction.UP;
        }

        /**
         * Returns our nine TACZ ammo slots directly instead of going through the capability cache, so the arm can
         * deposit ammo even before a gun is installed (the entry is created lazily and skipped on save while empty).
         */
        @Override
        @Nullable
        protected IItemHandler getHandler(ArmBlockEntity armBlockEntity) {
            if (!(this.level instanceof ServerLevel serverLevel)) {
                return null;
            }
            MountedGuns data = MountedGuns.get(serverLevel);
            MountedGun gun = data.get(this.pos);
            if (gun == null) {
                float yaw = this.cachedState.hasProperty(CannonMountBlock.HORIZONTAL_FACING)
                        ? this.cachedState.getValue(CannonMountBlock.HORIZONTAL_FACING).toYRot()
                        : 180.0F;
                gun = data.getOrCreate(this.pos, yaw);
            }
            return gun.getAmmo();
        }
    }

    /** Kept so the mod constructor can force Create's registry class to initialise before registration events. */
    public static void ensureRegistryClassLoaded() {
        CreateBuiltInRegistries.init();
    }
}
