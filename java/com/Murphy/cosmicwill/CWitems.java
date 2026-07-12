package com.Murphy.cosmicwill;

import com.Murphy.cosmicwill.item.AlwaysFoilItem;
import com.Murphy.cosmicwill.item.CWTiers;
import com.Murphy.cosmicwill.items.ChargedEnergyBladeItem;
import com.Murphy.cosmicwill.items.EmptyEnergyBladeItem;
import com.Murphy.cosmicwill.items.NullifierSwordItem;
import com.Murphy.cosmicwill.items.SealedChargedEnergyBladeItem;
import com.Murphy.cosmicwill.items.StellarKillerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CWitems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    CustomWill.MODID
            );

    public static final RegistryObject<Item> NULLIFIER_SWORD =
            ITEMS.register(
                    "nullifier_sword",
                    () -> new NullifierSwordItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item>
            STRONG_INTERACTION_PRIMORDIUM =
            ITEMS.register(
                    "strong_interaction_primordium",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(64)
                                    .fireResistant()
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final RegistryObject<Item> STELLAR_FRAGMENT =
            ITEMS.register(
                    "stellar_fragment",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(64)
                                    .fireResistant()
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final RegistryObject<Item> STELLAR_INGOT =
            ITEMS.register(
                    "stellar_ingot",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(64)
                                    .fireResistant()
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final RegistryObject<Item> STELLAR_CORE =
            ITEMS.register(
                    "stellar_core",
                    () -> new AlwaysFoilItem(
                            new Item.Properties()
                                    .stacksTo(64)
                                    .fireResistant()
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final RegistryObject<Item> BEDROCK_GRAIN =
            ITEMS.register(
                    "bedrock_grain",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(64)
                                    .fireResistant()
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final RegistryObject<Item> BEDROCK_PLATE =
            ITEMS.register(
                    "bedrock_plate",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(64)
                                    .fireResistant()
                                    .rarity(Rarity.RARE)
                    )
            );

    /**
     * 人造奇点仍然是物质压缩器内部使用的隐藏锚点标签物品。
     */
    public static final RegistryObject<Item> ARTIFICIAL_SINGULARITY =
            ITEMS.register(
                    "artificial_singularity",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> STELLAR_KILLER =
            ITEMS.register(
                    "stellar_killer",
                    () -> new StellarKillerItem(
                            CWTiers.STELLAR_KILLER,
                            27,
                            -2.4F,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final RegistryObject<Item> EMPTY_ENERGY_BLADE =
            ITEMS.register(
                    "empty_energy_blade",
                    () -> new EmptyEnergyBladeItem(
                            CWTiers.EMPTY_ENERGY_BLADE,
                            3,
                            -1.0F,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final RegistryObject<Item> CHARGED_ENERGY_BLADE =
            ITEMS.register(
                    "charged_energy_blade",
                    () -> new ChargedEnergyBladeItem(
                            CWTiers.CHARGED_ENERGY_BLADE,
                            63,
                            -3.2F,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                                    .rarity(Rarity.EPIC)
                    )
            );

    /**
     * 普通 Item，不具备攻击属性或耐久条。
     *
     * 它是基岩板封存后的无限耐久剑体，也是归零者的新前置武器；
     * 同时能够直接采掘自然基岩。
     */
    public static final RegistryObject<Item>
            SEALED_CHARGED_ENERGY_BLADE =
            ITEMS.register(
                    "sealed_charged_energy_blade",
                    () -> new SealedChargedEnergyBladeItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                                    .rarity(Rarity.EPIC)
                    )
            );

    private CWitems() {
    }
}
