package com.taczturrets.mount;

import com.taczturrets.TaczTurrets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Client sync of a gun mounted on a CBC cannon mount. {@code yaw}/{@code pitch} are WORLD-space aim angles (the
 * server already rotated them by the Sable sub-level pose when the mount is on a physicalized ship), and
 * {@code worldX/Y/Z} is the WORLD position of the gun pivot (same transform), so the renderer does not need any
 * ship pose of its own.
 */
public record MountedGunSyncPayload(BlockPos pos, ItemStack gun, float yaw, float pitch, float scale, boolean remove,
                                    double worldX, double worldY, double worldZ)
        implements CustomPacketPayload {
    public static final Type<MountedGunSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTurrets.MODID, "mounted_gun_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MountedGunSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.gun());
                buf.writeFloat(payload.yaw());
                buf.writeFloat(payload.pitch());
                buf.writeFloat(payload.scale());
                buf.writeBoolean(payload.remove());
                buf.writeDouble(payload.worldX());
                buf.writeDouble(payload.worldY());
                buf.writeDouble(payload.worldZ());
            },
            buf -> new MountedGunSyncPayload(BlockPos.STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
