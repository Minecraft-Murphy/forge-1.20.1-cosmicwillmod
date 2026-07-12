package com.Murphy.cosmicwill.blockentity;

import com.Murphy.cosmicwill.registry.CWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MiniatureStarBlockEntity
        extends BlockEntity {

    public MiniatureStarBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                CWBlockEntities.MINIATURE_STAR.get(),
                pos,
                state
        );
    }
}
