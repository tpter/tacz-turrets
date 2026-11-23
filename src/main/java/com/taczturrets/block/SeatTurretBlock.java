package com.taczturrets.block;

import com.taczturrets.TaczTurrets;
import com.taczturrets.blockentity.SeatTurretBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

/** AeroIE turret base meant to be linked to a control seat with the AeroIE linker. */
public class SeatTurretBlock extends AbstractTaczTurretBlock {
    public SeatTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SeatTurretBlock> codec() {
        return simpleCodec(SeatTurretBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeatTurretBlockEntity(TaczTurrets.SEAT_TURRET_BE.get(), pos, state);
    }
}

