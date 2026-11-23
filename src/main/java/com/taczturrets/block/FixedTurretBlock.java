package com.taczturrets.block;

import com.taczturrets.TaczTurrets;
import com.taczturrets.blockentity.FixedTurretBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

/** Fixed AeroIE turret base: its barrel is bolted to the physics block. */
public class FixedTurretBlock extends AbstractTaczTurretBlock {
    public FixedTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FixedTurretBlock> codec() {
        return simpleCodec(FixedTurretBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FixedTurretBlockEntity(TaczTurrets.FIXED_TURRET_BE.get(), pos, state);
    }
}

