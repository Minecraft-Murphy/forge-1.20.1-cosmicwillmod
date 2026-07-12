package com.Murphy.cosmicwill.item;

import com.Murphy.cosmicwill.CWitems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

public final class CWTiers {

    public static final Tier STELLAR_KILLER =
            new ForgeTier(
                    4,
                    Tiers.NETHERITE.getUses() + 1000,
                    Tiers.NETHERITE.getSpeed(),
                    4.0F,
                    Tiers.NETHERITE.getEnchantmentValue(),
                    BlockTags.NEEDS_DIAMOND_TOOL,
                    () -> Ingredient.of(
                            CWitems.STELLAR_INGOT.get()
                    )
            );

    public static final Tier EMPTY_ENERGY_BLADE =
            new ForgeTier(
                    4,
                    Tiers.GOLD.getUses(),
                    Tiers.GOLD.getSpeed(),
                    0.0F,
                    Tiers.GOLD.getEnchantmentValue(),
                    BlockTags.NEEDS_DIAMOND_TOOL,
                    () -> Ingredient.of(
                            CWitems.STRONG_INTERACTION_PRIMORDIUM.get()
                    )
            );

    public static final Tier CHARGED_ENERGY_BLADE =
            new ForgeTier(
                    4,
                    Tiers.NETHERITE.getUses() + 1000,
                    Tiers.NETHERITE.getSpeed(),
                    0.0F,
                    Tiers.NETHERITE.getEnchantmentValue(),
                    BlockTags.NEEDS_DIAMOND_TOOL,
                    () -> Ingredient.of(
                            CWitems.STELLAR_INGOT.get()
                    )
            );

    private CWTiers() {
    }
}
