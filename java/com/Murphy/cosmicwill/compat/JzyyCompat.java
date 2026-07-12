package com.Murphy.cosmicwill.compat;

import com.Murphy.cosmicwill.nullification.ErasureContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class JzyyCompat {

    private static final String PREFIX = "com.csdy.jzyy.";

    private JzyyCompat() {
    }

    public static boolean isRoot(Entity entity) {
        return entity.getClass().getName().startsWith(PREFIX);
    }

    public static void beforeErase(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !isRoot(entity)) {
            return;
        }

        try {
            Class<?> enumType = ReflectionUtil.findClass(
                    "com.csdy.jzyy.ms.enums.EntityCategory"
            );
            if (enumType != null && enumType.isEnum()) {
                Object killCategory = Enum.valueOf((Class<? extends Enum>) enumType, "csdykill");
                ReflectionUtil.invokeStaticVoid(
                        "com.csdy.jzyy.ms.CoreMsUtil",
                        new String[]{"setCategory"},
                        living,
                        killCategory
                );
            }
        } catch (Throwable ignored) {
        }

        ReflectionUtil.invokeStaticVoid(
                "com.csdy.jzyy.ms.util.LivingEntityUtil",
                new String[]{"setAbsoluteSeveranceHealth"},
                living,
                0.0F
        );
        ReflectionUtil.invokeStaticVoid(
                "com.csdy.jzyy.ms.util.LivingEntityUtil",
                new String[]{"forceSetAllCandidateHealth"},
                living,
                0.0F
        );

        ReflectionUtil.setNumberField(living, 0.0F,
                "currentHealth", "realHealth", "trueHealth");
        ReflectionUtil.setBooleanField(living, true,
                "isDying", "isDead", "dead");
    }

    public static boolean isAuxiliary(ErasureContext context, Entity candidate) {
        if (!context.rootClassName().startsWith(PREFIX)) {
            return false;
        }
        if (!candidate.getClass().getName().startsWith(PREFIX)) {
            return false;
        }
        if (candidate instanceof LivingEntity) {
            return false;
        }
        return candidate.position().distanceToSqr(context.origin()) <= 256.0D * 256.0D;
    }

    public static void lastResort(Entity entity) {
        if (!isRoot(entity)) {
            return;
        }
        ReflectionUtil.invokeStaticVoid(
                "com.csdy.jzyy.ms.util.MsUtil",
                new String[]{"superKillEntity"},
                entity
        );
    }
}
