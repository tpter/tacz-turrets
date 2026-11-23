package com.taczturrets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.taczturrets.entity.TurretShooterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * No-op renderer for the invisible {@link TurretShooterEntity}.
 * <p>
 * The entity must have a registered renderer so {@code EntityRenderDispatcher} does not NPE when it
 * resolves the renderer for the entity type during the render pass (see
 * {@code EntityRenderDispatcher.render} / {@code shouldRender}, which call {@code getRenderer(...)}
 * without a null check). We render nothing: the entity is a world anchor for TACZ's gun-sound packet.
 */
public final class TurretShooterRenderer extends EntityRenderer<TurretShooterEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public TurretShooterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TurretShooterEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(TurretShooterEntity entity, float entityYaw, float partialTick, PoseStack pose,
            MultiBufferSource buffer, int packedLight) {
        // Intentionally empty: the shooter is invisible.
    }
}
