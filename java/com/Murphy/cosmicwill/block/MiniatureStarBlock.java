package com.Murphy.cosmicwill.block;

import com.Murphy.cosmicwill.blockentity.CompressedFurnaceBlockEntity;
import com.Murphy.cosmicwill.blockentity.MiniatureStarBlockEntity;
import com.Murphy.cosmicwill.damage.CWDamageTypes;
import com.Murphy.cosmicwill.event.MiniatureStarInfluence;
import com.Murphy.cosmicwill.registry.CWBlocks;
import com.Murphy.cosmicwill.singularity.SingularityCatastrophe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class MiniatureStarBlock extends BaseEntityBlock {

    private static final int INNER_RADIUS = 1;
    private static final int BASE_RADIUS = 3;

    /*
     * 外层传播波会在两个半径之间循环
     * 第一次破坏 9×9 外壳，下一次继续扩张到 11×11
     */
    private static final int FIRST_WAVE_RADIUS = 4;
    private static final int OUTER_RADIUS = 5;

    /*
     * 约 20×20 的亡灵燃烧区。使用半径 10 的 Chebyshev 范围
     * 实际表现为以恒星为中心、边长约 21 格的立方区域。
     */
    private static final int UNDEAD_BURN_RADIUS = 10;

    private static final int BASE_TICK_INTERVAL = 10;

    private static final int OUTER_DELAY_MIN = 20;
    private static final int OUTER_DELAY_RANDOM = 21;

    private static final float BASE_DAMAGE = 4.0F;
    private static final float INNER_DAMAGE =
            BASE_DAMAGE * 3.0F;

    private static final float OBSIDIAN_HARDNESS = 50.0F;

    private static final Map<
            ServerLevel,
            Map<Long, Long>
            > NEXT_OUTER_WAVE =
            new WeakHashMap<>();

    private static final Map<
            ServerLevel,
            Map<Long, Integer>
            > NEXT_WAVE_RADIUS =
            new WeakHashMap<>();

    public MiniatureStarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new MiniatureStarBlockEntity(
                pos,
                state
        );
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        if (level instanceof ServerLevel serverLevel) {
            MiniatureStarInfluence.markActive(
                    serverLevel,
                    pos
            );

            setNextWaveRadius(
                    serverLevel,
                    pos,
                    FIRST_WAVE_RADIUS
            );

            scheduleNextOuterWave(
                    serverLevel,
                    pos,
                    serverLevel.getGameTime(),
                    serverLevel.random
            );

            level.scheduleTick(
                    pos,
                    this,
                    1
            );
        }
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())
                && level instanceof ServerLevel serverLevel) {
            clearOuterWave(
                    serverLevel,
                    pos
            );

            MiniatureStarInfluence.remove(
                    serverLevel,
                    pos
            );
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }

    @Override
    public void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        MiniatureStarInfluence.markActive(
                level,
                pos
        );

        /*
         * 两颗微型恒星只在中心间距落入 3×3×3 时失稳。
         * 处于更外层 7×7 或 9×9 范围时，它们会互相忽略。
         */
        if (hasCollidingMiniatureStar(
                level,
                pos
        )) {
            SingularityCatastrophe.trigger(
                    level,
                    pos
            );
            return;
        }

        /*
         * 9×9×9 作用范围内的压缩熔炉获得持续恒星热源。
         * 核心 3×3×3 中的熔炉随后仍会被绝对毁灭。
         */
        provideHeatToCompressedFurnaces(
                level,
                pos
        );

        annihilateInnerBlocks(
                level,
                pos
        );

        transformBaseBlocks(
                level,
                pos,
                INNER_RADIUS,
                BASE_RADIUS
        );

        processItems(
                level,
                pos,
                false,
                0
        );

        processLivingEntities(
                level,
                pos,
                false,
                0
        );

        /*
         * 20×20 辐射区：亡灵即使处于地形毁灭范围之外也会燃烧。
         */
        burnUndeadInRadiationZone(
                level,
                pos
        );

        int waveRadius =
                pollOuterWaveRadius(
                        level,
                        pos,
                        random
                );

        if (waveRadius > 0) {
            /*
             * 每次光圈出现只破坏当前这一层：
             * 先半径 4（9×9），下一次半径 5（11×11）。
             */
            transformBaseBlocks(
                    level,
                    pos,
                    waveRadius - 1,
                    waveRadius
            );

            processItems(
                    level,
                    pos,
                    true,
                    waveRadius
            );

            processLivingEntities(
                    level,
                    pos,
                    true,
                    waveRadius
            );

            emitOuterWaveParticles(
                    level,
                    pos,
                    random,
                    waveRadius
            );
        }

        level.scheduleTick(
                pos,
                this,
                BASE_TICK_INTERVAL
        );
    }

    @Override
    public void animateTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random
    ) {
        for (int i = 0; i < 3; i++) {
            level.addParticle(
                    ParticleTypes.FLAME,
                    pos.getX() + 0.2D
                            + random.nextDouble() * 0.6D,
                    pos.getY() + 0.2D
                            + random.nextDouble() * 0.6D,
                    pos.getZ() + 0.2D
                            + random.nextDouble() * 0.6D,
                    0.0D,
                    0.01D,
                    0.0D
            );
        }
    }

    private static void provideHeatToCompressedFurnaces(
            ServerLevel level,
            BlockPos center
    ) {
        BlockPos.MutableBlockPos target =
                new BlockPos.MutableBlockPos();

        for (int dx = -OUTER_RADIUS;
             dx <= OUTER_RADIUS;
             dx++) {
            for (int dy = -OUTER_RADIUS;
                 dy <= OUTER_RADIUS;
                 dy++) {
                for (int dz = -OUTER_RADIUS;
                     dz <= OUTER_RADIUS;
                     dz++) {
                    target.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (level.getBlockEntity(target)
                            instanceof CompressedFurnaceBlockEntity furnace) {
                        furnace.provideStellarHeat(
                                BASE_TICK_INTERVAL * 2
                        );
                    }
                }
            }
        }
    }

    private static boolean hasCollidingMiniatureStar(
            ServerLevel level,
            BlockPos center
    ) {
        for (int dx = -INNER_RADIUS;
             dx <= INNER_RADIUS;
             dx++) {
            for (int dy = -INNER_RADIUS;
                 dy <= INNER_RADIUS;
                 dy++) {
                for (int dz = -INNER_RADIUS;
                     dz <= INNER_RADIUS;
                     dz++) {
                    if (dx == 0
                            && dy == 0
                            && dz == 0) {
                        continue;
                    }

                    if (level.getBlockState(
                            center.offset(dx, dy, dz)
                    ).is(
                            CWBlocks.MINIATURE_STAR.get()
                    )) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void annihilateInnerBlocks(
            ServerLevel level,
            BlockPos center
    ) {
        BlockPos.MutableBlockPos target =
                new BlockPos.MutableBlockPos();

        for (int dx = -INNER_RADIUS;
             dx <= INNER_RADIUS;
             dx++) {
            for (int dy = -INNER_RADIUS;
                 dy <= INNER_RADIUS;
                 dy++) {
                for (int dz = -INNER_RADIUS;
                     dz <= INNER_RADIUS;
                     dz++) {
                    if (dx == 0
                            && dy == 0
                            && dz == 0) {
                        continue;
                    }

                    target.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    BlockState nearby =
                            level.getBlockState(target);

                    if (nearby.is(
                            CWBlocks.MINIATURE_STAR.get()
                    )) {
                        continue;
                    }

                    if (!level.isInWorldBounds(target)
                            || nearby.isAir()) {
                        continue;
                    }

                    level.removeBlockEntity(target);

                    level.setBlock(
                            target,
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL
                                    | Block.UPDATE_SUPPRESS_DROPS
                    );
                }
            }
        }
    }

    private static void transformBaseBlocks(
            ServerLevel level,
            BlockPos center,
            int innerExclusive,
            int outerInclusive
    ) {
        BlockPos.MutableBlockPos target =
                new BlockPos.MutableBlockPos();

        for (int dx = -outerInclusive;
             dx <= outerInclusive;
             dx++) {
            for (int dy = -outerInclusive;
                 dy <= outerInclusive;
                 dy++) {
                for (int dz = -outerInclusive;
                     dz <= outerInclusive;
                     dz++) {
                    int shell = chebyshevDistance(
                            dx,
                            dy,
                            dz
                    );

                    if (shell <= innerExclusive
                            || shell > outerInclusive) {
                        continue;
                    }

                    target.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    applyBaseBlockEffect(
                            level,
                            target
                    );
                }
            }
        }
    }

    private static void applyBaseBlockEffect(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.isInWorldBounds(pos)) {
            return;
        }

        BlockState nearby =
                level.getBlockState(pos);

        /*
         * 微型恒星之间在 3×3 核心以外互相免疫。
         */
        if (nearby.is(
                CWBlocks.MINIATURE_STAR.get()
        )) {
            return;
        }

        /*
         * 水源和流动水都使用同一个 WATER 方块，
         * 因此 7×7 层立即蒸发，9×9 层随外圈延迟传播蒸发。
         */
        if (nearby.is(Blocks.WATER)) {
            evaporateWater(
                    level,
                    pos
            );
            return;
        }

        if (nearby.isAir()
                || !nearby.getFluidState().isEmpty()) {
            return;
        }

        if (nearby.hasBlockEntity()
                || level.getBlockEntity(pos) != null) {
            return;
        }

        if (nearby.is(
                Blocks.CRYING_OBSIDIAN
        )) {
            evaporateCryingObsidian(
                    level,
                    pos
            );
            return;
        }

        float hardness =
                nearby.getDestroySpeed(
                        level,
                        pos
                );

        if (hardness < 0.0F
                || hardness
                >= OBSIDIAN_HARDNESS) {
            return;
        }

        if (isSoftMatter(
                nearby,
                hardness
        )) {
            level.setBlock(
                    pos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
                            | Block.UPDATE_SUPPRESS_DROPS
            );
        } else {
            level.setBlock(
                    pos,
                    Blocks.LAVA.defaultBlockState(),
                    Block.UPDATE_ALL
                            | Block.UPDATE_SUPPRESS_DROPS
            );
        }
    }

    private static boolean isSoftMatter(
            BlockState state,
            float hardness
    ) {
        if (hardness < 1.5F) {
            return true;
        }

        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_BUTTONS)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.WOODEN_PRESSURE_PLATES)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.RED_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM);
    }

    private static void evaporateWater(
            ServerLevel level,
            BlockPos pos
    ) {
        level.setBlock(
                pos,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
                        | Block.UPDATE_SUPPRESS_DROPS
        );

        level.playSound(
                null,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.7F,
                1.5F
        );

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                10,
                0.35D,
                0.35D,
                0.35D,
                0.02D
        );
    }

    private static void evaporateCryingObsidian(
            ServerLevel level,
            BlockPos pos
    ) {
        level.sendParticles(
                ParticleTypes.CLOUD,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                18,
                0.35D,
                0.35D,
                0.35D,
                0.025D
        );

        level.playSound(
                null,
                pos,
                SoundEvents.LAVA_EXTINGUISH,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );

        level.setBlock(
                pos,
                Blocks.OBSIDIAN.defaultBlockState(),
                Block.UPDATE_ALL
        );
    }

    private static void processItems(
            ServerLevel level,
            BlockPos center,
            boolean propagationOnly,
            int propagationRadius
    ) {
        AABB area = new AABB(center)
                .inflate(OUTER_RADIUS + 0.5D);

        for (ItemEntity itemEntity
                : level.getEntitiesOfClass(
                        ItemEntity.class,
                        area
                )) {
            int shell = entityShell(
                    itemEntity.getX(),
                    itemEntity.getY(),
                    itemEntity.getZ(),
                    center
            );

            if (shell <= INNER_RADIUS) {
                if (!propagationOnly) {
                    itemEntity.discard();
                }
                continue;
            }

            boolean inBaseLayer =
                    shell <= BASE_RADIUS;

            boolean inPropagationLayer =
                    shell == propagationRadius;

            if ((!propagationOnly && !inBaseLayer)
                    || (propagationOnly
                    && !inPropagationLayer)) {
                continue;
            }

            if (itemEntity
                    .getItem()
                    .getItem()
                    .isFireResistant()) {
                continue;
            }

            itemEntity.discard();
        }
    }

    private static void processLivingEntities(
            ServerLevel level,
            BlockPos center,
            boolean propagationOnly,
            int propagationRadius
    ) {
        AABB area = new AABB(center)
                .inflate(OUTER_RADIUS + 0.5D);

        for (LivingEntity living
                : level.getEntitiesOfClass(
                        LivingEntity.class,
                        area
                )) {
            int shell = entityShell(
                    living.getX(),
                    living.getY(),
                    living.getZ(),
                    center
            );

            if (shell <= INNER_RADIUS) {
                if (!propagationOnly) {
                    living.setSecondsOnFire(8);

                    living.hurt(
                            CWDamageTypes
                                    .miniatureStar(level),
                            INNER_DAMAGE
                    );
                }
                continue;
            }

            boolean inBaseLayer =
                    shell <= BASE_RADIUS;

            boolean inPropagationLayer =
                    shell == propagationRadius;

            if ((!propagationOnly && !inBaseLayer)
                    || (propagationOnly
                    && !inPropagationLayer)) {
                continue;
            }

            living.setSecondsOnFire(5);

            living.hurt(
                    CWDamageTypes.miniatureStar(level),
                    BASE_DAMAGE
            );
        }
    }

    private static void burnUndeadInRadiationZone(
            ServerLevel level,
            BlockPos center
    ) {
        AABB area =
                new AABB(center)
                        .inflate(
                                UNDEAD_BURN_RADIUS
                                        + 0.5D
                        );

        for (LivingEntity living
                : level.getEntitiesOfClass(
                        LivingEntity.class,
                        area
                )) {
            if (living.getMobType()
                    != MobType.UNDEAD) {
                continue;
            }

            int shell =
                    entityShell(
                            living.getX(),
                            living.getY(),
                            living.getZ(),
                            center
                    );

            if (shell > UNDEAD_BURN_RADIUS) {
                continue;
            }

            living.setSecondsOnFire(5);
        }
    }

    /**
     * 同时生成 XZ、XY、YZ 三个方向的火焰光圈。
     */
    private static void emitOuterWaveParticles(
            ServerLevel level,
            BlockPos center,
            RandomSource random,
            int waveRadius
    ) {
        for (int plane = 0;
             plane < 3;
             plane++) {
            for (int i = 0;
                 i < 48;
                 i++) {
                double angle =
                        Math.PI * 2.0D
                                * i / 48.0D;

                double radius =
                        waveRadius
                                + random.nextDouble()
                                * 0.35D;

                double a =
                        Math.cos(angle) * radius;

                double b =
                        Math.sin(angle) * radius;

                double x =
                        center.getX() + 0.5D;

                double y =
                        center.getY() + 0.5D;

                double z =
                        center.getZ() + 0.5D;

                if (plane == 0) {
                    x += a;
                    z += b;
                } else if (plane == 1) {
                    x += a;
                    y += b;
                } else {
                    y += a;
                    z += b;
                }

                level.sendParticles(
                        ParticleTypes.FLAME,
                        x,
                        y,
                        z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D
                );

                if (i % 6 == 0) {
                    level.sendParticles(
                            ParticleTypes.SMOKE,
                            x,
                            y,
                            z,
                            1,
                            0.0D,
                            0.0D,
                            0.0D,
                            0.0D
                    );
                }
            }
        }
    }

    private static int entityShell(
            double x,
            double y,
            double z,
            BlockPos center
    ) {
        int dx = Math.abs(
                Mth.floor(x)
                        - center.getX()
        );

        int dy = Math.abs(
                Mth.floor(y)
                        - center.getY()
        );

        int dz = Math.abs(
                Mth.floor(z)
                        - center.getZ()
        );

        return Math.max(
                dx,
                Math.max(dy, dz)
        );
    }

    private static int chebyshevDistance(
            int x,
            int y,
            int z
    ) {
        return Math.max(
                Math.abs(x),
                Math.max(
                        Math.abs(y),
                        Math.abs(z)
                )
        );
    }

    /**
     * @return 0 表示本次不传播；4 表示 9×9 光圈；5 表示 11×11 光圈。
     */
    private static int pollOuterWaveRadius(
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        Map<Long, Long> levelTimes =
                NEXT_OUTER_WAVE.computeIfAbsent(
                        level,
                        ignored -> new HashMap<>()
                );

        long key = pos.asLong();
        long now = level.getGameTime();

        Long due =
                levelTimes.get(key);

        if (due == null) {
            setNextWaveRadius(
                    level,
                    pos,
                    FIRST_WAVE_RADIUS
            );

            scheduleNextOuterWave(
                    level,
                    pos,
                    now,
                    random
            );

            return 0;
        }

        if (now < due) {
            return 0;
        }

        int radius =
                NEXT_WAVE_RADIUS
                        .computeIfAbsent(
                                level,
                                ignored -> new HashMap<>()
                        )
                        .getOrDefault(
                                key,
                                FIRST_WAVE_RADIUS
                        );

        int nextRadius =
                radius >= OUTER_RADIUS
                        ? FIRST_WAVE_RADIUS
                        : radius + 1;

        setNextWaveRadius(
                level,
                pos,
                nextRadius
        );

        scheduleNextOuterWave(
                level,
                pos,
                now,
                random
        );

        return radius;
    }

    private static void setNextWaveRadius(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        NEXT_WAVE_RADIUS
                .computeIfAbsent(
                        level,
                        ignored -> new HashMap<>()
                )
                .put(
                        pos.asLong(),
                        radius
                );
    }

    private static void scheduleNextOuterWave(
            ServerLevel level,
            BlockPos pos,
            long now,
            RandomSource random
    ) {
        NEXT_OUTER_WAVE
                .computeIfAbsent(
                        level,
                        ignored -> new HashMap<>()
                )
                .put(
                        pos.asLong(),
                        now
                                + OUTER_DELAY_MIN
                                + random.nextInt(
                                OUTER_DELAY_RANDOM
                        )
                );
    }

    private static void clearOuterWave(
            ServerLevel level,
            BlockPos pos
    ) {
        Map<Long, Long> levelTimes =
                NEXT_OUTER_WAVE.get(level);

        if (levelTimes == null) {
            return;
        }

        levelTimes.remove(pos.asLong());

        if (levelTimes.isEmpty()) {
            NEXT_OUTER_WAVE.remove(level);
        }

        Map<Long, Integer> levelRadii =
                NEXT_WAVE_RADIUS.get(level);

        if (levelRadii == null) {
            return;
        }

        levelRadii.remove(pos.asLong());

        if (levelRadii.isEmpty()) {
            NEXT_WAVE_RADIUS.remove(level);
        }
    }
}
