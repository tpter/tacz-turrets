package com.taczturrets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.taczturrets.blockentity.AbstractTaczTurretBlockEntity;
import com.taczturrets.blockentity.FixedTurretBlockEntity;
import com.taczturrets.blockentity.SeatTurretBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the installed TACZ gun on top of a turret base, rotated with the turret and scaled by the
 * per-turret render scale. The gun item is rendered through the vanilla item renderer, so TACZ's own
 * model and textures (and therefore its gun effects, which are driven by the item id) are preserved.
 * <p>
 * Model correction angles come from {@link TurretModelConfig}, which the client command {@code /tczmodel} edits while
 * the game runs - so the numbers can be tuned in front of the turret without restarting or reloading anything.
 */
public class TurretGunRenderer implements BlockEntityRenderer<AbstractTaczTurretBlockEntity> {
    public TurretGunRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AbstractTaczTurretBlockEntity turret, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack gun = turret.getGunStack();
        if (gun.isEmpty()) {
            return;
        }
        Vec3 direction = turret.getLocalBarrelDirection();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D)));

        TurretModelConfig.load();
        float modelYaw;
        float modelPitch;
        float modelRoll;
        if (turret instanceof SeatTurretBlockEntity) {
            modelYaw = TurretModelConfig.seatYaw;
            modelPitch = TurretModelConfig.seatPitch;
            modelRoll = TurretModelConfig.seatRoll;
        } else if (turret instanceof FixedTurretBlockEntity) {
            modelYaw = TurretModelConfig.fixedYaw;
            modelPitch = TurretModelConfig.fixedPitch;
            modelRoll = TurretModelConfig.fixedRoll;
        } else {
            // Unknown/legacy turret: fall back to whatever the block entity declares.
            modelYaw = turret.getGunModelYawDegrees();
            modelPitch = turret.getGunModelPitchDegrees();
            modelRoll = 0.0F;
        }

        pose.pushPose();
        pose.translate(0.5D, 0.5D + 0.55D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        // Per-turret model correction around the turret origin, applied innermost: yaw, then pitch, then roll.
        pose.mulPose(Axis.YP.rotationDegrees(modelYaw));
        pose.mulPose(Axis.XP.rotationDegrees(modelPitch));
        pose.mulPose(Axis.ZP.rotationDegrees(modelRoll));
        // Re-centring offset in the gun's local frame (after the model rotation), so oversized gun packs
        // whose origin is not at their visual centre can be nudged back onto the turret pivot.
        pose.translate(turret.getGunOffsetX(), turret.getGunOffsetY(), turret.getGunOffsetZ());
        float scale = turret.getGunScale();
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(gun, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                pose, buffers, turret.getLevel(), 0);
        pose.popPose();
    }
}
