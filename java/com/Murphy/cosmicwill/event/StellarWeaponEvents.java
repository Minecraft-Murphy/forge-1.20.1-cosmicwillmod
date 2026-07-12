package com.Murphy.cosmicwill.event;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.items.StellarKillerItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StellarWeaponEvents {

    private static final Map<
            LivingEntity,
            PendingPiercingHit
            > PENDING_HITS =
            new WeakHashMap<>();

    private StellarWeaponEvents() {
    }

    /**
     * 在原版进行护甲计算之前记录伤害。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(
            LivingHurtEvent event
    ) {
        LivingEntity target = event.getEntity();

        if (target.level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();

        if (!(source.getEntity()
                instanceof Player player)) {
            return;
        }

        if (source.getDirectEntity() != player) {
            return;
        }

        if (!(player.getMainHandItem().getItem()
                instanceof StellarKillerItem)) {
            return;
        }

        if (source.is(
                DamageTypeTags.BYPASSES_ARMOR
        )) {
            return;
        }

        float armor = target.getArmorValue();
        float toughness = (float) target
                .getAttributeValue(
                        Attributes.ARMOR_TOUGHNESS
                );

        PENDING_HITS.put(
                target,
                new PendingPiercingHit(
                        source,
                        event.getAmount(),
                        armor,
                        toughness
                )
        );
    }

    /**
     * 此时护甲、药水、附魔和伤害吸收已经参与计算。
     *
     * 通过“最终伤害 / 护甲后伤害”的比例保留其他防御机制，
     * 再把该比例应用到护甲前伤害，从而只恢复被护甲削减的部分。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(
            LivingDamageEvent event
    ) {
        LivingEntity target = event.getEntity();

        if (target.level().isClientSide) {
            return;
        }

        PendingPiercingHit pending =
                PENDING_HITS.remove(target);

        if (pending == null
                || pending.source()
                != event.getSource()) {
            return;
        }

        float beforeArmor =
                pending.amountBeforeArmor();

        float afterArmor =
                applyArmorReduction(
                        beforeArmor,
                        pending.armor(),
                        pending.toughness()
                );

        if (beforeArmor <= 0.0F
                || afterArmor <= 0.0F
                || event.getAmount() <= 0.0F) {
            return;
        }

        float retainedDefenseRatio =
                event.getAmount()
                        / afterArmor;

        float piercingFinal =
                beforeArmor
                        * retainedDefenseRatio;

        event.setAmount(
                Math.max(
                        event.getAmount(),
                        piercingFinal
                )
        );
    }

    /**
     * 与原版 CombatRules 的护甲计算一致。
     */
    private static float applyArmorReduction(
            float damage,
            float armor,
            float toughness
    ) {
        float divisor =
                2.0F + toughness / 4.0F;

        float effectiveArmor =
                Mth.clamp(
                        armor - damage / divisor,
                        armor * 0.2F,
                        20.0F
                );

        return damage
                * (1.0F
                - effectiveArmor / 25.0F);
    }

    private record PendingPiercingHit(
            DamageSource source,
            float amountBeforeArmor,
            float armor,
            float toughness
    ) {
    }
}
