package com.taczturrets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.taczturrets.TaczTurrets;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the TACZ guns that are mounted on CBC cannon mounts.
 * <p>
 * CBC's cannon mount block entity already has its own renderer, so this cannot be a block entity renderer: the gun
 * is drawn in the level render pass from the position/angle state synced by the server.
 */
@EventBusSubscriber(modid = TaczTurrets.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class MountedGunRenderer {
    private static final double MAX_RENDER_DISTANCE_SQR = 128.0D * 128.0D;

    // The gun model's rotation around the cannon mount origin is configurable in game with
    // /tczmodel set mount <yaw|pitch|roll> <degrees> - see TurretModelConfig.

    private MountedGunRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ClientMountedGuns.view().isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        for (Map.Entry<BlockPos, ClientMountedGuns.Entry> entry : ClientMountedGuns.view().entrySet()) {
            BlockPos pos = entry.getKey();
            ClientMountedGuns.Entry gun = entry.getValue();
            // The server sends the WORLD pivot (already transformed by the Sable sub-level pose), so a gun
            // mounted on a physicalized ship renders at the ship's actual position instead of empty space.
            Vec3 center = new Vec3(gun.worldX(), gun.worldY(), gun.worldZ());
            if (center.distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) {
                continue;
            }
            pose.pushPose();
            pose.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(-gun.yaw()));
            pose.mulPose(Axis.XP.rotationDegrees(-gun.pitch()));
            // Model correction: yaw, then pitch, then roll. Values live in TurretModelConfig and are editable in game
            // with /tczmodel set mount <yaw|pitch|roll> <degrees> (no restart needed).
            TurretModelConfig.load();
            pose.mulPose(Axis.YP.rotationDegrees(TurretModelConfig.mountYaw));
            pose.mulPose(Axis.XP.rotationDegrees(TurretModelConfig.mountPitch));
            pose.mulPose(Axis.ZP.rotationDegrees(TurretModelConfig.mountRoll));
            pose.scale(gun.scale(), gun.scale(), gun.scale());
            int light = LevelRenderer.getLightColor(minecraft.level, pos);
            minecraft.getItemRenderer().renderStatic(gun.gun(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, minecraft.level, 0);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
