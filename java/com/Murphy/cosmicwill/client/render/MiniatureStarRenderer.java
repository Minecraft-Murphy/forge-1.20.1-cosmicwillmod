package com.Murphy.cosmicwill.client.render;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.blockentity.MiniatureStarBlockEntity;
import com.Murphy.cosmicwill.client.MiniatureStarGlareState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

public final class MiniatureStarRenderer
        implements BlockEntityRenderer<
        MiniatureStarBlockEntity> {

    private static final ResourceLocation HALO_TEXTURE =
            Objects.requireNonNull(
                    ResourceLocation.tryBuild(
                            CustomWill.MODID,
                            "textures/misc/miniature_star_halo.png"
                    )
            );

    private static final ResourceLocation FILM_TEXTURE =
            Objects.requireNonNull(
                    ResourceLocation.tryBuild(
                            CustomWill.MODID,
                            "textures/item/miniature_star_white_film.png"
                    )
            );

    private static final RenderType HALO_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(
                    HALO_TEXTURE
            );

    private static final RenderType FILM_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(
                    FILM_TEXTURE
            );

    public MiniatureStarRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            MiniatureStarBlockEntity star,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        Camera camera =
                minecraft.gameRenderer
                        .getMainCamera();

        Vec3 cameraPos =
                camera.getPosition();

        Vec3 starCenter =
                Vec3.atCenterOf(
                        star.getBlockPos()
                );

        Vec3 toStar =
                starCenter.subtract(cameraPos);

        double distance =
                toStar.length();

        if (distance > getViewDistance()) {
            return;
        }

        boolean visible =
                hasLineOfSight(
                        star,
                        cameraPos,
                        starCenter
                );

        if (visible) {
            Vec3 direction =
                    toStar.normalize();

            Vector3f lookDirection =
                    new Vector3f(
                            camera.getLookVector()
                    ).normalize();

            double alignment =
                    lookDirection.x()
                            * direction.x
                            + lookDirection.y()
                            * direction.y
                            + lookDirection.z()
                            * direction.z;

            MiniatureStarGlareState.record(
                    calculateScreenIntensity(
                            distance,
                            alignment
                    )
            );
        }

        /*
         * 世界中的膜比物品模型更浅：
         * 只在表面留一层很薄的全亮白色反光，不遮住太阳纹理。
         */
        renderSurfaceFilm(
                star,
                partialTick,
                poseStack,
                buffers,
                distance
        );

        renderHalo(
                star,
                partialTick,
                poseStack,
                buffers,
                camera,
                distance
        );
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static boolean hasLineOfSight(
            MiniatureStarBlockEntity star,
            Vec3 cameraPos,
            Vec3 starCenter
    ) {
        if (star.getLevel() == null) {
            return false;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        HitResult hit =
                star.getLevel().clip(
                        new ClipContext(
                                cameraPos,
                                starCenter,
                                ClipContext.Block.VISUAL,
                                ClipContext.Fluid.NONE,
                                minecraft.player
                        )
                );

        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        return hit
                instanceof BlockHitResult blockHit
                && blockHit.getBlockPos()
                .equals(star.getBlockPos());
    }

    private static float calculateScreenIntensity(
            double distance,
            double alignment
    ) {
        float proximity =
                1.0F - Mth.clamp(
                        ((float) distance - 0.65F)
                                / 31.35F,
                        0.0F,
                        1.0F
                );

        float centered =
                smoothStep(
                        Mth.clamp(
                                ((float) alignment - 0.10F)
                                        / 0.80F,
                                0.0F,
                                1.0F
                        )
                );

        float intensity =
                proximity
                        * (0.42F + centered * 0.58F);

        if (distance < 1.0D) {
            intensity = Math.max(
                    intensity,
                    0.98F
            );
        } else if (distance < 2.0D) {
            intensity = Math.max(
                    intensity,
                    0.88F
            );
        }

        return Mth.clamp(
                intensity,
                0.0F,
                1.0F
        );
    }

    private static void renderSurfaceFilm(
            MiniatureStarBlockEntity star,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            double distance
    ) {
        float time = star.getLevel() == null
                ? 0.0F
                : star.getLevel().getGameTime()
                + partialTick;

        float pulse =
                0.92F
                        + 0.08F
                        * Mth.sin(
                        time * 0.18F
                );

        float distanceFade =
                1.0F - Mth.clamp(
                        ((float) distance - 20.0F)
                                / 44.0F,
                        0.0F,
                        1.0F
                );

        float alpha =
                0.42F
                        * pulse
                        * distanceFade;

        VertexConsumer consumer =
                buffers.getBuffer(
                        FILM_RENDER_TYPE
                );

        PoseStack.Pose pose =
                poseStack.last();

        Matrix4f matrix =
                pose.pose();

        Matrix3f normal =
                pose.normal();

        float min = -0.006F;
        float max = 1.006F;

        // North
        filmVertex(consumer, matrix, normal, min, min, min, 0, 1, alpha, 0, 0, -1);
        filmVertex(consumer, matrix, normal, max, min, min, 1, 1, alpha, 0, 0, -1);
        filmVertex(consumer, matrix, normal, max, max, min, 1, 0, alpha, 0, 0, -1);
        filmVertex(consumer, matrix, normal, min, max, min, 0, 0, alpha, 0, 0, -1);

        // South
        filmVertex(consumer, matrix, normal, max, min, max, 0, 1, alpha, 0, 0, 1);
        filmVertex(consumer, matrix, normal, min, min, max, 1, 1, alpha, 0, 0, 1);
        filmVertex(consumer, matrix, normal, min, max, max, 1, 0, alpha, 0, 0, 1);
        filmVertex(consumer, matrix, normal, max, max, max, 0, 0, alpha, 0, 0, 1);

        // West
        filmVertex(consumer, matrix, normal, min, min, max, 0, 1, alpha, -1, 0, 0);
        filmVertex(consumer, matrix, normal, min, min, min, 1, 1, alpha, -1, 0, 0);
        filmVertex(consumer, matrix, normal, min, max, min, 1, 0, alpha, -1, 0, 0);
        filmVertex(consumer, matrix, normal, min, max, max, 0, 0, alpha, -1, 0, 0);

        // East
        filmVertex(consumer, matrix, normal, max, min, min, 0, 1, alpha, 1, 0, 0);
        filmVertex(consumer, matrix, normal, max, min, max, 1, 1, alpha, 1, 0, 0);
        filmVertex(consumer, matrix, normal, max, max, max, 1, 0, alpha, 1, 0, 0);
        filmVertex(consumer, matrix, normal, max, max, min, 0, 0, alpha, 1, 0, 0);

        // Up
        filmVertex(consumer, matrix, normal, min, max, min, 0, 1, alpha, 0, 1, 0);
        filmVertex(consumer, matrix, normal, max, max, min, 1, 1, alpha, 0, 1, 0);
        filmVertex(consumer, matrix, normal, max, max, max, 1, 0, alpha, 0, 1, 0);
        filmVertex(consumer, matrix, normal, min, max, max, 0, 0, alpha, 0, 1, 0);

        // Down
        filmVertex(consumer, matrix, normal, min, min, max, 0, 1, alpha, 0, -1, 0);
        filmVertex(consumer, matrix, normal, max, min, max, 1, 1, alpha, 0, -1, 0);
        filmVertex(consumer, matrix, normal, max, min, min, 1, 0, alpha, 0, -1, 0);
        filmVertex(consumer, matrix, normal, min, min, min, 0, 0, alpha, 0, -1, 0);
    }

    private static void renderHalo(
            MiniatureStarBlockEntity star,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            double distance
    ) {
        float time = star.getLevel() == null
                ? 0.0F
                : star.getLevel().getGameTime()
                + partialTick;

        float pulse =
                0.94F
                        + 0.06F
                        * Mth.sin(
                        time * 0.22F
                );

        float distanceFade =
                1.0F - Mth.clamp(
                        ((float) distance - 24.0F)
                                / 40.0F,
                        0.0F,
                        1.0F
                );

        VertexConsumer consumer =
                buffers.getBuffer(
                        HALO_RENDER_TYPE
                );

        poseStack.pushPose();
        poseStack.translate(
                0.5D,
                0.5D,
                0.5D
        );

        poseStack.mulPose(
                camera.rotation()
        );

        drawHaloLayer(
                poseStack,
                consumer,
                2.05F * pulse,
                0.52F * distanceFade
        );

        drawHaloLayer(
                poseStack,
                consumer,
                2.80F * pulse,
                0.27F * distanceFade
        );

        drawHaloLayer(
                poseStack,
                consumer,
                3.75F * pulse,
                0.13F * distanceFade
        );

        poseStack.popPose();
    }

    private static void drawHaloLayer(
            PoseStack poseStack,
            VertexConsumer consumer,
            float size,
            float alpha
    ) {
        PoseStack.Pose pose =
                poseStack.last();

        Matrix4f matrix =
                pose.pose();

        Matrix3f normal =
                pose.normal();

        float half = size * 0.5F;

        haloVertex(
                consumer,
                matrix,
                normal,
                -half,
                -half,
                0.0F,
                0.0F,
                1.0F,
                alpha
        );

        haloVertex(
                consumer,
                matrix,
                normal,
                half,
                -half,
                0.0F,
                1.0F,
                1.0F,
                alpha
        );

        haloVertex(
                consumer,
                matrix,
                normal,
                half,
                half,
                0.0F,
                1.0F,
                0.0F,
                alpha
        );

        haloVertex(
                consumer,
                matrix,
                normal,
                -half,
                half,
                0.0F,
                0.0F,
                0.0F,
                alpha
        );
    }

    private static void filmVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float u,
            float v,
            float alpha,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.vertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .color(
                        1.0F,
                        1.0F,
                        1.0F,
                        alpha
                )
                .uv(u, v)
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(
                        LightTexture.FULL_BRIGHT
                )
                .normal(
                        normal,
                        normalX,
                        normalY,
                        normalZ
                )
                .endVertex();
    }

    private static void haloVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float u,
            float v,
            float alpha
    ) {
        consumer.vertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .color(
                        1.0F,
                        1.0F,
                        1.0F,
                        alpha
                )
                .uv(u, v)
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(
                        LightTexture.FULL_BRIGHT
                )
                .normal(
                        normal,
                        0.0F,
                        0.0F,
                        1.0F
                )
                .endVertex();
    }

    private static float smoothStep(
            float value
    ) {
        value = Mth.clamp(
                value,
                0.0F,
                1.0F
        );

        return value
                * value
                * (3.0F - 2.0F * value);
    }
}
