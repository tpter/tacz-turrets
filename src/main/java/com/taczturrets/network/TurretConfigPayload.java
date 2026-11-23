package com.taczturrets.network;

import com.taczturrets.TaczTurrets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server request carrying the full tuning set of a turret (render scale, gun re-centring
 * offset, idle angles and weapon channel). The turret screen sends this on every slider drag / channel
 * toggle; the server applies it in one go and re-syncs the block entity.
 */
public record TurretConfigPayload(BlockPos pos, float scale, float offsetX, float offsetY, float offsetZ,
        float defaultYaw, float defaultPitch, int channel) implements CustomPacketPayload {

    public static final Type<TurretConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTurrets.MODID, "turret_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TurretConfigPayload> STREAM_CODEC =
            StreamCodec.of(
                    (RegistryFriendlyByteBuf buf, TurretConfigPayload payload) -> {
                        BlockPos.STREAM_CODEC.encode(buf, payload.pos);
                        buf.writeFloat(payload.scale);
                        buf.writeFloat(payload.offsetX);
                        buf.writeFloat(payload.offsetY);
                        buf.writeFloat(payload.offsetZ);
                        buf.writeFloat(payload.defaultYaw);
                        buf.writeFloat(payload.defaultPitch);
                        buf.writeInt(payload.channel);
                    },
                    (RegistryFriendlyByteBuf buf) -> new TurretConfigPayload(
                            BlockPos.STREAM_CODEC.decode(buf),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
