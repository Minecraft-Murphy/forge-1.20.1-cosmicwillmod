package com.Murphy.cosmicwill.jei;

import com.Murphy.cosmicwill.CWitems;
import com.Murphy.cosmicwill.recipe.MatterCompressionRecipe;
import com.Murphy.cosmicwill.registry.CWBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Objects;

public final class MatterCompressionRecipeCategory
        implements IRecipeCategory<MatterCompressionRecipe> {

    private static final int WIDTH = 140;
    private static final int HEIGHT = 78;

    private static final int INPUT_X = 8;
    private static final int INPUT_Y = 24;

    private static final int ANCHOR_X = 61;
    private static final int ANCHOR_Y = 1;

    private static final int ARROW_X = 55;
    private static final int ARROW_Y = 26;

    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 24;

    private static final ResourceLocation UNIFORM_FONT =
            Objects.requireNonNull(
                    ResourceLocation.tryBuild(
                            "minecraft",
                            "uniform"
                    )
            );

    private final IDrawable icon;
    private final IDrawable arrow;

    public MatterCompressionRecipeCategory(
            IGuiHelper guiHelper
    ) {
        this.icon = guiHelper.createDrawableItemLike(
                CWBlocks.MATTER_COMPRESSOR.get()
        );

        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<MatterCompressionRecipe>
    getRecipeType() {
        return CWJeiPlugin.MATTER_COMPRESSION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(
                "jei.cosmicwill.matter_compressing"
        );
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            MatterCompressionRecipe recipe,
            IFocusGroup focuses
    ) {
        var inputSlot = builder.addSlot(
                        RecipeIngredientRole.INPUT,
                        INPUT_X,
                        INPUT_Y
                )
                .setStandardSlotBackground();

        for (ItemStack stack : recipe.getDisplayInputs()) {
            inputSlot.addItemStack(stack);
        }

        RecipeIngredientRole anchorRole =
                recipe.shouldConsumeAnchor()
                        ? RecipeIngredientRole.INPUT
                        : RecipeIngredientRole.CATALYST;

        var anchorSlot = builder.addSlot(
                        anchorRole,
                        ANCHOR_X,
                        ANCHOR_Y
                )
                .setStandardSlotBackground();

        if (!recipe.getAnchor().isEmpty()) {
            anchorSlot.addIngredients(
                    recipe.getAnchor()
            );
        }

        builder.addSlot(
                        RecipeIngredientRole.OUTPUT,
                        OUTPUT_X,
                        OUTPUT_Y
                )
                .setOutputSlotBackground()
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(
            MatterCompressionRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        arrow.draw(
                graphics,
                ARROW_X,
                ARROW_Y
        );

        Font font = Minecraft.getInstance().font;

        if (!recipe.getAnchor().isEmpty()) {
            Component anchorType =
                    Component.translatable(
                            recipe.shouldConsumeAnchor()
                                    ? "jei.cosmicwill.anchor_consumable"
                                    : "jei.cosmicwill.anchor_catalyst"
                    ).withStyle(style ->
                            style
                                    .withFont(UNIFORM_FONT)
                                    .withBold(true)
                    );

            int labelColor =
                    recipe.shouldConsumeAnchor()
                            ? 0xFFB23A3A
                            : 0xFF2C6F9E;

            graphics.drawString(
                    font,
                    anchorType,
                    ANCHOR_X + 21,
                    ANCHOR_Y + 5,
                    labelColor,
                    false
            );
        }

        String costString =
                String.format(
                        Locale.ROOT,
                        "%,d MU",
                        recipe.getMatterCost()
                );

        Component costText =
                Component.literal(costString)
                        .withStyle(style ->
                                style
                                        .withFont(UNIFORM_FONT)
                                        .withBold(true)
                        );

        int textWidth = font.width(costText);
        int textX = (WIDTH - textWidth) / 2;
        int textY = 50;

        graphics.fill(
                textX - 4,
                textY - 2,
                textX + textWidth + 4,
                textY + 11,
                0xB8EEEEEE
        );

        graphics.fill(
                textX - 4,
                textY - 2,
                textX + textWidth + 4,
                textY - 1,
                0xFF777777
        );

        graphics.drawString(
                font,
                costText,
                textX,
                textY,
                0xFF202020,
                false
        );

        /*
         * 人造奇点压缩完成后，压缩器会转入锁定的奇点状态。
         * 这个提示直接显示在 JEI 页面中，避免玩家提前进行不可逆压缩。
         */
        if (isArtificialSingularityRecipe(recipe)) {
            Component warning =
                    Component.translatable(
                            "jei.cosmicwill.singularity_lock_warning"
                    ).withStyle(style ->
                            style
                                    .withFont(UNIFORM_FONT)
                                    .withBold(true)
                    );

            int warningWidth = font.width(warning);
            int warningX = (WIDTH - warningWidth) / 2;

            graphics.drawString(
                    font,
                    warning,
                    warningX,
                    65,
                    0xFFFF3030,
                    false
            );
        }
    }

    @Override
    public void getTooltip(
            ITooltipBuilder tooltip,
            MatterCompressionRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY
    ) {
        boolean hoveringAnchor =
                mouseX >= ANCHOR_X - 1
                        && mouseX <= ANCHOR_X + 17
                        && mouseY >= ANCHOR_Y - 1
                        && mouseY <= ANCHOR_Y + 17;

        if (hoveringAnchor) {
            if (recipe.getAnchor().isEmpty()) {
                tooltip.add(
                        Component.translatable(
                                "jei.cosmicwill.no_anchor"
                        )
                );
            } else if (recipe.shouldConsumeAnchor()) {
                tooltip.add(
                        Component.translatable(
                                "jei.cosmicwill.anchor_consumed"
                        )
                );
            } else {
                tooltip.add(
                        Component.translatable(
                                "jei.cosmicwill.anchor_not_consumed"
                        )
                );
            }
        }

        if (isArtificialSingularityRecipe(recipe)
                && mouseY >= 62.0D) {
            tooltip.add(
                    Component.translatable(
                            "jei.cosmicwill.singularity_lock_warning"
                    ).withStyle(style ->
                            style
                                    .withColor(0xFF3030)
                                    .withBold(true)
                    )
            );
        }
    }

    private static boolean isArtificialSingularityRecipe(
            MatterCompressionRecipe recipe
    ) {
        return recipe.getResult()
                .is(CWitems.ARTIFICIAL_SINGULARITY.get());
    }
}
