package com.Murphy.cosmicwill.client;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.client.render.MatterCompressorRenderer;
import com.Murphy.cosmicwill.client.render.MiniatureStarRenderer;
import com.Murphy.cosmicwill.client.screen.CompressedFurnaceScreen;
import com.Murphy.cosmicwill.client.screen.MatterCompressorScreen;
import com.Murphy.cosmicwill.client.screen.MatterDeconstructorScreen;
import com.Murphy.cosmicwill.registry.CWBlockEntities;
import com.Murphy.cosmicwill.registry.CWMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class CWClientSetup {

    private CWClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(
            FMLClientSetupEvent event
    ) {
        event.enqueueWork(() -> {
            MenuScreens.register(
                    CWMenus.COMPRESSED_FURNACE.get(),
                    CompressedFurnaceScreen::new
            );

            MenuScreens.register(
                    CWMenus.MATTER_DECONSTRUCTOR.get(),
                    MatterDeconstructorScreen::new
            );

            MenuScreens.register(
                    CWMenus.MATTER_COMPRESSOR.get(),
                    MatterCompressorScreen::new
            );

            BlockEntityRenderers.register(
                    CWBlockEntities.MATTER_COMPRESSOR.get(),
                    MatterCompressorRenderer::new
            );

            BlockEntityRenderers.register(
                    CWBlockEntities.MINIATURE_STAR.get(),
                    MiniatureStarRenderer::new
            );
        });
    }
}
