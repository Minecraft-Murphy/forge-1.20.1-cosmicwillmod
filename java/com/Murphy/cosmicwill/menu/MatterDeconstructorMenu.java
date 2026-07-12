package com.Murphy.cosmicwill.menu;

import com.Murphy.cosmicwill.blockentity.MatterDeconstructorBlockEntity;
import com.Murphy.cosmicwill.registry.CWBlocks;
import com.Murphy.cosmicwill.registry.CWMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class MatterDeconstructorMenu
        extends AbstractContainerMenu {

    public static final int BUTTON_TOGGLE_SAFE_MODE = 0;

    private static final int MACHINE_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    private final MatterDeconstructorBlockEntity machine;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /**
     * 客户端构造器。
     */
    public MatterDeconstructorMenu(
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
                ),
                new SimpleContainerData(
                        MatterDeconstructorBlockEntity.DATA_COUNT
                )
        );
    }

    /**
     * 服务端构造器。
     */
    public MatterDeconstructorMenu(
            int containerId,
            Inventory inventory,
            MatterDeconstructorBlockEntity machine,
            ContainerData data
    ) {
        super(
                CWMenus.MATTER_DECONSTRUCTOR.get(),
                containerId
        );

        this.machine = machine;
        this.data = data;
        this.access = ContainerLevelAccess.create(
                inventory.player.level(),
                machine.getBlockPos()
        );

        checkContainerDataCount(
                data,
                MatterDeconstructorBlockEntity.DATA_COUNT
        );

        addSlot(
                new SlotItemHandler(
                        machine.getItemHandler(),
                        0,
                        29,
                        49
                )
        );

        // 玩家主物品栏。
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(
                                inventory,
                                column + row * 9 + 9,
                                8 + column * 18,
                                84 + row * 18
                        )
                );
            }
        }

        // 快捷栏。
        for (int column = 0; column < 9; column++) {
            addSlot(
                    new Slot(
                            inventory,
                            column,
                            8 + column * 18,
                            142
                    )
            );
        }

        addDataSlots(data);
    }

    private static MatterDeconstructorBlockEntity getMachine(
            Inventory inventory,
            BlockPos pos
    ) {
        if (inventory.player.level()
                .getBlockEntity(pos)
                instanceof MatterDeconstructorBlockEntity machine) {
            return machine;
        }

        throw new IllegalStateException(
                "Matter deconstructor block entity is missing at "
                        + pos
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                CWBlocks.MATTER_DECONSTRUCTOR.get()
        );
    }

    @Override
    public boolean clickMenuButton(
            Player player,
            int buttonId
    ) {
        if (buttonId != BUTTON_TOGGLE_SAFE_MODE) {
            return false;
        }

        if (!player.level().isClientSide) {
            machine.toggleSafeMode();
            broadcastChanges();
        }

        return true;
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

        if (index == MACHINE_SLOT) {
            if (!moveItemStackTo(
                    stack,
                    PLAYER_INV_START,
                    HOTBAR_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!machine.canDeconstruct(stack)) {
                return ItemStack.EMPTY;
            }

            if (!moveItemStackTo(
                    stack,
                    MACHINE_SLOT,
                    MACHINE_SLOT + 1,
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

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }

    public long getMatter() {
        return combineLong(data.get(0), data.get(1));
    }

    public long getCapacity() {
        return combineLong(data.get(2), data.get(3));
    }

    public boolean isSafeMode() {
        return data.get(4) != 0;
    }

    public boolean isWorking() {
        return data.get(5) != 0;
    }

    public MatterDeconstructorBlockEntity getMachine() {
        return machine;
    }

    private static long combineLong(
            int low,
            int high
    ) {
        return ((long) high << 32)
                | (low & 0xFFFFFFFFL);
    }
}
