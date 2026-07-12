package com.Murphy.cosmicwill.client;

import net.minecraft.util.Mth;

/**
 * 世界渲染阶段记录本帧视野内最强的微型恒星眩光，
 * HUD 阶段将其平滑追踪。
 *
 * 视线移开后不再立即归零，而是以较慢速度衰减，
 * 模拟人眼从强光中恢复的残留曝光。
 */
public final class MiniatureStarGlareState {

    private static float pendingIntensity;
    private static float displayedIntensity;

    private MiniatureStarGlareState() {
    }

    public static void record(float intensity) {
        pendingIntensity = Math.max(
                pendingIntensity,
                Mth.clamp(
                        intensity,
                        0.0F,
                        1.0F
                )
        );
    }

    /**
     * @param deltaTicks 客户端上一帧经过的游戏 Tick 数
     */
    public static float updateAndGet(
            float deltaTicks
    ) {
        float target = pendingIntensity;
        pendingIntensity = 0.0F;

        float safeDelta =
                Mth.clamp(
                        deltaTicks,
                        0.05F,
                        2.0F
                );

        /*
         * 看见恒星时约 0.15～0.25 秒建立曝光；
         * 移开后约 1.5～2.5 秒逐渐恢复。
         */
        float baseRate =
                target > displayedIntensity
                        ? 0.34F
                        : 0.045F;

        float frameRate =
                1.0F
                        - (float) Math.pow(
                        1.0F - baseRate,
                        safeDelta
                );

        displayedIntensity =
                Mth.lerp(
                        frameRate,
                        displayedIntensity,
                        target
                );

        if (target <= 0.0F
                && displayedIntensity < 0.0025F) {
            displayedIntensity = 0.0F;
        }

        return displayedIntensity;
    }

    public static void clear() {
        pendingIntensity = 0.0F;
        displayedIntensity = 0.0F;
    }
}
