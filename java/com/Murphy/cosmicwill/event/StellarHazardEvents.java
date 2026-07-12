package com.Murphy.cosmicwill.event;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.registry.CWBlocks;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StellarHazardEvents {

    private static final UUID MOVE_SLOW_UUID =
            UUID.fromString(
                    "a0434b54-2b73-4c85-b4b2-665df0b54621"
            );

    private static final UUID ATTACK_SLOW_UUID =
            UUID.fromString(
                    "52bd640d-79ec-4ba0-b2e4-e00ddc2bb761"
            );

    private StellarHazardEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide) {
            return;
        }

        Player player = event.player;
        boolean carryingStar =
                containsMiniatureStar(player);

        updateTransientModifier(
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                ),
                MOVE_SLOW_UUID,
                "Miniature star movement suppression",
                carryingStar
        );

        updateTransientModifier(
                player.getAttribute(
                        Attributes.ATTACK_SPEED
                ),
                ATTACK_SLOW_UUID,
                "Miniature star attack suppression",
                carryingStar
        );

        if (carryingStar
                && player.tickCount % 20 == 0) {
            player.setSecondsOnFire(2);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {
        if (!event.getState().is(
                CWBlocks.STELLAR_SHELL.get()
        )) {
            return;
        }

        if (!canHarvestStellarShell(
                event.getEntity()
        )) {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(
            BlockEvent.BreakEvent event
    ) {
        if (!event.getState().is(
                CWBlocks.STELLAR_SHELL.get()
        )) {
            return;
        }

        if (!canHarvestStellarShell(
                event.getPlayer()
        )) {
            event.setCanceled(true);
        }
    }

    private static boolean containsMiniatureStar(
            Player player
    ) {
        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {
            ItemStack stack =
                    player.getInventory()
                            .getItem(slot);

            if (stack.is(
                    CWBlocks.MINIATURE_STAR
                            .get()
                            .asItem()
            )) {
                return true;
            }
        }

        return false;
    }

    private static void updateTransientModifier(
            AttributeInstance attribute,
            UUID id,
            String name,
            boolean enabled
    ) {
        if (attribute == null) {
            return;
        }

        AttributeModifier existing =
                attribute.getModifier(id);

        if (enabled && existing == null) {
            attribute.addTransientModifier(
                    new AttributeModifier(
                            id,
                            name,
                            -0.5D,
                            AttributeModifier.Operation
                                    .MULTIPLY_TOTAL
                    )
            );
        } else if (!enabled && existing != null) {
            attribute.removeModifier(id);
        }
    }

    private static boolean canHarvestStellarShell(
            Player player
    ) {
        if (player.isCreative()) {
            return true;
        }

        return player.getMainHandItem()
                .is(Items.NETHERITE_PICKAXE);
    }
}
