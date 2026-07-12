package com.Murphy.cosmicwill.registry;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.menu.CompressedFurnaceMenu;
import com.Murphy.cosmicwill.menu.MatterCompressorMenu;
import com.Murphy.cosmicwill.menu.MatterDeconstructorMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CWMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    ForgeRegistries.MENU_TYPES,
                    CustomWill.MODID
            );

    public static final RegistryObject<MenuType<CompressedFurnaceMenu>>
            COMPRESSED_FURNACE =
            MENUS.register(
                    "compressed_furnace",
                    () -> new MenuType<>(
                            CompressedFurnaceMenu::new,
                            FeatureFlags.DEFAULT_FLAGS
                    )
            );

    public static final RegistryObject<MenuType<MatterDeconstructorMenu>>
            MATTER_DECONSTRUCTOR =
            MENUS.register(
                    "matter_deconstructor",
                    () -> IForgeMenuType.create(
                            MatterDeconstructorMenu::new
                    )
            );

    public static final RegistryObject<MenuType<MatterCompressorMenu>>
            MATTER_COMPRESSOR =
            MENUS.register(
                    "matter_compressor",
                    () -> IForgeMenuType.create(
                            MatterCompressorMenu::new
                    )
            );

    private CWMenus() {
    }
}
