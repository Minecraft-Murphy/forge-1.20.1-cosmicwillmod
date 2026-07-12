package com.Murphy.cosmicwill.compat;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class GenericBossBarCompat {
    private GenericBossBarCompat() {
    }

    public static void closeBossBars(Entity entity) {
        for (Field field : ReflectionUtil.allFields(entity.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value instanceof ServerBossEvent bossEvent) {
                    bossEvent.removeAllPlayers();
                    bossEvent.setVisible(false);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
