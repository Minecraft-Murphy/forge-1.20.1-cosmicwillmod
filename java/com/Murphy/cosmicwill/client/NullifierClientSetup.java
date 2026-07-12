package com.Murphy.cosmicwill.client;

import com.Murphy.cosmicwill.CWitems;
import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.items.NullifierSwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Objects;

/** 注册归零者之剑的客户端动态模型谓词。 */
@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class NullifierClientSetup {

    private static final ResourceLocation VISUAL_STATE =
            Objects.requireNonNull(
                    ResourceLocation.tryBuild(
                            CustomWill.MODID,
                            "nullifier_visual_state"
                    )
            );

    private NullifierClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                CWitems.NULLIFIER_SWORD.get(),
                VISUAL_STATE,
                (stack, level, holder, seed) -> {
                    ClientLevel effectiveLevel = level;
                    if (effectiveLevel == null) {
                        effectiveLevel = Minecraft.getInstance().level;
                    }

                    return NullifierSwordItem.getVisualState(
                            stack,
                            effectiveLevel
                    ) / 100.0F;
                }
        ));
    }
}
