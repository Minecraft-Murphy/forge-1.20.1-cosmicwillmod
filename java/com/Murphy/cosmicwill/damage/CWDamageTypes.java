package com.Murphy.cosmicwill.damage;

import com.Murphy.cosmicwill.CustomWill;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Objects;

public final class CWDamageTypes {

    public static final ResourceKey<DamageType> MINIATURE_STAR =
            createKey("miniature_star");

    public static final ResourceKey<DamageType> BLACK_HOLE =
            createKey("black_hole");

    public static final ResourceKey<DamageType> NULLIFICATION =
            createKey("nullification");

    /**
     * 充能能量剑的恒星爆震。
     *
     * 只加入 is_explosion，不加入任何穿甲标签：
     * 护甲、韧性与爆炸保护都会正常生效。
     */
    public static final ResourceKey<DamageType>
            CHARGED_BLADE_EXPLOSION =
            createKey("charged_blade_explosion");

    private CWDamageTypes() {
    }

    public static DamageSource miniatureStar(
            Level level
    ) {
        return source(
                level,
                MINIATURE_STAR
        );
    }

    public static DamageSource blackHole(
            Level level
    ) {
        return source(
                level,
                BLACK_HOLE
        );
    }

    public static DamageSource nullification(
            Level level,
            @Nullable Entity attacker
    ) {
        return sourceWithAttacker(
                level,
                NULLIFICATION,
                attacker
        );
    }

    public static DamageSource chargedBladeExplosion(
            Level level,
            @Nullable Entity attacker
    ) {
        return sourceWithAttacker(
                level,
                CHARGED_BLADE_EXPLOSION,
                attacker
        );
    }

    public static boolean isNullification(
            DamageSource source
    ) {
        return source.is(NULLIFICATION);
    }

    private static ResourceKey<DamageType> createKey(
            String path
    ) {
        return ResourceKey.create(
                Registries.DAMAGE_TYPE,
                Objects.requireNonNull(
                        ResourceLocation.tryBuild(
                                CustomWill.MODID,
                                path
                        )
                )
        );
    }

    private static DamageSource source(
            Level level,
            ResourceKey<DamageType> key
    ) {
        return new DamageSource(
                holder(level, key)
        );
    }

    private static DamageSource sourceWithAttacker(
            Level level,
            ResourceKey<DamageType> key,
            @Nullable Entity attacker
    ) {
        Holder<DamageType> holder =
                holder(level, key);

        return attacker == null
                ? new DamageSource(holder)
                : new DamageSource(
                        holder,
                        attacker,
                        attacker
                );
    }

    private static Holder<DamageType> holder(
            Level level,
            ResourceKey<DamageType> key
    ) {
        return level.registryAccess()
                .registryOrThrow(
                        Registries.DAMAGE_TYPE
                )
                .getHolderOrThrow(key);
    }
}
