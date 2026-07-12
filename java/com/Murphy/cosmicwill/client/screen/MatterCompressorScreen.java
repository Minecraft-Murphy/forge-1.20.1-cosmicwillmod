package com.Murphy.cosmicwill.client.screen;

import com.Murphy.cosmicwill.menu.MatterCompressorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MatterCompressorScreen
        extends AbstractContainerScreen<MatterCompressorMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 140;

    public MatterCompressorScreen(
            MatterCompressorMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);

        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
        inventoryLabelY = 47;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);
        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        if (menu.getSlot(0).getItem().isEmpty()
                && isHovering(
                        79,
                        24,
                        18,
                        18,
                        mouseX,
                        mouseY
                )) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.cosmicwill.matter_compressor.anchor"
                    ),
                    mouseX,
                    mouseY
            );
        }

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(
                x,
                y,
                x + imageWidth,
                y + imageHeight,
                0xFFC6C6C6
        );

        graphics.fill(
                x,
                y,
                x + imageWidth,
                y + 2,
                0xFFFFFFFF
        );

        graphics.fill(
                x,
                y,
                x + 2,
                y + imageHeight,
                0xFFFFFFFF
        );

        graphics.fill(
                x,
                y + imageHeight - 2,
                x + imageWidth,
                y + imageHeight,
                0xFF555555
        );

        graphics.fill(
                x + imageWidth - 2,
                y,
                x + imageWidth,
                y + imageHeight,
                0xFF555555
        );

        drawSlot(
                graphics,
                x + 79,
                y + 24
        );

        for (int row = 0; row < 3; row++) {
            for (int column = 0;
                 column < 9;
                 column++) {
                drawSlot(
                        graphics,
                        x + 7 + column * 18,
                        y + 57 + row * 18
                );
            }
        }

        for (int column = 0;
             column < 9;
             column++) {
            drawSlot(
                    graphics,
                    x + 7 + column * 18,
                    y + 115
            );
        }
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawString(
                font,
                title,
                titleLabelX,
                titleLabelY,
                0x404040,
                false
        );

        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                0x404040,
                false
        );
    }

    private static void drawSlot(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(
                x,
                y,
                x + 18,
                y + 18,
                0xFF373737
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + 18,
                y + 2,
                0xFF555555
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + 2,
                y + 18,
                0xFF555555
        );

        graphics.fill(
                x + 2,
                y + 2,
                x + 17,
                y + 17,
                0xFF8B8B8B
        );

        graphics.fill(
                x + 2,
                y + 16,
                x + 17,
                y + 17,
                0xFFFFFFFF
        );

        graphics.fill(
                x + 16,
                y + 2,
                x + 17,
                y + 17,
                0xFFFFFFFF
        );
    }
}
