package com.Murphy.cosmicwill.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.Objects;

public final class MatterCompressionRecipeSerializer
        implements RecipeSerializer<MatterCompressionRecipe> {

    @Override
    public MatterCompressionRecipe fromJson(
            ResourceLocation recipeId,
            JsonObject json
    ) {
        boolean hasNormalInput =
                json.has("input");

        boolean hasBlockTagInput =
                json.has("input_block_tag");

        if (hasNormalInput == hasBlockTagInput) {
            throw new JsonParseException(
                    "Matter compression recipe "
                            + recipeId
                            + " must contain exactly one of "
                            + "'input' or 'input_block_tag'"
            );
        }

        Ingredient input = hasNormalInput
                ? Ingredient.fromJson(
                        GsonHelper.getAsJsonObject(
                                json,
                                "input"
                        )
                )
                : Ingredient.EMPTY;

        @Nullable
        TagKey<Block> inputBlockTag =
                hasBlockTagInput
                        ? TagKey.create(
                                Registries.BLOCK,
                                parseId(
                                        GsonHelper.getAsString(
                                                json,
                                                "input_block_tag"
                                        ),
                                        "input block tag"
                                )
                        )
                        : null;

        int inputCount = GsonHelper.getAsInt(
                json,
                "input_count",
                1
        );

        if (inputCount < 1 || inputCount > 64) {
            throw new JsonParseException(
                    "Matter compression input_count must be between 1 and 64"
            );
        }

        Ingredient anchor = json.has("anchor")
                ? Ingredient.fromJson(
                        GsonHelper.getAsJsonObject(
                                json,
                                "anchor"
                        )
                )
                : Ingredient.EMPTY;

        long matterCost = GsonHelper.getAsLong(
                json,
                "matter_cost"
        );

        int processingTime = GsonHelper.getAsInt(
                json,
                "processing_time",
                100
        );

        boolean consumeAnchor =
                GsonHelper.getAsBoolean(
                        json,
                        "consume_anchor",
                        false
                );

        ItemStack result = readResult(
                GsonHelper.getAsJsonObject(
                        json,
                        "result"
                )
        );

        return new MatterCompressionRecipe(
                recipeId,
                input,
                inputBlockTag,
                inputCount,
                anchor,
                result,
                matterCost,
                processingTime,
                consumeAnchor
        );
    }

    @Override
    @Nullable
    public MatterCompressionRecipe fromNetwork(
            ResourceLocation recipeId,
            FriendlyByteBuf buffer
    ) {
        boolean usesBlockTag =
                buffer.readBoolean();

        Ingredient input = usesBlockTag
                ? Ingredient.EMPTY
                : Ingredient.fromNetwork(buffer);

        @Nullable
        TagKey<Block> inputBlockTag =
                usesBlockTag
                        ? TagKey.create(
                                Registries.BLOCK,
                                buffer.readResourceLocation()
                        )
                        : null;

        int inputCount =
                buffer.readVarInt();

        Ingredient anchor =
                Ingredient.fromNetwork(buffer);

        ItemStack result =
                buffer.readItem();

        long matterCost =
                buffer.readLong();

        int processingTime =
                buffer.readVarInt();

        boolean consumeAnchor =
                buffer.readBoolean();

        return new MatterCompressionRecipe(
                recipeId,
                input,
                inputBlockTag,
                inputCount,
                anchor,
                result,
                matterCost,
                processingTime,
                consumeAnchor
        );
    }

    @Override
    public void toNetwork(
            FriendlyByteBuf buffer,
            MatterCompressionRecipe recipe
    ) {
        buffer.writeBoolean(
                recipe.hasInputBlockTag()
        );

        if (recipe.hasInputBlockTag()) {
            buffer.writeResourceLocation(
                    Objects.requireNonNull(
                            recipe.getInputBlockTag()
                    ).location()
            );
        } else {
            recipe.getInput().toNetwork(buffer);
        }

        buffer.writeVarInt(
                recipe.getInputCount()
        );

        recipe.getAnchor().toNetwork(buffer);
        buffer.writeItem(recipe.getResult());
        buffer.writeLong(recipe.getMatterCost());

        buffer.writeVarInt(
                recipe.getProcessingTime()
        );

        buffer.writeBoolean(
                recipe.shouldConsumeAnchor()
        );
    }

    private static ItemStack readResult(
            JsonObject json
    ) {
        ResourceLocation itemId =
                parseId(
                        GsonHelper.getAsString(
                                json,
                                "item"
                        ),
                        "result item"
                );

        Item item =
                BuiltInRegistries.ITEM.get(itemId);

        if (item == Items.AIR) {
            throw new JsonParseException(
                    "Unknown matter compression result item: "
                            + itemId
            );
        }

        int count = GsonHelper.getAsInt(
                json,
                "count",
                1
        );

        if (count < 1) {
            throw new JsonParseException(
                    "Matter compression result count must be positive"
            );
        }

        return new ItemStack(item, count);
    }

    private static ResourceLocation parseId(
            String value,
            String description
    ) {
        ResourceLocation id =
                ResourceLocation.tryParse(value);

        if (id == null) {
            throw new JsonParseException(
                    "Invalid "
                            + description
                            + ": "
                            + value
            );
        }

        return id;
    }
}
