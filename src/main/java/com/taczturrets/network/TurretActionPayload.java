package com.taczturrets.network;

import com.taczturrets.TaczTurrets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server request for one of the turret actions that used to be bound to a right click.
 * The action is executed by {@link TurretActions} on the server thread.
 */
public record TurretActionPayload(BlockPos pos, byte action) implements CustomPacketPayload {
    public static final byte INSTALL_GUN = 0;
    public static final byte OPEN_MENU = 1;
    public static final byte CYCLE_SCALE = 2;

    public static final Type<TurretActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTurrets.MODID, "turret_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TurretActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TurretActionPayload::pos,
                    ByteBufCodecs.BYTE, TurretActionPayload::action,
                    TurretActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
