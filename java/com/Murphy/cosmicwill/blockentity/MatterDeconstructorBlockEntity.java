package com.Murphy.cosmicwill.blockentity;

import com.Murphy.cosmicwill.block.MatterDeconstructorBlock;
import com.Murphy.cosmicwill.config.CWServerConfig;
import com.Murphy.cosmicwill.data.CWTags;
import com.Murphy.cosmicwill.matter.CWMatterCapabilities;
import com.Murphy.cosmicwill.matter.IMatterStorage;
import com.Murphy.cosmicwill.menu.MatterDeconstructorMenu;
import com.Murphy.cosmicwill.registry.CWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class MatterDeconstructorBlockEntity
        extends BlockEntity
        implements MenuProvider, IMatterStorage {

    public static final int DATA_COUNT = 6;

    private final ItemStackHandler items =
            new ItemStackHandler(1) {
                @Override
                protected void onContentsChanged(int slot) {
                    MatterDeconstructorBlockEntity.this
                            .setChanged();
                }

                @Override
                public boolean isItemValid(
                        int slot,
                        @Nonnull ItemStack stack
                ) {
                    return canDeconstruct(stack);
                }
            };

    private long matter;
    private boolean safeMode = true;

    /**
     * 解构速度很快，普通一组物品可能在一个 Tick 内处理完。
     * 用短暂余辉确保正面点亮效果肉眼可见。
     */
    private int visualWorkTicks;

    private LazyOptional<IItemHandler> itemCapability =
            LazyOptional.empty();

    private LazyOptional<IMatterStorage> matterCapability =
            LazyOptional.empty();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lowInt(matter);
                case 1 -> highInt(matter);
                case 2 -> lowInt(getCapacity());
                case 3 -> highInt(getCapacity());
                case 4 -> safeMode ? 1 : 0;
                case 5 -> isWorking() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {

        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MatterDeconstructorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                CWBlockEntities.MATTER_DECONSTRUCTOR.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MatterDeconstructorBlockEntity machine
    ) {
        if (level.isClientSide) {
            return;
        }

        ItemStack input = machine.items.getStackInSlot(0);

        boolean canRun =
                !level.hasNeighborSignal(pos)
                        && !input.isEmpty()
                        && machine.canDeconstruct(input)
                        && machine.matter < machine.getCapacity();

        boolean didWork = false;

        if (canRun) {
            long remainingCapacity =
                    machine.getCapacity() - machine.matter;

            int consumed = (int) Math.min(
                    Math.min(
                            input.getCount(),
                            CWServerConfig.ITEMS_PER_TICK.get()
                    ),
                    remainingCapacity
            );

            if (consumed > 0) {
                input.shrink(consumed);
                machine.matter += consumed;
                machine.visualWorkTicks = 6;
                machine.setChanged();
                didWork = true;
            }
        }

        if (!didWork && machine.visualWorkTicks > 0) {
            machine.visualWorkTicks--;
        }

        boolean workingNow = machine.visualWorkTicks > 0;

        if (state.getValue(
                MatterDeconstructorBlock.LIT
        ) != workingNow) {
            level.setBlock(
                    pos,
                    state.setValue(
                            MatterDeconstructorBlock.LIT,
                            workingNow
                    ),
                    3
            );
        }
    }

    public boolean canDeconstruct(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.is(
                CWTags.Items
                        .DECONSTRUCTION_ABSOLUTE_BLACKLIST
        )) {
            return false;
        }

        if (!safeMode) {
            return true;
        }

        if (stack.is(
                CWTags.Items.DECONSTRUCTION_PROTECTED
        )) {
            return false;
        }

        if (stack.hasTag()
                || stack.isEnchanted()
                || stack.hasCustomHoverName()
                || stack.isDamaged()) {
            return false;
        }

        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock()
                instanceof ShulkerBoxBlock) {
            return false;
        }

        /*
         * 保护背包、储罐、带内部槽位的模组物品。
         * 某些普通工具也可能暴露能力，因此这里只检查物品存储能力。
         */
        return stack
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(handler -> handler.getSlots() <= 0)
                .orElse(true);
    }

    public ItemStackHandler getItemHandler() {
        return items;
    }

    public boolean isSafeMode() {
        return safeMode;
    }

    public void toggleSafeMode() {
        safeMode = !safeMode;
        setChanged();
    }

    public boolean isWorking() {
        return visualWorkTicks > 0;
    }

    @Override
    public long getMatter() {
        return matter;
    }

    @Override
    public long getCapacity() {
        return CWServerConfig.MATTER_CAPACITY.get();
    }

    @Override
    public long insertMatter(
            long amount,
            boolean simulate
    ) {
        if (amount <= 0L) {
            return 0L;
        }

        long accepted = Math.min(
                amount,
                Math.max(0L, getCapacity() - matter)
        );

        if (!simulate && accepted > 0L) {
            matter += accepted;
            setChanged();
        }

        return accepted;
    }

    @Override
    public long extractMatter(
            long amount,
            boolean simulate
    ) {
        if (amount <= 0L) {
            return 0L;
        }

        long extracted = Math.min(amount, matter);

        if (!simulate && extracted > 0L) {
            matter -= extracted;
            setChanged();
        }

        return extracted;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "container.cosmicwill.matter_deconstructor"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new MatterDeconstructorMenu(
                containerId,
                inventory,
                this,
                data
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putLong("Matter", matter);
        tag.putBoolean("SafeMode", safeMode);
        tag.put("Items", items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        matter = Math.max(0L, tag.getLong("Matter"));

        safeMode = !tag.contains("SafeMode")
                || tag.getBoolean("SafeMode");

        if (tag.contains("Items")) {
            items.deserializeNBT(
                    tag.getCompound("Items")
            );
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        itemCapability = LazyOptional.of(() -> items);
        matterCapability = LazyOptional.of(() -> this);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(
            @Nonnull Capability<T> capability,
            @Nullable Direction side
    ) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }

        if (capability == CWMatterCapabilities.MATTER) {
            return matterCapability.cast();
        }

        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();

        itemCapability.invalidate();
        matterCapability.invalidate();
    }

    private static int lowInt(long value) {
        return (int) value;
    }

    private static int highInt(long value) {
        return (int) (value >>> 32);
    }
}
