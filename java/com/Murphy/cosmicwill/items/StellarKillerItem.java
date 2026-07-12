package com.Murphy.cosmicwill.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class StellarKillerItem extends SwordItem {

    private static final int BURN_SECONDS = 5;

    public StellarKillerItem(
            Tier tier,
            int attackDamageModifier,
            float attackSpeedModifier,
            Properties properties
    ) {
        super(
                tier,
                attackDamageModifier,
                attackSpeedModifier,
                properties
        );
    }

    @Override
    public boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker
    ) {
        if (!target.fireImmune()) {
            target.setSecondsOnFire(BURN_SECONDS);
        }

        return super.hurtEnemy(
                stack,
                target,
                attacker
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.stellar_killer.piercing"
                ).withStyle(ChatFormatting.AQUA)
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.stellar_killer.fire"
                ).withStyle(ChatFormatting.RED)
        );

        super.appendHoverText(
                stack,
                level,
                tooltip,
                flag
        );
    }
}
