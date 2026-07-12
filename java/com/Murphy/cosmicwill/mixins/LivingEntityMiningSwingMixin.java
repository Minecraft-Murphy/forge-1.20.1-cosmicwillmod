package com.Murphy.cosmicwill.mixins;

import com.Murphy.cosmicwill.items.NullifierSwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 仅在客户端阻止归零者对方块进行“挖掘挥手”。
 *
 * 攻击实体时 hitResult 为 EntityHitResult，不会被取消；
 * 右键攻击和模式切换动画也不会受影响。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMiningSwingMixin {

    @Inject(
            method =
                    "swing(Lnet/minecraft/world/InteractionHand;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cosmicwill$cancelNullifierMiningSwing(
            InteractionHand hand,
            boolean updateSelf,
            CallbackInfo callback
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (self != minecraft.player
                || !(minecraft.hitResult
                instanceof BlockHitResult)
                || !minecraft.options
                .keyAttack
                .isDown()) {
            return;
        }

        ItemStack held =
                self.getItemInHand(hand);

        if (held.getItem()
                instanceof NullifierSwordItem) {
            callback.cancel();
        }
    }
}
