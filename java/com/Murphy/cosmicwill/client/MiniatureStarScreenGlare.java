package com.Murphy.cosmicwill.client;

import com.Murphy.cosmicwill.CustomWill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class MiniatureStarScreenGlare {

    private static final ResourceLocation GLARE_TEXTURE =
            Objects.requireNonNull(
                    ResourceLocation.tryBuild(
                            CustomWill.MODID,
                            "textures/misc/miniature_star_glare.png"
                    )
            );

    private MiniatureStarScreenGlare() {
    }

    @SubscribeEvent
    public static void onRenderGui(
            RenderGuiEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            MiniatureStarGlareState.clear();
            return;
        }

        float intensity =
                MiniatureStarGlareState.updateAndGet(
                        minecraft.getDeltaFrameTime()
                );

        if (intensity <= 0.01F) {
            return;
        }

        GuiGraphics graphics =
                event.getGuiGraphics();

        int width =
                event.getWindow()
                        .getGuiScaledWidth();

        int height =
                event.getWindow()
                        .getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        /*
         * 第一层：从屏幕四周向中心蔓延的白雾。
         */
        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                Mth.clamp(
                        intensity * 0.96F,
                        0.0F,
                        1.0F
                )
        );

        graphics.blit(
                GLARE_TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                width,
                height,
                256,
                256
        );

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        /*
         * 第二层：极近距离时让画面中央也受到轻微曝光，
         * 确保小于一格时至少半屏处于明显白化状态。
         */
        float close =
                Mth.clamp(
                        (intensity - 0.70F)
                                / 0.30F,
                        0.0F,
                        1.0F
                );

        int closeAlpha =
                (int) (
                        255.0F
                                * 0.22F
                                * close
                                * close
                );

        if (closeAlpha > 0) {
            graphics.fill(
                    0,
                    0,
                    width,
                    height,
                    closeAlpha << 24
                            | 0x00FFFFFF
            );
        }

        RenderSystem.disableBlend();
    }
}
