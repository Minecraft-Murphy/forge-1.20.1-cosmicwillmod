package com.Murphy.cosmicwill;

import com.Murphy.cosmicwill.config.CWServerConfig;
import com.Murphy.cosmicwill.network.CWNetwork;
import com.Murphy.cosmicwill.registry.CWBlockEntities;
import com.Murphy.cosmicwill.registry.CWBlocks;
import com.Murphy.cosmicwill.registry.CWCreativeTabs;
import com.Murphy.cosmicwill.registry.CWFeatures;
import com.Murphy.cosmicwill.registry.CWGlobalLootModifiers;
import com.Murphy.cosmicwill.registry.CWMenus;
import com.Murphy.cosmicwill.registry.CWRecipeSerializers;
import com.Murphy.cosmicwill.registry.CWRecipeTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CustomWill.MODID)
public class CustomWill {

    public static final String MODID = "cosmicwill";

    public CustomWill(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        CWBlocks.BLOCKS.register(modEventBus);
        CWitems.ITEMS.register(modEventBus);
        CWBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CWMenus.MENUS.register(modEventBus);
        CWRecipeTypes.RECIPE_TYPES.register(modEventBus);
        CWRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        CWFeatures.FEATURES.register(modEventBus);
        CWGlobalLootModifiers.GLOBAL_LOOT_MODIFIERS.register(modEventBus);
        CWCreativeTabs.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        context.registerConfig(
                ModConfig.Type.SERVER,
                CWServerConfig.SPEC
        );

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CWNetwork::register);
    }
}
