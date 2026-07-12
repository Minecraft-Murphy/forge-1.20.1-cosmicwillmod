package com.Murphy.cosmicwill.matter;

import com.Murphy.cosmicwill.CustomWill;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class CWMatterCapabilities {

    public static final Capability<IMatterStorage> MATTER =
            CapabilityManager.get(
                    new CapabilityToken<>() {
                    }
            );

    private CWMatterCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(
            RegisterCapabilitiesEvent event
    ) {
        event.register(IMatterStorage.class);
    }
}
