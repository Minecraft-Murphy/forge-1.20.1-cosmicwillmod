package com.Murphy.cosmicwill.client;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.items.NullifierSwordItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Objects;

public final class NullifierPhotonRenderer {

    private static final ResourceLocation PHOTON_TEXTURE =
            Objects.requireNonNull(
                    ResourceLocation.tryBuild(
                            CustomWill.MODID,
                            "textures/misc/nullifier_photon.png"
                    )
            );

    private static final RenderType PHOTON_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(PHOTON_TEXTURE);

    private static final float BLADE_START_X = 28.0F / 64.0F;
    private static final float BLADE_START_Y = 1.0F - 34.0F / 64.0F;
    private static final float BLADE_END_X = 62.5F / 64.0F;
    private static final float BLADE_END_Y = 1.0F - 1.5F / 64.0F;

    private static final int ANCHOR_COUNT = 12;

    private static final float MAX_RADIUS = 0.155F;
    private static final float CONTACT_RADIUS = 0.014F;

    private static final float PHOTON_LENGTH = 0.062F;
    private static final float PHOTON_WIDTH = 0.020F;

    private static final float WAVE_PERIOD_TICKS = 15.0F;
    private static final float MAX_PHASE_STAGGER = 0.32F;

    /** 上端顺时针补偿，抵消持剑姿态造成的视觉误差。 */
    private static final float TOP_ANGLE_COMPENSATION_DEGREES = -20.0F;
    private static final float TOP_COMPENSATION_START = 0.68F;

    /**
     * 直接删掉靠近护手的底部锚点。
     * 12 个锚点时，这个值会删掉前三个锚点，让最底下那几条彻底消失。
     */
    private static final float BOTTOM_CULL_END = 0.26F;

    /**
     * 从底部开始，到这个位置为止逐渐解除“压平”。
     * 也就是说底部会更水平，中部恢复原始斜率。
     */
    private static final float LOWER_FLATTEN_END = 0.46F;

    /**
     * 最底部斜率压平到原来的 45%。
     * 原来底部 k≈1，现在会被压到 k≈0.45，更缓。
     */
    private static final float LOWER_SLOPE_FACTOR_MIN = 0.45F;

    private NullifierPhotonRenderer() {
    }

    public static boolean shouldRender(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    public static void render(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffers
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        float partialTick = minecraft.getFrameTime();
        float time = level.getGameTime() + partialTick;

        float collapse = getCollapseAmount(stack, level);
        float signedDirection = 1.0F - collapse * 2.0F;
        boolean inward = signedDirection < 0.0F;

        float directionStrength = Math.abs(signedDirection);
        float activeRadius = MAX_RADIUS * Mth.lerp(
                directionStrength,
                0.32F,
                1.0F
        );

        float basePhase = fraction(time / WAVE_PERIOD_TICKS);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer consumer = buffers.getBuffer(PHOTON_RENDER_TYPE);

        PhotonColor color = PhotonColor.interpolate(collapse);

        for (int anchor = 0; anchor < ANCHOR_COUNT; anchor++) {
            float t = (anchor + 0.5F) / ANCHOR_COUNT;

            /*
             * 直接裁掉护手附近粒子。
             */
            if (t < BOTTOM_CULL_END) {
                continue;
            }

            float targetX = Mth.lerp(t, BLADE_START_X, BLADE_END_X);
            float targetY = Mth.lerp(t, BLADE_START_Y, BLADE_END_Y);

            /*
             * 基础规律：
             * 下部：/
             * 中部：—
             * 上部：\
             */
            float slopeK = 1.0F - 2.0F * t;

            /*
             * 底部压平：
             * t 越靠近 BOTTOM_CULL_END，斜率越缓；
             * 到 LOWER_FLATTEN_END 后恢复原样。
             */
            float lowerFlattenBlend = smoothStep(
                    Mth.clamp(
                            (LOWER_FLATTEN_END - t)
                                    / (LOWER_FLATTEN_END - BOTTOM_CULL_END),
                            0.0F,
                            1.0F
                    )
            );

            float lowerSlopeFactor = Mth.lerp(
                    lowerFlattenBlend,
                    1.0F,
                    LOWER_SLOPE_FACTOR_MIN
            );

            float effectiveSlopeK = slopeK * lowerSlopeFactor;

            /*
             * 上端仍然补偿 20°。
             */
            float topBlend = smoothStep(
                    Mth.clamp(
                            (t - TOP_COMPENSATION_START)
                                    / (1.0F - TOP_COMPENSATION_START),
                            0.0F,
                            1.0F
                    )
            );

            float topCompensationRadians =
                    TOP_ANGLE_COMPENSATION_DEGREES
                            * Mth.DEG_TO_RAD
                            * topBlend;

            float cos = Mth.cos(topCompensationRadians);
            float sin = Mth.sin(topCompensationRadians);

            for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
                float side = sideIndex == 0 ? -1.0F : 1.0F;
                int laneIndex = anchor * 2 + sideIndex;

                float radialX = side;
                float radialY = side * effectiveSlopeK;

                /*
                 * 只旋转上端。
                 */
                float rotatedX = radialX * cos - radialY * sin;
                float rotatedY = radialX * sin + radialY * cos;

                radialX = rotatedX;
                radialY = rotatedY;

                float invLen = Mth.invSqrt(
                        radialX * radialX + radialY * radialY
                );
                radialX *= invLen;
                radialY *= invLen;

                float phaseOffset =
                        signedHash(laneIndex, 11.7F)
                                * MAX_PHASE_STAGGER;

                float speedMul =
                        0.82F
                                + 0.38F
                                * hash01(laneIndex, 23.4F);

                float localPhase = fraction(
                        basePhase * speedMul + phaseOffset
                );
                float travel = smoothStep(localPhase);

                float radiusScale =
                        0.86F
                                + 0.22F
                                * hash01(laneIndex, 37.1F);

                float laneRadius = activeRadius * radiusScale;

                float distance = inward
                        ? Mth.lerp(travel, laneRadius, CONTACT_RADIUS)
                        : Mth.lerp(travel, CONTACT_RADIUS, laneRadius);

                float fadeIn = smoothStep(
                        Mth.clamp(
                                localPhase / 0.18F,
                                0.0F,
                                1.0F
                        )
                );

                float fadeOut = smoothStep(
                        Mth.clamp(
                                (1.0F - localPhase) / 0.24F,
                                0.0F,
                                1.0F
                        )
                );

                float phaseFade = fadeIn * fadeOut;

                float contactFade = inward
                        ? smoothStep(
                                Mth.clamp(
                                        (distance - CONTACT_RADIUS)
                                                / 0.045F,
                                        0.0F,
                                        1.0F
                                )
                        )
                        : 1.0F;

                float outwardFade = inward
                        ? 1.0F
                        : 1.0F - 0.58F * travel;

                float lifeAlpha =
                        0.96F
                                * phaseFade
                                * contactFade
                                * outwardFade;

                lifeAlpha *= Mth.lerp(
                        directionStrength,
                        0.55F,
                        1.0F
                );

                float centerX = targetX + radialX * distance;
                float centerY = targetY + radialY * distance;

                float lengthScale =
                        0.86F
                                + 0.24F
                                * hash01(laneIndex, 51.8F);

                float widthScale =
                        0.88F
                                + 0.16F
                                * hash01(laneIndex, 66.2F);

                float localLength = PHOTON_LENGTH * lengthScale;
                float localWidth = PHOTON_WIDTH * widthScale;

                drawRectangle(
                        consumer,
                        matrix,
                        normal,
                        centerX,
                        centerY,
                        0.548F,
                        radialX,
                        radialY,
                        localLength * 1.30F,
                        localWidth * 1.55F,
                        color.glowR(),
                        color.glowG(),
                        color.glowB(),
                        lifeAlpha * color.glowAlpha()
                );

                drawRectangle(
                        consumer,
                        matrix,
                        normal,
                        centerX,
                        centerY,
                        0.550F,
                        radialX,
                        radialY,
                        localLength,
                        localWidth,
                        color.coreR(),
                        color.coreG(),
                        color.coreB(),
                        lifeAlpha
                );
            }
        }
    }

    private static float getCollapseAmount(
            ItemStack stack,
            ClientLevel level
    ) {
        if (!NullifierSwordItem.isTransitioning(stack, level)) {
            return NullifierSwordItem.getMode(stack)
                    == NullifierSwordItem.MODE_NULLIFICATION
                    ? 1.0F
                    : 0.0F;
        }

        float progress = smoothStep(
                NullifierSwordItem.getTransitionProgress(
                        stack,
                        level
                )
        );

        int previous =
                NullifierSwordItem.getPreviousMode(stack);
        int current =
                NullifierSwordItem.getMode(stack);

        if (previous
                == NullifierSwordItem.MODE_DESTRUCTION
                && current
                == NullifierSwordItem.MODE_NULLIFICATION) {
            return progress;
        }

        return 1.0F - progress;
    }

    private static void drawRectangle(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            float centerX,
            float centerY,
            float z,
            float directionX,
            float directionY,
            float length,
            float width,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float halfLength = length * 0.5F;
        float halfWidth = width * 0.5F;

        float sideX = -directionY;
        float sideY = directionX;

        float ax = centerX - directionX * halfLength - sideX * halfWidth;
        float ay = centerY - directionY * halfLength - sideY * halfWidth;

        float bx = centerX + directionX * halfLength - sideX * halfWidth;
        float by = centerY + directionY * halfLength - sideY * halfWidth;

        float cx = centerX + directionX * halfLength + sideX * halfWidth;
        float cy = centerY + directionY * halfLength + sideY * halfWidth;

        float dx = centerX - directionX * halfLength + sideX * halfWidth;
        float dy = centerY - directionY * halfLength + sideY * halfWidth;

        vertex(consumer, matrix, normal, ax, ay, z, red, green, blue, alpha, 0.0F, 1.0F);
        vertex(consumer, matrix, normal, bx, by, z, red, green, blue, alpha, 1.0F, 1.0F);
        vertex(consumer, matrix, normal, cx, cy, z, red, green, blue, alpha, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, dx, dy, z, red, green, blue, alpha, 0.0F, 0.0F);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    private static float hash01(int index, float salt) {
        float value =
                Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F;
        return fraction(value);
    }

    private static float signedHash(int index, float salt) {
        return hash01(index, salt) * 2.0F - 1.0F;
    }

    private static float fraction(float value) {
        return value - Mth.floor(value);
    }

    private static float smoothStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private record PhotonColor(
            float coreR,
            float coreG,
            float coreB,
            float glowR,
            float glowG,
            float glowB,
            float glowAlpha
    ) {
        private static PhotonColor interpolate(float collapse) {
            float destructionCoreR = 1.00F;
            float destructionCoreG = 1.00F;
            float destructionCoreB = 1.00F;

            float destructionGlowR = 0.48F;
            float destructionGlowG = 0.82F;
            float destructionGlowB = 1.00F;

            float nullCoreR = 0.010F;
            float nullCoreG = 0.001F;
            float nullCoreB = 0.001F;

            float nullGlowR = 0.22F;
            float nullGlowG = 0.008F;
            float nullGlowB = 0.008F;

            return new PhotonColor(
                    Mth.lerp(collapse, destructionCoreR, nullCoreR),
                    Mth.lerp(collapse, destructionCoreG, nullCoreG),
                    Mth.lerp(collapse, destructionCoreB, nullCoreB),
                    Mth.lerp(collapse, destructionGlowR, nullGlowR),
                    Mth.lerp(collapse, destructionGlowG, nullGlowG),
                    Mth.lerp(collapse, destructionGlowB, nullGlowB),
                    Mth.lerp(collapse, 0.38F, 0.58F)
            );
        }
    }
}