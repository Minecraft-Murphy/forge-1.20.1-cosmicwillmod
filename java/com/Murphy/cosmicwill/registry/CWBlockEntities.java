package com.Murphy.cosmicwill.registry;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.blockentity.CompressedFurnaceBlockEntity;
import com.Murphy.cosmicwill.blockentity.MatterCompressorBlockEntity;
import com.Murphy.cosmicwill.blockentity.MatterDeconstructorBlockEntity;
import com.Murphy.cosmicwill.blockentity.MiniatureStarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CWBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>>
            BLOCK_ENTITIES =
            DeferredRegister.create(
                    ForgeRegistries.BLOCK_ENTITY_TYPES,
                    CustomWill.MODID
            );

    public static final RegistryObject<
            BlockEntityType<CompressedFurnaceBlockEntity>
            > COMPRESSED_FURNACE =
            BLOCK_ENTITIES.register(
                    "compressed_furnace",
                    () -> BlockEntityType.Builder
                            .of(
                                    CompressedFurnaceBlockEntity::new,
                                    CWBlocks.COMPRESSED_FURNACE.get()
                            )
                            .build(null)
            );

    public static final RegistryObject<
            BlockEntityType<MatterDeconstructorBlockEntity>
            > MATTER_DECONSTRUCTOR =
            BLOCK_ENTITIES.register(
                    "matter_deconstructor",
                    () -> BlockEntityType.Builder
                            .of(
                                    MatterDeconstructorBlockEntity::new,
                                    CWBlocks.MATTER_DECONSTRUCTOR.get()
                            )
                            .build(null)
            );

    public static final RegistryObject<
            BlockEntityType<MatterCompressorBlockEntity>
            > MATTER_COMPRESSOR =
            BLOCK_ENTITIES.register(
                    "matter_compressor",
                    () -> BlockEntityType.Builder
                            .of(
                                    MatterCompressorBlockEntity::new,
                                    CWBlocks.MATTER_COMPRESSOR.get()
                            )
                            .build(null)
            );

    public static final RegistryObject<
            BlockEntityType<MiniatureStarBlockEntity>
            > MINIATURE_STAR =
            BLOCK_ENTITIES.register(
                    "miniature_star",
                    () -> BlockEntityType.Builder
                            .of(
                                    MiniatureStarBlockEntity::new,
                                    CWBlocks.MINIATURE_STAR.get()
                            )
                            .build(null)
            );

    private CWBlockEntities() {
    }
}
