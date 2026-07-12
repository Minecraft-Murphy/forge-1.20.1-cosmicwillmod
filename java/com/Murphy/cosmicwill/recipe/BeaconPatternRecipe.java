package com.Murphy.cosmicwill.recipe;

import com.Murphy.cosmicwill.CWitems;
import com.Murphy.cosmicwill.registry.CWBlocks;
import com.Murphy.cosmicwill.registry.CWRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BeaconPatternRecipe
        extends CustomRecipe
        implements IShapedRecipe<CraftingContainer> {

    private static final TagKey<Block> BEACON_BASE_BLOCKS =
            TagKey.create(
                    Registries.BLOCK,
                    Objects.requireNonNull(
                            ResourceLocation.tryBuild(
                                    "minecraft",
                                    "beacon_base_blocks"
                            )
                    )
            );

    public enum Mode {
        STRONG_MATERIAL_STAR_RING,
        STRONG_MATERIAL_PRIMORDIUM_CROSS,
        COMPRESSED_FURNACE
    }

    private final Mode mode;

    public BeaconPatternRecipe(
            ResourceLocation id,
            CraftingBookCategory category,
            Mode mode
    ) {
        super(id, category);
        this.mode = mode;
    }

    @Override
    public boolean matches(
            CraftingContainer container,
            Level level
    ) {
        if (container.getWidth() != 3
                || container.getHeight() != 3) {
            return false;
        }

        return switch (mode) {
            case STRONG_MATERIAL_STAR_RING ->
                    matchesStarRing(container);

            case STRONG_MATERIAL_PRIMORDIUM_CROSS ->
                    matchesPrimordiumCross(container);

            case COMPRESSED_FURNACE ->
                    matchesCompressedFurnace(container);
        };
    }

    private static boolean matchesStarRing(
            CraftingContainer container
    ) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = container.getItem(slot);

            if (slot == 4) {
                if (!stack.is(Items.NETHER_STAR)) {
                    return false;
                }
            } else if (!isBeaconBase(stack)) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchesPrimordiumCross(
            CraftingContainer container
    ) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = container.getItem(slot);

            if (slot == 4) {
                if (!stack.is(
                        CWitems
                                .STRONG_INTERACTION_PRIMORDIUM
                                .get()
                )) {
                    return false;
                }
                continue;
            }

            if (slot == 1
                    || slot == 3
                    || slot == 5
                    || slot == 7) {
                if (!isBeaconBase(stack)) {
                    return false;
                }
            } else if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchesCompressedFurnace(
            CraftingContainer container
    ) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = container.getItem(slot);

            if (slot == 4) {
                if (!isBeaconBase(stack)) {
                    return false;
                }
            } else if (slot == 7) {
                if (!stack.is(Items.FURNACE)
                        && !stack.is(Items.BLAST_FURNACE)) {
                    return false;
                }
            } else if (!stack.is(Items.COBBLESTONE)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isBeaconBase(
            ItemStack stack
    ) {
        if (!(stack.getItem()
                instanceof BlockItem blockItem)) {
            return false;
        }

        return blockItem
                .getBlock()
                .defaultBlockState()
                .is(BEACON_BASE_BLOCKS);
    }

    @Override
    public ItemStack assemble(
            CraftingContainer container,
            RegistryAccess registryAccess
    ) {
        return result();
    }

    @Override
    public ItemStack getResultItem(
            RegistryAccess registryAccess
    ) {
        return result();
    }

    private ItemStack result() {
        return switch (mode) {
            case STRONG_MATERIAL_STAR_RING,
                    STRONG_MATERIAL_PRIMORDIUM_CROSS ->
                    new ItemStack(
                            CWBlocks
                                    .STRONG_INTERACTION_MATERIAL
                                    .get(),
                            4
                    );

            case COMPRESSED_FURNACE ->
                    new ItemStack(
                            CWBlocks.COMPRESSED_FURNACE.get()
                    );
        };
    }

    /**
     * JEI 展示时动态收集所有真正属于
     * minecraft:beacon_base_blocks 方块标签的 BlockItem。
     *
     * 因而第三方模组只要正确把自己的方块加入原版信标基座方块标签，
     * 工作台匹配与 JEI 轮换展示都会自动兼容。
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients =
                NonNullList.withSize(
                        9,
                        Ingredient.EMPTY
                );

        Ingredient beaconBase =
                createBeaconBaseIngredient();

        switch (mode) {
            case STRONG_MATERIAL_STAR_RING -> {
                for (int slot = 0; slot < 9; slot++) {
                    ingredients.set(
                            slot,
                            slot == 4
                                    ? Ingredient.of(
                                    Items.NETHER_STAR
                            )
                                    : beaconBase
                    );
                }
            }

            case STRONG_MATERIAL_PRIMORDIUM_CROSS -> {
                ingredients.set(1, beaconBase);
                ingredients.set(3, beaconBase);
                ingredients.set(
                        4,
                        Ingredient.of(
                                CWitems
                                        .STRONG_INTERACTION_PRIMORDIUM
                                        .get()
                        )
                );
                ingredients.set(5, beaconBase);
                ingredients.set(7, beaconBase);
            }

            case COMPRESSED_FURNACE -> {
                Ingredient cobblestone =
                        Ingredient.of(
                                Items.COBBLESTONE
                        );

                for (int slot = 0; slot < 9; slot++) {
                    ingredients.set(
                            slot,
                            cobblestone
                    );
                }

                ingredients.set(4, beaconBase);
                ingredients.set(
                        7,
                        Ingredient.of(
                                Items.FURNACE,
                                Items.BLAST_FURNACE
                        )
                );
            }
        }

        return ingredients;
    }

    private static Ingredient createBeaconBaseIngredient() {
        List<ItemStack> stacks =
                new ArrayList<>();

        for (Item item
                : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof BlockItem blockItem)) {
                continue;
            }

            if (blockItem
                    .getBlock()
                    .defaultBlockState()
                    .is(BEACON_BASE_BLOCKS)) {
                stacks.add(
                        new ItemStack(item)
                );
            }
        }

        if (stacks.isEmpty()) {
            return Ingredient.of(
                    Items.IRON_BLOCK,
                    Items.GOLD_BLOCK,
                    Items.DIAMOND_BLOCK,
                    Items.EMERALD_BLOCK,
                    Items.NETHERITE_BLOCK
            );
        }

        return Ingredient.of(
                stacks.toArray(
                        ItemStack[]::new
                )
        );
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return width >= 3 && height >= 3;
    }

    @Override
    public int getRecipeWidth() {
        return 3;
    }

    @Override
    public int getRecipeHeight() {
        return 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return switch (mode) {
            case STRONG_MATERIAL_STAR_RING ->
                    CWRecipeSerializers
                            .STRONG_MATERIAL_STAR_RING
                            .get();

            case STRONG_MATERIAL_PRIMORDIUM_CROSS ->
                    CWRecipeSerializers
                            .STRONG_MATERIAL_PRIMORDIUM_CROSS
                            .get();

            case COMPRESSED_FURNACE ->
                    CWRecipeSerializers
                            .COMPRESSED_FURNACE_BEACON
                            .get();
        };
    }
}
