package com.Murphy.cosmicwill.client.screen;

import com.Murphy.cosmicwill.menu.MatterDeconstructorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.util.Locale;

public final class MatterDeconstructorScreen extends AbstractContainerScreen<MatterDeconstructorMenu> {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int TANK_X = 119;
    private static final int TANK_Y = 35;
    private static final int TANK_WIDTH = 25;
    private static final int TANK_HEIGHT = 47;


    private static final ResourceLocation FURNACE_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/furnace.png");

    private Button safeModeButton;

    public MatterDeconstructorScreen(MatterDeconstructorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();

        this.safeModeButton = this.addRenderableWidget(
                Button.builder(Component.empty(), button -> {
                            if (this.minecraft != null && this.minecraft.gameMode != null) {
                                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                            }
                        })
                        .bounds(this.leftPos + 64, this.topPos + 4, 104, 28)
                        .build()
        );
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.safeModeButton != null) {
            this.safeModeButton.setMessage(Component.empty());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        drawSafeModeButtonText(graphics);

        if (this.isHovering(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(
                    this.font,
                    Component.literal(format(this.menu.getMatter()) + " / " + format(this.menu.getCapacity()) + " MU"),
                    mouseX,
                    mouseY
            );
        }

        if (this.menu.getSlot(0).getItem().isEmpty()
                && this.isHovering(28, 48, 18, 18, mouseX, mouseY)) {
            graphics.renderTooltip(
                    this.font,
                    Component.translatable("gui.cosmicwill.matter_deconstructor.input"),
                    mouseX,
                    mouseY
            );
        }

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);

        graphics.fill(x, y, x + this.imageWidth, y + 2, 0xFFFFFFFF);
        graphics.fill(x, y, x + 2, y + this.imageHeight, 0xFFFFFFFF);
        graphics.fill(x, y + this.imageHeight - 2, x + this.imageWidth, y + this.imageHeight, 0xFF555555);
        graphics.fill(x + this.imageWidth - 2, y, x + this.imageWidth, y + this.imageHeight, 0xFF555555);

        drawSlot(graphics, x + 28, y + 48);


        graphics.blit(
                FURNACE_TEXTURE,
                x + 61,
                y + 49,
                79.0F,
                34.0F,
                24,
                17,
                256,
                256
        );

        graphics.fill(
                x + TANK_X,
                y + TANK_Y,
                x + TANK_X + TANK_WIDTH,
                y + TANK_Y + TANK_HEIGHT,
                0xFF373737
        );
        graphics.fill(
                x + TANK_X + 2,
                y + TANK_Y + 2,
                x + TANK_X + TANK_WIDTH - 2,
                y + TANK_Y + TANK_HEIGHT - 2,
                0xFF777777
        );

        long capacity = Math.max(1L, this.menu.getCapacity());
        double ratio = Math.min(1.0D, (double) this.menu.getMatter() / (double) capacity);
        int innerHeight = 43;
        int fillHeight = (int) Math.ceil(innerHeight * ratio);

        if (fillHeight > 0) {
            int fillTop = y + TANK_Y + TANK_HEIGHT - 2 - fillHeight;

            graphics.fill(
                    x + TANK_X + 2,
                    fillTop,
                    x + TANK_X + TANK_WIDTH - 2,
                    y + TANK_Y + TANK_HEIGHT - 2,
                    0xFFB9F7FF
            );
            graphics.fill(
                    x + TANK_X + 4,
                    fillTop,
                    x + TANK_X + 7,
                    y + TANK_Y + TANK_HEIGHT - 2,
                    0xFFFFFFFF
            );
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, x + 7 + column * 18, y + 83 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, x + 7 + column * 18, y + 141);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                this.font,
                this.title,
                this.titleLabelX,
                this.titleLabelY,
                0x404040,
                false
        );

        graphics.drawString(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                0x404040,
                false
        );
    }

    private void drawSafeModeButtonText(GuiGraphics graphics) {
        if (this.safeModeButton == null) {
            return;
        }

        boolean safe = this.menu.isSafeMode();

        Component firstLine = Component.translatable(
                safe
                        ? "gui.cosmicwill.safe_mode.on"
                        : "gui.cosmicwill.safe_mode.off"
        );

        Component secondLine = Component.translatable(
                safe
                        ? "gui.cosmicwill.safe_mode.keep_nbt"
                        : "gui.cosmicwill.safe_mode.destroy_all"
        );

        int centerX = this.safeModeButton.getX() + this.safeModeButton.getWidth() / 2;

        graphics.drawCenteredString(
                this.font,
                firstLine,
                centerX,
                this.safeModeButton.getY() + 4,
                safe ? 0xB8FFB8 : 0xFFB8B8
        );

        graphics.drawCenteredString(
                this.font,
                secondLine,
                centerX,
                this.safeModeButton.getY() + 15,
                0xE0E0E0
        );
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 18, y + 2, 0xFF555555);
        graphics.fill(x + 1, y + 1, x + 2, y + 18, 0xFF555555);
        graphics.fill(x + 2, y + 2, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x + 2, y + 16, x + 17, y + 17, 0xFFFFFFFF);
        graphics.fill(x + 16, y + 2, x + 17, y + 17, 0xFFFFFFFF);
    }

    private static String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }
}
