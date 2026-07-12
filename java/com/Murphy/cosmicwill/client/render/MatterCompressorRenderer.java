package com.Murphy.cosmicwill.client.render;

import com.Murphy.cosmicwill.CWitems;
import com.Murphy.cosmicwill.block.MatterCompressorBlock;
import com.Murphy.cosmicwill.blockentity.MatterCompressorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MatterCompressorRenderer
        implements BlockEntityRenderer<
        MatterCompressorBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public MatterCompressorRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockRenderer =
                Minecraft.getInstance()
                        .getBlockRenderer();
    }

    @Override
    public void render(
            MatterCompressorBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack displayed =
                machine.getRenderedStack();

        BlockState state =
                machine.getBlockState();

        boolean singularity =
                state.hasProperty(
                        MatterCompressorBlock.SINGULARITY
                )
                        && state.getValue(
                        MatterCompressorBlock.SINGULARITY
                );

        if (singularity) {
            renderInternalBlackHole(
                    machine,
                    partialTick,
                    poseStack,
                    buffers
            );

            /*
             * 最终阶段仍显示投入的充能能量剑体，
             * 但隐藏作为锚点存在的人造奇点物品本身。
             */
            if (!displayed.isEmpty()) {
                renderWorkpiece(
                        machine,
                        displayed,
                        poseStack,
                        buffers,
                        LightTexture.FULL_BRIGHT,
                        0.56D,
                        0.48F
                );
            }

            return;
        }

        boolean working =
                state.hasProperty(
                        MatterCompressorBlock.WORKING
                )
                        && state.getValue(
                        MatterCompressorBlock.WORKING
                );

        float squeeze = 0.0F;

        if (working && machine.getLevel() != null) {
            float phase = (
                    machine.getLevel().getGameTime()
                            + partialTick
            ) % 20.0F / 20.0F;

            squeeze = 1.0F
                    - Math.abs(
                    phase * 2.0F - 1.0F
            );
        }

        renderPressPlate(
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                0.80F - squeeze * 0.22F
        );

        renderPressPlate(
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                0.20F + squeeze * 0.22F
        );

        if (!displayed.isEmpty()) {
            renderWorkpiece(
                    machine,
                    displayed,
                    poseStack,
                    buffers,
                    packedLight,
                    0.50D,
                    0.58F
            );
        }
    }

    private static void renderInternalBlackHole(
            MatterCompressorBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers
    ) {
        ItemStack singularity =
                new ItemStack(
                        CWitems.ARTIFICIAL_SINGULARITY.get()
                );

        float spin = machine.getLevel() == null
                ? 0.0F
                : (machine.getLevel().getGameTime()
                + partialTick) * 4.0F;

        poseStack.pushPose();
        poseStack.translate(
                0.5D,
                0.50D,
                0.5D
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(spin)
        );
        poseStack.scale(
                0.82F,
                0.82F,
                0.82F
        );

        /*
         * 三张互相交叉的纯黑贴片形成 MC 风格的近似球状黑洞，
         * 比单张平面物品在第三人称下更稳定。
         */
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            i * 60.0F
                    )
            );

            Minecraft.getInstance()
                    .getItemRenderer()
                    .renderStatic(
                            singularity,
                            ItemDisplayContext.FIXED,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            poseStack,
                            buffers,
                            machine.getLevel(),
                            (int) machine
                                    .getBlockPos()
                                    .asLong()
                                    + i
                    );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void renderWorkpiece(
            MatterCompressorBlockEntity machine,
            ItemStack displayed,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            double centerY,
            float scale
    ) {
        BlockState state =
                machine.getBlockState();

        poseStack.pushPose();
        poseStack.translate(
                0.5D,
                centerY,
                0.5D
        );

        if (state.hasProperty(
                MatterCompressorBlock.FACING
        )) {
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            -state.getValue(
                                    MatterCompressorBlock.FACING
                            ).toYRot()
                    )
            );
        }

        poseStack.translate(
                0.0D,
                0.0D,
                -0.08D
        );
        poseStack.scale(
                scale,
                scale,
                scale
        );

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        displayed,
                        ItemDisplayContext.GROUND,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        buffers,
                        machine.getLevel(),
                        (int) machine
                                .getBlockPos()
                                .asLong()
                );

        poseStack.popPose();
    }

    private void renderPressPlate(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            float centerY
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.5D,
                centerY,
                0.5D
        );

        poseStack.scale(
                0.68F,
                0.10F,
                0.68F
        );

        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        blockRenderer.renderSingleBlock(
                Blocks.IRON_BLOCK
                        .defaultBlockState(),
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }
}
