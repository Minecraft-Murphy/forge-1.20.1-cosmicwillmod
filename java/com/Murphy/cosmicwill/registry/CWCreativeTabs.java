package com.Murphy.cosmicwill.registry;

import com.Murphy.cosmicwill.CWitems;
import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.items.NullifierSwordItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CWCreativeTabs {

    public static final DeferredRegister<CreativeModeTab>
            CREATIVE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    CustomWill.MODID
            );

    private static final Map<String, Integer>
            BLOCK_ORDER =
            Map.ofEntries(
                    Map.entry("stellar_remnant", 0),
                    Map.entry("nether_star_block", 10),
                    Map.entry(
                            "strong_interaction_material",
                            20
                    ),
                    Map.entry("stellar_shell", 30),
                    Map.entry("miniature_star", 40),
                    Map.entry("compressed_furnace", 50),
                    Map.entry("compressed_piston", 60),
                    Map.entry("matter_deconstructor", 70),
                    Map.entry("matter_compressor", 80)
            );

    private static final Map<String, Integer>
            MATERIAL_ORDER =
            Map.ofEntries(
                    Map.entry("stellar_fragment", 0),
                    Map.entry(
                            "strong_interaction_primordium",
                            10
                    ),
                    Map.entry("stellar_ingot", 20),
                    Map.entry("stellar_core", 30),
                    Map.entry("bedrock_grain", 40),
                    Map.entry("bedrock_plate", 50)
            );

    private static final Map<String, Integer>
            WEAPON_ORDER =
            Map.ofEntries(
                    Map.entry("stellar_killer", 0),
                    Map.entry("empty_energy_blade", 10),
                    Map.entry("charged_energy_blade", 20),
                    Map.entry(
                            "sealed_charged_energy_blade",
                            30
                    )
            );

    public static final RegistryObject<CreativeModeTab>
            COSMIC_WILL_TAB =
            CREATIVE_TABS.register(
                    "cosmic_will",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.literal(
                                            "宇宙意志"
                                    )
                            )
                            .icon(
                                    CWCreativeTabs::createNullificationIcon
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        List<Item> blocks =
                                                new ArrayList<>();

                                        List<Item> materials =
                                                new ArrayList<>();

                                        List<Item> weapons =
                                                new ArrayList<>();

                                        for (Item item
                                                : ForgeRegistries.ITEMS
                                                .getValues()) {
                                            if (!belongsToThisMod(item)
                                                    || item == Items.AIR
                                                    || isHidden(item)) {
                                                continue;
                                            }

                                            if (item
                                                    instanceof BlockItem) {
                                                blocks.add(item);
                                            } else if (isWeapon(item)) {
                                                weapons.add(item);
                                            } else {
                                                materials.add(item);
                                            }
                                        }

                                        blocks.sort(
                                                categoryComparator(
                                                        BLOCK_ORDER
                                                )
                                        );

                                        materials.sort(
                                                categoryComparator(
                                                        MATERIAL_ORDER
                                                )
                                        );

                                        weapons.sort(
                                                categoryComparator(
                                                        WEAPON_ORDER
                                                )
                                        );

                                        blocks.forEach(
                                                output::accept
                                        );

                                        materials.forEach(
                                                output::accept
                                        );

                                        weapons.forEach(
                                                output::accept
                                        );
                                    }
                            )
                            .build()
            );

    private CWCreativeTabs() {
    }

    private static ItemStack createNullificationIcon() {
        ItemStack icon =
                new ItemStack(
                        CWitems.NULLIFIER_SWORD.get()
                );

        NullifierSwordItem.setMode(
                icon,
                NullifierSwordItem.MODE_NULLIFICATION
        );

        return icon;
    }

    private static boolean isHidden(Item item) {
        return item == CWitems.NULLIFIER_SWORD.get()
                || item
                == CWitems.ARTIFICIAL_SINGULARITY.get();
    }

    private static boolean isWeapon(Item item) {
        return item instanceof SwordItem
                || item
                == CWitems.SEALED_CHARGED_ENERGY_BLADE
                .get();
    }

    private static Comparator<Item> categoryComparator(
            Map<String, Integer> order
    ) {
        return Comparator
                .comparingInt(
                        (Item item) ->
                                order.getOrDefault(
                                        registryPath(item),
                                        10_000
                                )
                )
                .thenComparing(
                        CWCreativeTabs::registryPath
                );
    }

    private static boolean belongsToThisMod(
            Item item
    ) {
        ResourceLocation id =
                ForgeRegistries.ITEMS.getKey(item);

        return id != null
                && CustomWill.MODID.equals(
                        id.getNamespace()
                );
    }

    private static String registryPath(
            Item item
    ) {
        ResourceLocation id =
                ForgeRegistries.ITEMS.getKey(item);

        return id == null
                ? ""
                : id.getPath();
    }
}
