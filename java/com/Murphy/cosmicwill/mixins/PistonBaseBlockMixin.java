package com.Murphy.cosmicwill.mixins;

import com.Murphy.cosmicwill.block.CompressedPistonBlock;
import com.Murphy.cosmicwill.registry.CWBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {

    @Redirect(
            method = "moveBlocks",
            at = @At(
                    value = "FIELD",
                    target =
                            "Lnet/minecraft/world/level/block/Blocks;"
                                    + "PISTON_HEAD:"
                                    + "Lnet/minecraft/world/level/block/Block;"
            )
    )
    private Block cosmicwill$useCompressedPistonHead() {
        if ((Object) this
                instanceof CompressedPistonBlock) {
            return CWBlocks.COMPRESSED_PISTON_HEAD.get();
        }

        return Blocks.PISTON_HEAD;
    }
}
