package com.Murphy.cosmicwill.recipe;

import com.Murphy.cosmicwill.registry.CWBlocks;
import com.Murphy.cosmicwill.registry.CWRecipeSerializers;
import com.Murphy.cosmicwill.registry.CWRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MatterCompressionRecipe
        implements Recipe<Container> {

    private final ResourceLocation id;
    private final Ingredient input;

    @Nullable
    private final TagKey<Block> inputBlockTag;

    /**
     * 世界加工位中必须至少存在的同种物品数量。
     */
    private final int inputCount;

    private final Ingredient anchor;
    private final ItemStack result;
    private final long matterCost;
    private final int processingTime;
    private final boolean consumeAnchor;

    public MatterCompressionRecipe(
            ResourceLocation id,
            Ingredient input,
            @Nullable TagKey<Block> inputBlockTag,
            int inputCount,
            Ingredient anchor,
            ItemStack result,
            long matterCost,
            int processingTime,
            boolean consumeAnchor
    ) {
        this.id = id;
        this.input = input;
        this.inputBlockTag = inputBlockTag;
        this.inputCount = Math.max(1, inputCount);
        this.anchor = anchor;
        this.result = result;
        this.matterCost = Math.max(0L, matterCost);
        this.processingTime = Math.max(1, processingTime);
        this.consumeAnchor = consumeAnchor;
    }

    @Override
    public boolean matches(
            Container container,
            Level level
    ) {
        ItemStack inputStack =
                container.getItem(0);

        ItemStack anchorStack =
                container.getItem(1);

        if (!matchesInput(inputStack)) {
            return false;
        }

        if (anchor.isEmpty()) {
            return anchorStack.isEmpty();
        }

        return anchor.test(anchorStack);
    }

    public boolean matchesInput(ItemStack stack) {
        return stack.getCount() >= inputCount
                && matchesInputItem(stack);
    }

    /**
     * 只判断物品种类，不判断数量和锚点。
     *
     * 压缩器用它决定潜行右键时应从玩家手中吸入多少个同类物品。
     */
    public boolean matchesInputItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (inputBlockTag == null) {
            return input.test(stack);
        }

        if (!(stack.getItem()
                instanceof BlockItem blockItem)) {
            return false;
        }

        return blockItem
                .getBlock()
                .defaultBlockState()
                .is(inputBlockTag);
    }

    @Override
    public ItemStack assemble(
            Container container,
            RegistryAccess registryAccess
    ) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return true;
    }

    @Override
    public ItemStack getResultItem(
            RegistryAccess registryAccess
    ) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients =
                NonNullList.create();

        if (inputBlockTag == null) {
            ingredients.add(input);
        }

        if (!anchor.isEmpty()) {
            ingredients.add(anchor);
        }

        return ingredients;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(
                CWBlocks.MATTER_COMPRESSOR.get()
        );
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CWRecipeSerializers
                .MATTER_COMPRESSING
                .get();
    }

    @Override
    public RecipeType<?> getType() {
        return CWRecipeTypes
                .MATTER_COMPRESSING
                .get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public Ingredient getInput() {
        return input;
    }

    public boolean hasInputBlockTag() {
        return inputBlockTag != null;
    }

    @Nullable
    public TagKey<Block> getInputBlockTag() {
        return inputBlockTag;
    }

    public int getInputCount() {
        return inputCount;
    }

    /**
     * JEI 展示使用：所有候选物品都携带实际需求数量。
     */
    public List<ItemStack> getDisplayInputs() {
        List<ItemStack> stacks =
                new ArrayList<>();

        if (inputBlockTag == null) {
            stacks.addAll(
                    Arrays.asList(
                            input.getItems()
                    )
            );
        } else {
            BuiltInRegistries.BLOCK
                    .getTag(inputBlockTag)
                    .ifPresent(namedSet ->
                            namedSet.forEach(holder -> {
                                Item item = holder
                                        .value()
                                        .asItem();

                                if (item != Items.AIR) {
                                    stacks.add(
                                            new ItemStack(item)
                                    );
                                }
                            })
                    );
        }

        for (ItemStack stack : stacks) {
            stack.setCount(
                    Math.min(
                            inputCount,
                            stack.getMaxStackSize()
                    )
            );
        }

        return stacks;
    }

    public Ingredient getAnchor() {
        return anchor;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public long getMatterCost() {
        return matterCost;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public boolean shouldConsumeAnchor() {
        return consumeAnchor;
    }
}
