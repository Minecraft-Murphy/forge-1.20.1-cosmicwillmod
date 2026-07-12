package com.Murphy.cosmicwill.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class StatefulMachineBlockItem extends BlockItem {

    public StatefulMachineBlockItem(
            Block block,
            Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        CompoundTag blockEntityTag =
                stack.getTagElement("BlockEntityTag");

        if (blockEntityTag != null) {
            long matter = blockEntityTag.getLong("Matter");
            boolean safeMode = !blockEntityTag.contains("SafeMode")
                    || blockEntityTag.getBoolean("SafeMode");

            if (matter > 0L) {
                tooltip.add(
                        Component.translatable(
                                "tooltip.cosmicwill.stored_matter",
                                NumberFormat
                                        .getIntegerInstance(Locale.US)
                                        .format(matter)
                        ).withStyle(ChatFormatting.AQUA)
                );
            }

            tooltip.add(
                    Component.translatable(
                            safeMode
                                    ? "tooltip.cosmicwill.safe_mode_on"
                                    : "tooltip.cosmicwill.safe_mode_off"
                    ).withStyle(
                            safeMode
                                    ? ChatFormatting.GREEN
                                    : ChatFormatting.RED
                    )
            );
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }
}
