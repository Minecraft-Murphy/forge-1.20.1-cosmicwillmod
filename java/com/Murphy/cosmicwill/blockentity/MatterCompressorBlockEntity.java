package com.Murphy.cosmicwill.blockentity;

import com.Murphy.cosmicwill.CWitems;
import com.Murphy.cosmicwill.block.MatterCompressorBlock;
import com.Murphy.cosmicwill.matter.CWMatterCapabilities;
import com.Murphy.cosmicwill.matter.IMatterStorage;
import com.Murphy.cosmicwill.menu.MatterCompressorMenu;
import com.Murphy.cosmicwill.recipe.MatterCompressionRecipe;
import com.Murphy.cosmicwill.registry.CWBlockEntities;
import com.Murphy.cosmicwill.registry.CWRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MatterCompressorBlockEntity
        extends BlockEntity
        implements MenuProvider {

    /**
     * 压缩器每 Tick 最多调用的物质当量。
     *
     * 500,000 MU 的配方至少需要 5,000 Tick（250 秒）完成能量输入。
     */
    public static final long MATTER_PER_TICK = 100L;

    private final ItemStackHandler anchorInventory =
            new ItemStackHandler(1) {

                @Override
                public int getSlotLimit(int slot) {
                    return 1;
                }

                @Override
                public boolean isItemValid(
                        int slot,
                        @Nonnull ItemStack stack
                ) {
                    return !isProcessing()
                            && !hasInstalledSingularity();
                }

                @Override
                public ItemStack insertItem(
                        int slot,
                        @Nonnull ItemStack stack,
                        boolean simulate
                ) {
                    if (!internalAnchorMutation
                            && (isProcessing()
                            || hasInstalledSingularity())) {
                        return stack;
                    }

                    return super.insertItem(
                            slot,
                            stack,
                            simulate
                    );
                }

                @Override
                @Nonnull
                public ItemStack extractItem(
                        int slot,
                        int amount,
                        boolean simulate
                ) {
                    if (!internalAnchorMutation
                            && (isProcessing()
                            || hasInstalledSingularity())) {
                        return ItemStack.EMPTY;
                    }

                    return super.extractItem(
                            slot,
                            amount,
                            simulate
                    );
                }

                @Override
                protected void onContentsChanged(int slot) {
                    syncSingularityStateFromAnchor();
                    setChangedAndSync();
                }
            };

    private LazyOptional<IItemHandler> itemCapability =
            LazyOptional.empty();

    /**
     * 世界中显示、被上下压板加工的物品。
     */
    private ItemStack workpiece = ItemStack.EMPTY;

    @Nullable
    private ResourceLocation activeRecipeId;

    private int progress;
    private long consumedMatter;
    private boolean resultReady;

    /**
     * 允许机器内部在配方完成时安装或消耗锚点，
     * 同时继续阻止 GUI、漏斗和玩家在作业中修改锚点。
     */
    private boolean internalAnchorMutation;

    public MatterCompressorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                CWBlockEntities.MATTER_COMPRESSOR.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MatterCompressorBlockEntity machine
    ) {
        if (level.isClientSide) {
            return;
        }

        if (machine.hasInstalledSingularity()
                && level instanceof ServerLevel serverLevel
                && level.getGameTime() % 2L == 0L) {
            serverLevel.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.65D,
                    pos.getZ() + 0.5D,
                    2,
                    0.35D,
                    0.25D,
                    0.35D,
                    0.01D
            );

            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.55D,
                    pos.getZ() + 0.5D,
                    1,
                    0.25D,
                    0.20D,
                    0.25D,
                    0.005D
            );
        }

        if (machine.workpiece.isEmpty()
                || machine.resultReady) {
            machine.setWorking(false);
            return;
        }

        MatterCompressionRecipe recipe =
                machine.resolveRecipe();

        if (recipe == null) {
            machine.setWorking(false);
            return;
        }

        int minimumDuration = Math.max(
                1,
                recipe.getProcessingTime()
        );

        long totalMatterCost = Math.max(
                0L,
                recipe.getMatterCost()
        );

        long remainingMatter = Math.max(
                0L,
                totalMatterCost
                        - machine.consumedMatter
        );

        /*
         * MU 尚未输入完时，每 Tick 最多抽取 100 MU。
         *
         * 相邻解构器即使只剩 37 MU，也会先抽取这 37 MU并保存进度；
         * 玩家补充 MU 后从原进度继续，不会回滚。
         */
        if (remainingMatter > 0L) {
            long requested = Math.min(
                    MATTER_PER_TICK,
                    remainingMatter
            );

            long extracted =
                    machine.extractMatterUpTo(
                            requested
                    );

            if (extracted <= 0L) {
                machine.setWorking(false);
                return;
            }

            machine.consumedMatter += extracted;
        }

        /*
         * processing_time 仍作为最短机械作业时间。
         *
         * 例如测试配方只有 10 MU，会在第一 Tick 输入完 MU，
         * 但上下压板仍会完成 JSON 中设定的 60 Tick 动画。
         * 高成本配方则由 100 MU/Tick 的输入速度决定更长时间。
         */
        machine.progress = Math.min(
                minimumDuration,
                machine.progress + 1
        );

        machine.setWorking(true);
        machine.setChanged();

        boolean matterComplete =
                machine.consumedMatter
                        >= totalMatterCost;

        boolean animationComplete =
                machine.progress
                        >= minimumDuration;

        if (matterComplete && animationComplete) {
            machine.completeRecipe(recipe);
        }
    }

    @Nullable
    private MatterCompressionRecipe resolveRecipe() {
        if (level == null || workpiece.isEmpty()) {
            return null;
        }

        if (activeRecipeId != null) {
            Optional<MatterCompressionRecipe> active =
                    level.getRecipeManager()
                            .byKey(activeRecipeId)
                            .filter(
                                    MatterCompressionRecipe.class
                                            ::isInstance
                            )
                            .map(
                                    MatterCompressionRecipe.class
                                            ::cast
                            );

            if (active.isPresent()
                    && active.get().matches(
                    createRecipeInput(),
                    level
            )) {
                return active.get();
            }

            /*
             * 配方文件被重载或锚点被外部异常修改时，安全地停止旧进度。
             */
            activeRecipeId = null;
            progress = 0;
            consumedMatter = 0L;
        }

        Optional<MatterCompressionRecipe> found =
                level.getRecipeManager()
                        .getRecipeFor(
                                CWRecipeTypes
                                        .MATTER_COMPRESSING
                                        .get(),
                                createRecipeInput(),
                                level
                        );

        if (found.isEmpty()) {
            return null;
        }

        MatterCompressionRecipe recipe = found.get();
        activeRecipeId = recipe.getId();
        progress = 0;
        consumedMatter = 0L;
        setChangedAndSync();
        return recipe;
    }

    private Container createRecipeInput() {
        return new SimpleContainer(
                workpiece.copy(),
                anchorInventory
                        .getStackInSlot(0)
                        .copy()
        );
    }

    private void completeRecipe(
            MatterCompressionRecipe recipe
    ) {
        ItemStack remainder =
                workpiece.copy();

        remainder.shrink(
                recipe.getInputCount()
        );

        ItemStack result =
                recipe.getResult();

        /*
         * 微型恒星压缩完成后，不把“人造奇点”作为可取出的成品。
         * 它会直接进入隐藏锚点槽，并把整台机器切换为奇点形态。
         */
        if (result.is(
                CWitems.ARTIFICIAL_SINGULARITY.get()
        )) {
            installSingularityInternally();
            ejectRemainder(remainder);
            return;
        }

        workpiece = result;

        if (recipe.shouldConsumeAnchor()) {
            consumeAnchorInternally();
        }

        resultReady = true;
        activeRecipeId = null;
        progress = 0;
        consumedMatter = 0L;
        setWorking(false);

        /*
         * 最终归零者配方会消耗人造奇点。
         * 锚点消失后，机器恢复普通外观、GUI 与可挖掘状态。
         */
        syncSingularityStateFromAnchor();
        ejectRemainder(remainder);
        setChangedAndSync();
    }

    private void ejectRemainder(
            ItemStack remainder
    ) {
        if (remainder.isEmpty()
                || level == null
                || level.isClientSide) {
            return;
        }

        ItemEntity entity =
                new ItemEntity(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.75D,
                        worldPosition.getZ() + 0.5D,
                        remainder
                );

        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    private void installSingularityInternally() {
        internalAnchorMutation = true;

        try {
            anchorInventory.setStackInSlot(
                    0,
                    new ItemStack(
                            CWitems.ARTIFICIAL_SINGULARITY.get()
                    )
            );
        } finally {
            internalAnchorMutation = false;
        }

        workpiece = ItemStack.EMPTY;
        resultReady = false;
        activeRecipeId = null;
        progress = 0;
        consumedMatter = 0L;
        setWorking(false);
        setSingularityState(true);
        setChangedAndSync();
    }

    private void consumeAnchorInternally() {
        internalAnchorMutation = true;

        try {
            anchorInventory.extractItem(
                    0,
                    1,
                    false
            );
        } finally {
            internalAnchorMutation = false;
        }
    }

    public void handleShiftInteraction(
            Player player,
            InteractionHand hand
    ) {
        ItemStack held =
                player.getItemInHand(hand);

        if (workpiece.isEmpty()) {
            if (held.isEmpty()) {
                return;
            }

            int amount =
                    determineInitialInsertionAmount(
                            held
                    );

            workpiece =
                    held.copyWithCount(amount);

            if (!player.getAbilities().instabuild) {
                held.shrink(amount);
            }

            clearProcessState();

            player.displayClientMessage(
                    Component.translatable(
                            "message.cosmicwill.matter_compressor.inserted",
                            workpiece.getHoverName()
                    ),
                    true
            );

            setChangedAndSync();
            return;
        }

        if (!held.isEmpty()) {
            if (isProcessing() || resultReady) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.cosmicwill.matter_compressor.locked"
                        ),
                        true
                );
                return;
            }

            if (!ItemStack.isSameItemSameTags(
                    workpiece,
                    held
            )) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.cosmicwill.matter_compressor.occupied"
                        ),
                        true
                );
                return;
            }

            int amount =
                    calculateMergeAmount(held);

            if (amount <= 0) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.cosmicwill.matter_compressor.stack_full"
                        ),
                        true
                );
                return;
            }

            workpiece.grow(amount);

            if (!player.getAbilities().instabuild) {
                held.shrink(amount);
            }

            clearProcessState();
            setChangedAndSync();
            return;
        }

        if (isProcessing()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.cosmicwill.matter_compressor.locked"
                    ),
                    true
            );
            return;
        }

        ItemStack returned =
                workpiece.copy();

        workpiece =
                ItemStack.EMPTY;

        clearProcessState();
        player.setItemInHand(
                hand,
                returned
        );

        player.displayClientMessage(
                Component.translatable(
                        "message.cosmicwill.matter_compressor.removed",
                        returned.getHoverName()
                ),
                true
        );

        setChangedAndSync();
    }

    private int determineInitialInsertionAmount(
            ItemStack held
    ) {
        int preferred =
                findPreferredInputCount(held);

        if (preferred > 0
                && held.getCount() >= preferred) {
            return preferred;
        }

        return Math.min(
                held.getCount(),
                held.getMaxStackSize()
        );
    }

    private int calculateMergeAmount(
            ItemStack held
    ) {
        int maximum =
                workpiece.getMaxStackSize();

        ItemStack combined =
                workpiece.copy();

        combined.setCount(
                Math.min(
                        maximum,
                        workpiece.getCount()
                                + held.getCount()
                )
        );

        int preferred =
                findPreferredInputCount(combined);

        int target = preferred > 0
                ? Math.min(preferred, maximum)
                : maximum;

        int available =
                Math.max(
                        0,
                        target - workpiece.getCount()
                );

        return Math.min(
                available,
                held.getCount()
        );
    }

    /**
     * 忽略锚点，只根据输入物品种类寻找在当前手持数量内
     * 可满足的最大配方数量。
     *
     * 例如：
     * - 煤炭配方需要 1 个，手持 64 个时只吸入 1 个；
     * - 深板岩配方需要 64 个，手持 32 个时先吸入 32 个，
     *   再次潜行右键可补足到 64 个。
     */
    private int findPreferredInputCount(
            ItemStack candidate
    ) {
        if (level == null
                || candidate.isEmpty()) {
            return 0;
        }

        int best = 0;

        for (MatterCompressionRecipe recipe
                : level.getRecipeManager()
                .getAllRecipesFor(
                        CWRecipeTypes
                                .MATTER_COMPRESSING
                                .get()
                )) {
            if (!recipe.matchesInputItem(candidate)) {
                continue;
            }

            int count =
                    recipe.getInputCount();

            if (count <= candidate.getCount()) {
                best = Math.max(best, count);
            }
        }

        return best;
    }

    public boolean hasInstalledSingularity() {
        return anchorInventory
                .getStackInSlot(0)
                .is(
                        CWitems.ARTIFICIAL_SINGULARITY.get()
                );
    }

    private void syncSingularityStateFromAnchor() {
        if (level == null || level.isClientSide) {
            return;
        }

        setSingularityState(
                hasInstalledSingularity()
        );
    }

    private void setSingularityState(
            boolean singularity
    ) {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(
                MatterCompressorBlock.SINGULARITY
        )) {
            return;
        }

        if (state.getValue(
                MatterCompressorBlock.SINGULARITY
        ) == singularity) {
            return;
        }

        level.setBlock(
                worldPosition,
                state.setValue(
                        MatterCompressorBlock.SINGULARITY,
                        singularity
                ),
                Block.UPDATE_CLIENTS
        );
    }

    private void clearProcessState() {
        activeRecipeId = null;
        progress = 0;
        consumedMatter = 0L;
        resultReady = false;
        setWorking(false);
    }

    public boolean isProcessing() {
        return activeRecipeId != null
                && !resultReady
                && !workpiece.isEmpty();
    }

    public ItemStackHandler getAnchorInventory() {
        return anchorInventory;
    }

    public ItemStack getRenderedStack() {
        return workpiece;
    }

    public boolean isResultReady() {
        return resultReady;
    }

    public int getProgress() {
        return progress;
    }

    public long getConsumedMatter() {
        return consumedMatter;
    }

    private long getAvailableMatter() {
        long available = 0L;

        for (IMatterStorage storage
                : getAdjacentMatterStorages()) {
            long stored = Math.max(
                    0L,
                    storage.getMatter()
            );

            if (Long.MAX_VALUE - available < stored) {
                return Long.MAX_VALUE;
            }

            available += stored;
        }

        return available;
    }

    /**
     * 从所有相邻物质储存中合计抽取，单 Tick 允许部分抽取。
     *
     * @return 本 Tick 实际抽取的 MU。
     */
    private long extractMatterUpTo(long maximum) {
        if (maximum <= 0L) {
            return 0L;
        }

        long remaining = maximum;
        long extractedTotal = 0L;

        for (IMatterStorage storage
                : getAdjacentMatterStorages()) {
            long extracted = storage.extractMatter(
                    remaining,
                    false
            );

            extractedTotal += extracted;
            remaining -= extracted;

            if (remaining <= 0L) {
                break;
            }
        }

        return extractedTotal;
    }

    private List<IMatterStorage>
    getAdjacentMatterStorages() {
        List<IMatterStorage> result =
                new ArrayList<>();

        if (level == null) {
            return result;
        }

        for (Direction direction
                : Direction.values()) {
            BlockEntity neighbor =
                    level.getBlockEntity(
                            worldPosition.relative(direction)
                    );

            if (neighbor == null) {
                continue;
            }

            neighbor.getCapability(
                            CWMatterCapabilities.MATTER,
                            direction.getOpposite()
                    )
                    .resolve()
                    .ifPresent(result::add);
        }

        return result;
    }

    private void setWorking(boolean working) {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(
                MatterCompressorBlock.WORKING
        )) {
            return;
        }

        if (state.getValue(
                MatterCompressorBlock.WORKING
        ) == working) {
            return;
        }

        level.setBlock(
                worldPosition,
                state.setValue(
                        MatterCompressorBlock.WORKING,
                        working
                ),
                Block.UPDATE_CLIENTS
        );
    }

    private void setChangedAndSync() {
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "container.cosmicwill.matter_compressor"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        if (hasInstalledSingularity()) {
            return null;
        }

        return new MatterCompressorMenu(
                containerId,
                inventory,
                this
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(
                "AnchorInventory",
                anchorInventory.serializeNBT()
        );

        if (!workpiece.isEmpty()) {
            tag.put(
                    "Workpiece",
                    workpiece.save(
                            new CompoundTag()
                    )
            );
        }

        tag.putInt("Progress", progress);
        tag.putLong(
                "ConsumedMatter",
                consumedMatter
        );
        tag.putBoolean(
                "ResultReady",
                resultReady
        );

        if (activeRecipeId != null) {
            tag.putString(
                    "ActiveRecipe",
                    activeRecipeId.toString()
            );
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("AnchorInventory")) {
            anchorInventory.deserializeNBT(
                    tag.getCompound(
                            "AnchorInventory"
                    )
            );
        }

        workpiece = tag.contains("Workpiece")
                ? ItemStack.of(
                        tag.getCompound("Workpiece")
                )
                : ItemStack.EMPTY;

        progress = Math.max(
                0,
                tag.getInt("Progress")
        );

        consumedMatter = Math.max(
                0L,
                tag.getLong("ConsumedMatter")
        );

        resultReady = tag.getBoolean(
                "ResultReady"
        );

        activeRecipeId = tag.contains(
                "ActiveRecipe"
        )
                ? ResourceLocation.tryParse(
                        tag.getString("ActiveRecipe")
                )
                : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        itemCapability = LazyOptional.of(
                () -> anchorInventory
        );

        syncSingularityStateFromAnchor();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(
            @Nonnull Capability<T> capability,
            @Nullable Direction side
    ) {
        if (capability
                == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }

        return super.getCapability(
                capability,
                side
        );
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket
                .create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet
    ) {
        CompoundTag tag = packet.getTag();

        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag
    ) {
        load(tag);
    }
}
