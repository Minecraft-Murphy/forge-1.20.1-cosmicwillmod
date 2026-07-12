package com.Murphy.cosmicwill.event;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.registry.CWBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MiniatureStarGazeEvents {

    private static final int CHECK_INTERVAL = 5;
    private static final int REQUIRED_GAZE_TICKS = 60;

    /*
     * “近距离凝视”的最大距离。
     * 屏幕白雾可以在更远处出现，但失明只在八格以内累计。
     */
    private static final int MAX_GAZE_DISTANCE = 8;
    private static final double MAX_GAZE_DISTANCE_SQR =
            MAX_GAZE_DISTANCE * MAX_GAZE_DISTANCE;

    /*
     * 约 15° 的凝视锥，不会因为恒星仅仅处于屏幕边缘就累计。
     */
    private static final double REQUIRED_ALIGNMENT = 0.965D;

    private static final Map<UUID, Integer> GAZE_TICKS =
            new HashMap<>();

    private MiniatureStarGazeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player
                instanceof ServerPlayer player)
                || player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        UUID playerId = player.getUUID();

        if (!isStaringAtMiniatureStar(player)) {
            GAZE_TICKS.remove(playerId);
            return;
        }

        int accumulated =
                Math.min(
                        REQUIRED_GAZE_TICKS + 20,
                        GAZE_TICKS.getOrDefault(
                                playerId,
                                0
                        ) + CHECK_INTERVAL
                );

        GAZE_TICKS.put(
                playerId,
                accumulated
        );

        if (accumulated < REQUIRED_GAZE_TICKS) {
            return;
        }

        /*
         * 持续凝视时每五 Tick 刷新一次。
         * 移开视线后仍保留约 2.5 秒的短暂失明。
         */
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.BLINDNESS,
                        50,
                        0,
                        false,
                        false,
                        true
                )
        );
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        GAZE_TICKS.remove(
                event.getEntity().getUUID()
        );
    }

    private static boolean isStaringAtMiniatureStar(
            ServerPlayer player
    ) {
        if (!(player.level()
                instanceof ServerLevel level)) {
            return false;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look =
                player.getViewVector(1.0F)
                        .normalize();

        BlockPos origin =
                BlockPos.containing(eye);

        BlockPos min =
                origin.offset(
                        -MAX_GAZE_DISTANCE,
                        -MAX_GAZE_DISTANCE,
                        -MAX_GAZE_DISTANCE
                );

        BlockPos max =
                origin.offset(
                        MAX_GAZE_DISTANCE,
                        MAX_GAZE_DISTANCE,
                        MAX_GAZE_DISTANCE
                );

        for (BlockPos pos
                : BlockPos.betweenClosed(min, max)) {
            if (!level.getBlockState(pos)
                    .is(CWBlocks.MINIATURE_STAR.get())) {
                continue;
            }

            Vec3 center =
                    Vec3.atCenterOf(pos);

            Vec3 direction =
                    center.subtract(eye);

            double distanceSqr =
                    direction.lengthSqr();

            if (distanceSqr > MAX_GAZE_DISTANCE_SQR
                    || distanceSqr < 0.0001D) {
                continue;
            }

            double alignment =
                    look.dot(
                            direction.normalize()
                    );

            if (alignment < REQUIRED_ALIGNMENT) {
                continue;
            }

            HitResult hit =
                    level.clip(
                            new ClipContext(
                                    eye,
                                    center,
                                    ClipContext.Block.VISUAL,
                                    ClipContext.Fluid.NONE,
                                    player
                            )
                    );

            if (hit.getType()
                    == HitResult.Type.MISS) {
                return true;
            }

            if (hit instanceof BlockHitResult blockHit
                    && blockHit.getBlockPos().equals(pos)) {
                return true;
            }
        }

        return false;
    }
}
