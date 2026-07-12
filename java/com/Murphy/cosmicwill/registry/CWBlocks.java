package com.Murphy.cosmicwill.registry;

import com.Murphy.cosmicwill.CWitems;
import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.block.CompressedFurnaceBlock;
import com.Murphy.cosmicwill.block.CompressedPistonBlock;
import com.Murphy.cosmicwill.block.CompressedPistonHeadBlock;
import com.Murphy.cosmicwill.block.MatterCompressorBlock;
import com.Murphy.cosmicwill.block.MatterDeconstructorBlock;
import com.Murphy.cosmicwill.block.MiniatureStarBlock;
import com.Murphy.cosmicwill.block.StellarShellBlock;
import com.Murphy.cosmicwill.block.StrongInteractionMaterialBlock;
import com.Murphy.cosmicwill.item.AlwaysFoilBlockItem;
import com.Murphy.cosmicwill.item.MiniatureStarBlockItem;
import com.Murphy.cosmicwill.item.StatefulMachineBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class CWBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(
                    ForgeRegistries.BLOCKS,
                    CustomWill.MODID
            );

    public static final RegistryObject<Block> COMPRESSED_FURNACE =
            registerBlock(
                    "compressed_furnace",
                    () -> new CompressedFurnaceBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.FURNACE)
                                    .lightLevel(state ->
                                            state.getValue(
                                                    CompressedFurnaceBlock.LIT
                                            ) ? 13 : 0
                                    )
                    ),
                    false
            );

    public static final RegistryObject<Block> COMPRESSED_PISTON =
            registerBlock(
                    "compressed_piston",
                    () -> new CompressedPistonBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.PISTON)
                                    .strength(4.0F, 12.0F)
                    ),
                    false
            );

    /**
     * 活塞运动过程中由 PistonBaseBlockMixin 自动使用。
     * 这是内部结构方块，不注册 BlockItem，也不会进入创造栏。
     */
    public static final RegistryObject<Block> COMPRESSED_PISTON_HEAD =
            BLOCKS.register(
                    "compressed_piston_head",
                    () -> new CompressedPistonHeadBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.PISTON_HEAD)
                    )
            );

    public static final RegistryObject<Block> MATTER_DECONSTRUCTOR =
            registerBlock(
                    "matter_deconstructor",
                    () -> new MatterDeconstructorBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.IRON_BLOCK)
                                    .strength(5.0F, 8.0F)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(state ->
                                            state.getValue(
                                                    MatterDeconstructorBlock.LIT
                                            ) ? 13 : 0
                                    )
                    ),
                    true
            );

    public static final RegistryObject<Block> MATTER_COMPRESSOR =
            registerBlock(
                    "matter_compressor",
                    () -> new MatterCompressorBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.IRON_BLOCK)
                                    .strength(6.0F, 10.0F)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()
                                    .lightLevel(state -> {
                                        if (state.getValue(
                                                MatterCompressorBlock.SINGULARITY
                                        )) {
                                            return 12;
                                        }

                                        return state.getValue(
                                                MatterCompressorBlock.WORKING
                                        ) ? 6 : 0;
                                    })
                    ),
                    false
            );

    public static final RegistryObject<Block> STELLAR_REMNANT =
            registerBlock(
                    "stellar_remnant",
                    () -> new Block(
                            BlockBehaviour.Properties
                                    .copy(Blocks.ANCIENT_DEBRIS)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(state -> 5)
                    ),
                    false,
                    new Item.Properties()
                            .fireResistant()
            );

    public static final RegistryObject<Block> NETHER_STAR_BLOCK =
            registerFoilBlock(
                    "nether_star_block",
                    () -> new Block(
                            BlockBehaviour.Properties
                                    .copy(Blocks.NETHERITE_BLOCK)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(state -> 8)
                    ),
                    new Item.Properties()
                            .fireResistant()
                            .rarity(Rarity.RARE)
            );

    public static final RegistryObject<Block> STELLAR_SHELL =
            registerBlock(
                    "stellar_shell",
                    () -> new StellarShellBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.OBSIDIAN)
                                    .strength(60.0F, 1400.0F)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(state -> 12)
                    ),
                    false,
                    new Item.Properties()
                            .fireResistant()
                            .rarity(Rarity.RARE)
            );

    public static final RegistryObject<Block> MINIATURE_STAR =
            registerMiniatureStar(
                    "miniature_star",
                    () -> new MiniatureStarBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.OBSIDIAN)
                                    .strength(25.0F, 1200.0F)
                                    .requiresCorrectToolForDrops()
                                    .lightLevel(state -> 15)
                    ),
                    new Item.Properties()
                            .stacksTo(16)
                            .fireResistant()
                            .rarity(Rarity.EPIC)
            );

    /**
     * 强相互作用原质经过结构化排列后形成的工程材料块。
     */
    public static final RegistryObject<Block>
            STRONG_INTERACTION_MATERIAL =
            registerBlock(
                    "strong_interaction_material",
                    () -> new StrongInteractionMaterialBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.NETHERITE_BLOCK)
                                    .strength(15.0F, 1200.0F)
                                    .requiresCorrectToolForDrops()
                    ),
                    false,
                    new Item.Properties()
                            .fireResistant()
                            .rarity(Rarity.RARE)
            );

    private CWBlocks() {
    }

    private static <T extends Block> RegistryObject<T> registerBlock(
            String name,
            Supplier<T> supplier,
            boolean statefulItem
    ) {
        return registerBlock(
                name,
                supplier,
                statefulItem,
                new Item.Properties()
        );
    }

    private static <T extends Block> RegistryObject<T> registerBlock(
            String name,
            Supplier<T> supplier,
            boolean statefulItem,
            Item.Properties itemProperties
    ) {
        RegistryObject<T> block =
                BLOCKS.register(name, supplier);

        CWitems.ITEMS.register(
                name,
                () -> statefulItem
                        ? new StatefulMachineBlockItem(
                                block.get(),
                                itemProperties
                        )
                        : new BlockItem(
                                block.get(),
                                itemProperties
                        )
        );

        return block;
    }

    private static <T extends Block> RegistryObject<T> registerFoilBlock(
            String name,
            Supplier<T> supplier,
            Item.Properties itemProperties
    ) {
        RegistryObject<T> block =
                BLOCKS.register(name, supplier);

        CWitems.ITEMS.register(
                name,
                () -> new AlwaysFoilBlockItem(
                        block.get(),
                        itemProperties
                )
        );

        return block;
    }

    private static <T extends Block> RegistryObject<T> registerMiniatureStar(
            String name,
            Supplier<T> supplier,
            Item.Properties itemProperties
    ) {
        RegistryObject<T> block =
                BLOCKS.register(name, supplier);

        CWitems.ITEMS.register(
                name,
                () -> new MiniatureStarBlockItem(
                        block.get(),
                        itemProperties
                )
        );

        return block;
    }
}
