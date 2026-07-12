package com.Murphy.cosmicwill.items;

import com.Murphy.cosmicwill.nullification.NullificationManager;
import com.Murphy.cosmicwill.nullification.NullifierTargeting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 归零者之剑。
 *
 * I：毁灭左键——自然距离单体，结算奖励后终结。
 * II：毁灭右键——自然距离前方范围，逐个结算后终结。
 * III：归零左键——自然距离单体，闪红后下一 Tick 无奖励抹除。
 * IV：归零右键——160 格视线单体，立即无奖励抹除。
 */
public class NullifierSwordItem extends Item {

    private static final String MODE_TAG = "NullifierMode";
    private static final String PREVIOUS_MODE_TAG = "NullifierPreviousMode";
    private static final String TRANSITION_START_TAG = "NullifierTransitionStart";

    /** 20 Tick = 1 秒。 */
    public static final int TRANSITION_TICKS = 20;

    public static final int MODE_DESTRUCTION = 0;
    public static final int MODE_NULLIFICATION = 1;
    public static final double NULLIFICATION_RANGE = 160.0D;

    public NullifierSwordItem(Properties properties) {
        super(properties);
    }

    public static int getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return MODE_DESTRUCTION;
        }
        return tag.getInt(MODE_TAG) == MODE_NULLIFICATION
                ? MODE_NULLIFICATION
                : MODE_DESTRUCTION;
    }

    /**
     * 立即设置模式，不播放过渡。保留这个重载，供命令或兼容代码使用。
     */
    public static void setMode(ItemStack stack, int mode) {
        int safeMode = mode == MODE_NULLIFICATION
                ? MODE_NULLIFICATION
                : MODE_DESTRUCTION;

        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(MODE_TAG, safeMode);
        tag.putInt(PREVIOUS_MODE_TAG, safeMode);
        tag.remove(TRANSITION_START_TAG);
    }

    /**
     * 开始一次有 1 秒视觉过渡的模式切换。
     */
    public static void beginModeTransition(
            ItemStack stack,
            int nextMode,
            long gameTime
    ) {
        int currentMode = getMode(stack);
        int safeNextMode = nextMode == MODE_NULLIFICATION
                ? MODE_NULLIFICATION
                : MODE_DESTRUCTION;

        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(PREVIOUS_MODE_TAG, currentMode);
        tag.putInt(MODE_TAG, safeNextMode);
        tag.putLong(TRANSITION_START_TAG, gameTime);
    }

    public static int getPreviousMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(PREVIOUS_MODE_TAG)) {
            return getMode(stack);
        }

        return tag.getInt(PREVIOUS_MODE_TAG) == MODE_NULLIFICATION
                ? MODE_NULLIFICATION
                : MODE_DESTRUCTION;
    }

    public static float getTransitionProgress(
            ItemStack stack,
            @Nullable Level level
    ) {
        CompoundTag tag = stack.getTag();
        if (tag == null
                || level == null
                || !tag.contains(TRANSITION_START_TAG)) {
            return 1.0F;
        }

        long elapsed = level.getGameTime() - tag.getLong(TRANSITION_START_TAG);
        if (elapsed <= 0L) {
            return 0.0F;
        }
        if (elapsed >= TRANSITION_TICKS) {
            return 1.0F;
        }

        return elapsed / (float) TRANSITION_TICKS;
    }

    public static boolean isTransitioning(
            ItemStack stack,
            @Nullable Level level
    ) {
        return getPreviousMode(stack) != getMode(stack)
                && getTransitionProgress(stack, level) < 1.0F;
    }


    public static int getVisualState(
            ItemStack stack,
            @Nullable Level level
    ) {
        int currentMode = getMode(stack);
        int previousMode = getPreviousMode(stack);
        float progress = getTransitionProgress(stack, level);

        if (previousMode == currentMode || progress >= 1.0F) {
            return currentMode == MODE_NULLIFICATION ? 11 : 0;
        }

        int frame = Math.min(9, (int) (progress * 10.0F));

        if (previousMode == MODE_DESTRUCTION
                && currentMode == MODE_NULLIFICATION) {
            return 1 + frame;
        }

        if (previousMode == MODE_NULLIFICATION
                && currentMode == MODE_DESTRUCTION) {
            return 12 + frame;
        }

        return currentMode == MODE_NULLIFICATION ? 11 : 0;
    }

    private static Component getModeName(int mode) {
        if (mode == MODE_NULLIFICATION) {
            return Component.translatable(
                    "mode.cosmicwill.nullifier.nullification"
            ).withStyle(
                    ChatFormatting.DARK_PURPLE,
                    ChatFormatting.BOLD
            );
        }

        return Component.translatable(
                "mode.cosmicwill.nullifier.destruction"
        ).withStyle(
                ChatFormatting.RED,
                ChatFormatting.BOLD
        );
    }

    public static void toggleModeAndNotify(
            ItemStack stack,
            Player player
    ) {
        if (player.level().isClientSide) {
            return;
        }

        int nextMode = getMode(stack) == MODE_DESTRUCTION
                ? MODE_NULLIFICATION
                : MODE_DESTRUCTION;

        beginModeTransition(
                stack,
                nextMode,
                player.level().getGameTime()
        );

        player.displayClientMessage(
                Component.translatable(
                        "message.cosmicwill.nullifier.mode_switched",
                        getModeName(nextMode)
                ).withStyle(ChatFormatting.GRAY),
                true
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                toggleModeAndNotify(stack, player);
            }
            return InteractionResultHolder.sidedSuccess(
                    stack,
                    level.isClientSide
            );
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(
                    stack,
                    level.isClientSide
            );
        }

        if (getMode(stack) == MODE_DESTRUCTION) {
            NullifierTargeting.performDestructionSweep(
                    serverPlayer,
                    null
            );
            serverPlayer.swing(hand, true);
            return InteractionResultHolder.success(stack);
        }

        Entity target = NullifierTargeting.findEntityInSight(
                serverPlayer,
                NULLIFICATION_RANGE
        );

        if (target != null && target != serverPlayer) {
            NullificationManager.conceptErase(
                    target,
                    serverPlayer
            );
            serverPlayer.swing(hand, true);
        } else {
            serverPlayer.displayClientMessage(
                    Component.translatable(
                            "message.cosmicwill.nullifier.no_target"
                    ).withStyle(ChatFormatting.DARK_GRAY),
                    true
            );
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean onLeftClickEntity(
            ItemStack stack,
            Player player,
            Entity rawTarget
    ) {
        if (player.level().isClientSide) {
            return false;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        Entity target = NullifierTargeting.resolveParent(rawTarget);
        if (target == null || target == serverPlayer) {
            return true;
        }

        if (getMode(stack) == MODE_DESTRUCTION) {
            NullificationManager.settleAndDestroy(
                    target,
                    serverPlayer
            );
        } else {
            NullificationManager.ruleNullify(
                    target,
                    serverPlayer
            );
        }

        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity rawTarget,
            InteractionHand hand
    ) {
        if (player.isShiftKeyDown()) {
            if (!player.level().isClientSide) {
                toggleModeAndNotify(stack, player);
            }
            return InteractionResult.sidedSuccess(
                    player.level().isClientSide
            );
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        Entity target = NullifierTargeting.resolveParent(rawTarget);
        if (target == null || target == serverPlayer) {
            return InteractionResult.FAIL;
        }

        if (getMode(stack) == MODE_DESTRUCTION) {
            NullifierTargeting.performDestructionSweep(
                    serverPlayer,
                    target
            );
        } else {
            NullificationManager.conceptErase(
                    target,
                    serverPlayer
            );
        }

        serverPlayer.swing(hand, true);
        return InteractionResult.CONSUME;
    }


    @Override
    public boolean onBlockStartBreak(
            ItemStack stack,
            BlockPos pos,
            Player player
    ) {
        /*
         * 客户端必须返回 false，让原版把开始破坏方块的数据包
         * 发送给服务器。服务器侧由 CosmicBlockBreakEvents
         * 立即执行毁灭或归零。
         */
        if (player.level().isClientSide) {
            return false;
        }

        if (!(player.level()
                instanceof ServerLevel serverLevel)) {
            return true;
        }

        BlockState state =
                serverLevel.getBlockState(pos);

        if (state.isAir()) {
            return true;
        }

        if (getMode(stack) == MODE_DESTRUCTION) {
            /*
             * 原版基岩没有正常采掘掉落，因此单独补出方块本体。
             */
            if (state.is(Blocks.BEDROCK)) {
                Block.popResource(
                        serverLevel,
                        pos,
                        new ItemStack(Blocks.BEDROCK)
                );
            }

            serverLevel.destroyBlock(
                    pos,
                    true,
                    player
            );
        } else {
            /*
             * 先清除方块实体，避免箱子内容、机器库存或潜影盒内容掉出。
             */
            serverLevel.removeBlockEntity(pos);

            serverLevel.setBlock(
                    pos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
                            | Block.UPDATE_SUPPRESS_DROPS
            );
        }

        return true;
    }

    @Override
    public Component getName(
            ItemStack stack
    ) {
        return Component.translatable(
                getDescriptionId(stack)
        ).withStyle(ChatFormatting.RED);
    }

    @Override
    public Rarity getRarity(
            ItemStack stack
    ) {
        return Rarity.EPIC;
    }


    @Override
    public float getDestroySpeed(
            ItemStack stack,
            BlockState state
    ) {
        return Float.MAX_VALUE;
    }

    @Override
    public boolean isCorrectToolForDrops(
            ItemStack stack,
            BlockState state
    ) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getMode(stack) == MODE_NULLIFICATION;
    }

    @Override
    public boolean shouldCauseReequipAnimation(
            ItemStack oldStack,
            ItemStack newStack,
            boolean slotChanged
    ) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        int mode = getMode(stack);

        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.nullifier.txt"
                ).withStyle(ChatFormatting.DARK_RED)
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.nullifier.current_mode"
                ).withStyle(ChatFormatting.GRAY)
                        .append(getModeName(mode))
        );

        tooltip.add(Component.empty());

        if (mode == MODE_DESTRUCTION) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.cosmicwill.destruction.left_detail"
                    ).withStyle(ChatFormatting.DARK_GRAY)
            );
            tooltip.add(
                    Component.translatable(
                            "tooltip.cosmicwill.destruction.right_detail"
                    ).withStyle(ChatFormatting.DARK_GRAY)
            );
        } else {
            tooltip.add(
                    Component.translatable(
                            "tooltip.cosmicwill.nullification.left_detail"
                    ).withStyle(ChatFormatting.DARK_GRAY)
            );
            tooltip.add(
                    Component.translatable(
                            "tooltip.cosmicwill.nullification.right_detail"
                    ).withStyle(ChatFormatting.DARK_GRAY)
            );
        }
        tooltip.add(
                Component.translatable(
                        mode == MODE_DESTRUCTION
                                ? "tooltip.cosmicwill.nullifier.mining_destruction"
                                : "tooltip.cosmicwill.nullifier.mining_nullification"
                ).withStyle(ChatFormatting.DARK_GRAY)
        );

        tooltip.add(Component.empty());
        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.nullifier.mode_switched"
                ).withStyle(ChatFormatting.GRAY)
        );


        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.nullifier.quote"
                ).withStyle(
                        ChatFormatting.DARK_GRAY,
                        ChatFormatting.ITALIC
                )
        );
    }
}
