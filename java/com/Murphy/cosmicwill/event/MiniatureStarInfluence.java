package com.Murphy.cosmicwill.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录当前已加载并持续运行的微型恒星。
 *
 * 使用最近心跳时间而不是每次怪物生成都扫描 15×15×15 方块，
 * 避免大量自然生成尝试造成不必要的方块查询。
 */
public final class MiniatureStarInfluence {

    private static final long STALE_AFTER_TICKS = 40L;

    private static final Map<
            ServerLevel,
            Map<Long, Long>
            > ACTIVE_STARS =
            new WeakHashMap<>();

    private MiniatureStarInfluence() {
    }

    public static synchronized void markActive(
            ServerLevel level,
            BlockPos pos
    ) {
        ACTIVE_STARS
                .computeIfAbsent(
                        level,
                        ignored ->
                                new ConcurrentHashMap<>()
                )
                .put(
                        pos.asLong(),
                        level.getGameTime()
                );
    }

    public static synchronized void remove(
            ServerLevel level,
            BlockPos pos
    ) {
        Map<Long, Long> stars =
                ACTIVE_STARS.get(level);

        if (stars == null) {
            return;
        }

        stars.remove(pos.asLong());

        if (stars.isEmpty()) {
            ACTIVE_STARS.remove(level);
        }
    }

    public static synchronized boolean hasActiveStarWithin(
            ServerLevel level,
            BlockPos target,
            int radius
    ) {
        Map<Long, Long> stars =
                ACTIVE_STARS.get(level);

        if (stars == null
                || stars.isEmpty()) {
            return false;
        }

        long now = level.getGameTime();

        Iterator<Map.Entry<Long, Long>> iterator =
                stars.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry =
                    iterator.next();

            if (now - entry.getValue()
                    > STALE_AFTER_TICKS) {
                iterator.remove();
                continue;
            }

            BlockPos starPos =
                    BlockPos.of(
                            entry.getKey()
                    );

            int dx = Math.abs(
                    starPos.getX()
                            - target.getX()
            );

            int dy = Math.abs(
                    starPos.getY()
                            - target.getY()
            );

            int dz = Math.abs(
                    starPos.getZ()
                            - target.getZ()
            );

            if (Math.max(
                    dx,
                    Math.max(dy, dz)
            ) <= radius) {
                return true;
            }
        }

        if (stars.isEmpty()) {
            ACTIVE_STARS.remove(level);
        }

        return false;
    }

    public static synchronized void clear(
            ServerLevel level
    ) {
        ACTIVE_STARS.remove(level);
    }
}
