package com.Murphy.cosmicwill.menu;

import com.Murphy.cosmicwill.blockentity.MatterCompressorBlockEntity;
import com.Murphy.cosmicwill.registry.CWBlocks;
import com.Murphy.cosmicwill.registry.CWMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class MatterCompressorMenu
        extends AbstractContainerMenu {

    private static final int ANCHOR_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    private final MatterCompressorBlockEntity machine;
    private final ContainerLevelAccess access;

    public MatterCompressorMenu(
            int containerId,
            Inventory inventory,
            FriendlyByteBuf buffer
    ) {
        this(
                containerId,
                inventory,
                getMachine(
                        inventory,
                        buffer.readBlockPos()
                )
        );
    }

    public MatterCompressorMenu(
            int containerId,
            Inventory inventory,
            MatterCompressorBlockEntity machine
    ) {
        super(
                CWMenus.MATTER_COMPRESSOR.get(),
                containerId
        );

        this.machine = machine;
        this.access = ContainerLevelAccess.create(
                inventory.player.level(),
                machine.getBlockPos()
        );

        addSlot(
                new SlotItemHandler(
                        machine.getAnchorInventory(),
                        0,
                        80,
                        25
                ) {
                    @Override
                    public boolean mayPlace(
                            ItemStack stack
                    ) {
                        return !machine.isProcessing()
                                && super.mayPlace(stack);
                    }

                    @Override
                    public boolean mayPickup(
                            Player player
                    ) {
                        return !machine.isProcessing()
                                && super.mayPickup(player);
                    }
                }
        );

        for (int row = 0; row < 3; row++) {
            for (int column = 0;
                 column < 9;
                 column++) {
                addSlot(
                        new Slot(
                                inventory,
                                column
                                        + row * 9
                                        + 9,
                                8 + column * 18,
                                58 + row * 18
                        )
                );
            }
        }

        for (int column = 0;
             column < 9;
             column++) {
            addSlot(
                    new Slot(
                            inventory,
                            column,
                            8 + column * 18,
                            116
                    )
            );
        }
    }

    private static MatterCompressorBlockEntity
    getMachine(
            Inventory inventory,
            BlockPos pos
    ) {
        if (inventory.player.level()
                .getBlockEntity(pos)
                instanceof MatterCompressorBlockEntity machine) {
            return machine;
        }

        throw new IllegalStateException(
                "Matter compressor block entity is missing at "
                        + pos
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                CWBlocks.MATTER_COMPRESSOR.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == ANCHOR_SLOT) {
            if (!moveItemStackTo(
                    stack,
                    PLAYER_INV_START,
                    HOTBAR_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (machine.isProcessing()) {
                return ItemStack.EMPTY;
            }

            if (!moveItemStackTo(
                    stack,
                    ANCHOR_SLOT,
                    ANCHOR_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount()
                == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }

    public MatterCompressorBlockEntity
    getMachine() {
        return machine;
    }
}
