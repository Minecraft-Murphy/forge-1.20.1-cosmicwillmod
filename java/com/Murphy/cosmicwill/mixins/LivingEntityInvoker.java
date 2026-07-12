package com.Murphy.cosmicwill.mixins;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Invoker("dropAllDeathLoot")
    void cosmicwill$invokeDropAllDeathLoot(DamageSource source);

    @Invoker("dropExperience")
    void cosmicwill$invokeDropExperience();
}
