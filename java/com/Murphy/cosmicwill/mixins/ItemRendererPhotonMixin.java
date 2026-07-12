package com.Murphy.cosmicwill.mixins;

import com.Murphy.cosmicwill.CWitems;
import com.Murphy.cosmicwill.client.NullifierPhotonRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ItemRenderer.class)
public abstract class ItemRendererPhotonMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("TAIL")
    )
    private void cosmicwill$renderNullifierPhotons(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            BakedModel model,
            CallbackInfo ci
    ) {
        if (!stack.is(CWitems.NULLIFIER_SWORD.get())
                || !NullifierPhotonRenderer.shouldRender(displayContext)) {
            return;
        }

        poseStack.pushPose();

        /* 与原版物品使用完全相同的主手、副手和视角变换。 */
        model.applyTransform(displayContext, poseStack, leftHand);
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        NullifierPhotonRenderer.render(stack, poseStack, buffers);

        poseStack.popPose();
    }
}
