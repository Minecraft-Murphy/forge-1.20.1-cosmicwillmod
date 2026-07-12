package com.Murphy.cosmicwill.registry;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.worldgen.feature.StellarMeteorFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CWFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, CustomWill.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>>
            STELLAR_METEOR = FEATURES.register(
                    "stellar_meteor",
                    () -> new StellarMeteorFeature(NoneFeatureConfiguration.CODEC)
            );

    private CWFeatures() {
    }
}
