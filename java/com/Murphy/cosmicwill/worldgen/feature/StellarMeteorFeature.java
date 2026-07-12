package com.Murphy.cosmicwill.worldgen.feature;

import com.Murphy.cosmicwill.registry.CWBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


public final class StellarMeteorFeature
        extends Feature<NoneFeatureConfiguration> {

    private static final BlockState OBSIDIAN =
            Blocks.OBSIDIAN.defaultBlockState();

    private static final BlockState CRYING_OBSIDIAN =
            Blocks.CRYING_OBSIDIAN.defaultBlockState();

    private static final BlockState AIR =
            Blocks.AIR.defaultBlockState();

    public StellarMeteorFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<
                    NoneFeatureConfiguration
                    > context
    ) {
        WorldGenLevel level =
                context.level();

        RandomSource random =
                context.random();

        BlockPos surface =
                context.origin();


        if (level.getBiome(surface)
                .is(Biomes.THE_END)) {
            return false;
        }

        BlockPos supportPos =
                surface.below();

        if (!isNaturalTerrain(
                level.getBlockState(supportPos)
        )) {
            return false;
        }

        int radius =
                random.nextFloat() < 0.22F
                        ? 5
                        : 4;

        BlockPos center =
                surface.below(radius - 1);

        if (!isNaturalTerrain(
                level.getBlockState(center)
        )) {
            return false;
        }

        BlockPos lowerSupport =
                center.below(
                        Math.max(
                                1,
                                radius - 2
                        )
                );

        if (!isNaturalTerrain(
                level.getBlockState(
                        lowerSupport
                )
        )) {
            return false;
        }

        carveShallowCrater(
                level,
                surface,
                radius,
                random
        );

        if (!buildMeteorBody(
                level,
                center,
                radius,
                random
        )) {
            return false;
        }

        placeCore(
                level,
                center
        );

        scatterRimDebris(
                level,
                surface,
                radius,
                random
        );

        return true;
    }

    private static boolean buildMeteorBody(
            WorldGenLevel level,
            BlockPos center,
            int radius,
            RandomSource random
    ) {
        double outerRadiusSq =
                radius * radius;

        double innerRadius =
                radius - 1.35D;

        double innerRadiusSq =
                innerRadius * innerRadius;

        boolean placedAny = false;

        for (int dx = -radius;
             dx <= radius;
             dx++) {
            for (int dy = -radius;
                 dy <= radius;
                 dy++) {
                for (int dz = -radius;
                     dz <= radius;
                     dz++) {
                    double distanceSq =
                            dx * dx
                                    + dz * dz
                                    + dy * dy
                                    * 1.10D;

                    if (distanceSq
                            > outerRadiusSq) {
                        continue;
                    }

                    BlockPos target =
                            center.offset(
                                    dx,
                                    dy,
                                    dz
                            );

                    BlockState existing =
                            level.getBlockState(
                                    target
                            );

                    if (!canMeteorReplace(
                            existing
                    )) {
                        continue;
                    }

                    if (distanceSq
                            >= innerRadiusSq) {
                        level.setBlock(
                                target,
                                random.nextFloat()
                                        < 0.22F
                                        ? CRYING_OBSIDIAN
                                        : OBSIDIAN,
                                2
                        );

                        placedAny = true;
                    } else {
                        level.setBlock(
                                target,
                                AIR,
                                2
                        );
                    }
                }
            }
        }

        return placedAny;
    }

    private static void placeCore(
            WorldGenLevel level,
            BlockPos center
    ) {
        BlockPos coreMin =
                center.offset(
                        -1,
                        -1,
                        -1
                );

        BlockState remnant =
                CWBlocks.STELLAR_REMNANT
                        .get()
                        .defaultBlockState();

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    level.setBlock(
                            coreMin.offset(
                                    x,
                                    y,
                                    z
                            ),
                            remnant,
                            2
                    );
                }
            }
        }
    }

    private static void carveShallowCrater(
            WorldGenLevel level,
            BlockPos surface,
            int radius,
            RandomSource random
    ) {
        int craterRadius =
                radius + 2;

        for (int dx = -craterRadius;
             dx <= craterRadius;
             dx++) {
            for (int dz = -craterRadius;
                 dz <= craterRadius;
                 dz++) {
                double distance =
                        Math.sqrt(
                                dx * dx
                                        + dz * dz
                        );

                if (distance
                        > craterRadius
                        + random.nextDouble()
                        * 0.55D) {
                    continue;
                }

                int worldX =
                        surface.getX() + dx;

                int worldZ =
                        surface.getZ() + dz;

                int topY =
                        level.getHeight(
                                Heightmap.Types
                                        .WORLD_SURFACE_WG,
                                worldX,
                                worldZ
                        ) - 1;

                double normalized =
                        distance
                                / craterRadius;

                int depth =
                        normalized < 0.32D
                                ? 3
                                : normalized < 0.68D
                                ? 2
                                : 1;

                for (int step = 0;
                     step < depth;
                     step++) {
                    BlockPos target =
                            new BlockPos(
                                    worldX,
                                    topY - step,
                                    worldZ
                            );

                    BlockState state =
                            level.getBlockState(
                                    target
                            );

                    if (canMeteorReplace(state)
                            && !state.isAir()) {
                        level.setBlock(
                                target,
                                AIR,
                                2
                        );
                    }
                }
            }
        }
    }

    private static void scatterRimDebris(
            WorldGenLevel level,
            BlockPos surface,
            int radius,
            RandomSource random
    ) {
        int attempts =
                7 + random.nextInt(6);

        for (int i = 0;
             i < attempts;
             i++) {
            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0D;

            double distance =
                    radius
                            + 1.5D
                            + random.nextDouble()
                            * 3.0D;

            int x =
                    surface.getX()
                            + (int) Math.round(
                            Math.cos(angle)
                                    * distance
                    );

            int z =
                    surface.getZ()
                            + (int) Math.round(
                            Math.sin(angle)
                                    * distance
                    );

            int y =
                    level.getHeight(
                            Heightmap.Types
                                    .WORLD_SURFACE_WG,
                            x,
                            z
                    );

            BlockPos target =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (!level.getBlockState(target)
                    .isAir()) {
                continue;
            }

            if (!isNaturalTerrain(
                    level.getBlockState(
                            target.below()
                    )
            )) {
                continue;
            }

            level.setBlock(
                    target,
                    random.nextFloat()
                            < 0.28F
                            ? CRYING_OBSIDIAN
                            : OBSIDIAN,
                    2
            );
        }
    }

    /**
     * 陨星外壳可以占据空气，也可以替换末地石；
     * 其他任何方块都不会被世界生成覆盖。
     */
    private static boolean canMeteorReplace(
            BlockState state
    ) {
        return state.isAir()
                || isNaturalTerrain(state);
    }

    /**
     * 所有地表支撑、陨坑挖掘和散落碎片落点都必须是末地石。
     */
    private static boolean isNaturalTerrain(
            BlockState state
    ) {
        return state.is(Blocks.END_STONE);
    }
}
