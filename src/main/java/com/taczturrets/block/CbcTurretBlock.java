package com.taczturrets.block;

import com.taczturrets.TaczTurrets;
import com.taczturrets.blockentity.CbcTurretBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

/** Turret base driven by a Create Big Cannons cannon mount placed nearby. */
public class CbcTurretBlock extends AbstractTaczTurretBlock {
    public CbcTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CbcTurretBlock> codec() {
        return simpleCodec(CbcTurretBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CbcTurretBlockEntity(TaczTurrets.CBC_TURRET_BE.get(), pos, state);
    }
}

