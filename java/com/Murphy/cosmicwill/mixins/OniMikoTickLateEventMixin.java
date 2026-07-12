package com.Murphy.cosmicwill.mixins;

import com.Murphy.cosmicwill.compat.OniMikoCompat;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "oni_miko.event.TickLateEvent",
        remap = false,
        priority = 2000
)
public abstract class OniMikoTickLateEventMixin {


    @Dynamic("Optional target supplied at runtime by the Oni Miko mod")
    @Inject(
            method = "tickEvent(Lnet/minecraftforge/event/TickEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void cosmicwill$cancelErasedTask(
            TickEvent event,
            CallbackInfo ci
    ) {
        if (!OniMikoCompat.shouldCancelTickLateEvent(this)) {
            return;
        }

        try {
            MinecraftForge.EVENT_BUS.unregister(this);
        } catch (Throwable ignored) {
        }

        ci.cancel();
    }
}
