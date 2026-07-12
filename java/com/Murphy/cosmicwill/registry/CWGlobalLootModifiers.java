package com.Murphy.cosmicwill.registry;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.loot.AddStellarRemnantLootModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CWGlobalLootModifiers {

    /**
     * Forge 1.20.1 的 GLOBAL_LOOT_MODIFIER_SERIALIZERS
     * 注册表元素类型是 Codec<? extends IGlobalLootModifier>。
     */
    public static final DeferredRegister<
            Codec<? extends IGlobalLootModifier>
            > GLOBAL_LOOT_MODIFIERS =
            DeferredRegister.create(
                    ForgeRegistries.Keys
                            .GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    CustomWill.MODID
            );

    public static final RegistryObject<
            Codec<? extends IGlobalLootModifier>
            > ADD_STELLAR_REMNANT =
            GLOBAL_LOOT_MODIFIERS.register(
                    "add_stellar_remnant",
                    () -> AddStellarRemnantLootModifier.CODEC
            );

    private CWGlobalLootModifiers() {
    }
}