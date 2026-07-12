package com.Murphy.cosmicwill.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class EmptyEnergyBladeItem extends SwordItem {

    public EmptyEnergyBladeItem(
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

    /**
     * Forge 在真正计算玩家攻击前调用此方法。
     * 返回 true 会完整取消本次攻击，因此不会造成：
     * 伤害、击退、火焰、横扫、耐久消耗或受击无敌帧。
     */
    @Override
    public boolean onLeftClickEntity(
            ItemStack stack,
            Player player,
            Entity entity
    ) {
        return true;
    }

    /**
     * 未充能的剑体无法稳定容纳内部恒星物质。
     * 裸持时每秒尝试让持有者燃烧两秒。
     */
    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean selected
    ) {
        super.inventoryTick(
                stack,
                level,
                entity,
                slotId,
                selected
        );

        if (level.isClientSide
                || !(entity instanceof LivingEntity living)) {
            return;
        }

        boolean held =
                living.getMainHandItem() == stack
                        || living.getOffhandItem() == stack;

        if (!held || living.tickCount % 20 != 0) {
            return;
        }

        if (isProtectedFromIgnition(living)) {
            return;
        }

        if (!living.isInWaterRainOrBubble()) {
            living.setSecondsOnFire(2);
        }
    }

    private static boolean isProtectedFromIgnition(
            LivingEntity living
    ) {
        if (living.fireImmune()
                || living.hasEffect(
                MobEffects.FIRE_RESISTANCE
        )) {
            return true;
        }

        for (ItemStack armor
                : living.getArmorSlots()) {
            if (EnchantmentHelper
                    .getItemEnchantmentLevel(
                            Enchantments.FIRE_PROTECTION,
                            armor
                    ) > 0) {
                return true;
            }
        }

        return false;
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
                        "tooltip.cosmicwill.empty_energy_blade.inert"
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.empty_energy_blade.burning"
                ).withStyle(ChatFormatting.RED)
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.empty_energy_blade.immunity"
                ).withStyle(ChatFormatting.GOLD)
        );

        super.appendHoverText(
                stack,
                level,
                tooltip,
                flag
        );
    }
}
