package com.Murphy.cosmicwill.loot;

import com.Murphy.cosmicwill.registry.CWBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * 向符合条件的战利品表追加恒星残骸。
 *
 * 具体作用于哪个箱子，由 loot modifier JSON 中的
 * forge:loot_table_id 条件决定。
 */
public final class AddStellarRemnantLootModifier
        extends LootModifier {

    /**
     * Forge 1.20.1 的全局战利品修改器使用 Codec，
     * 不是较新版本接口中的 MapCodec。
     */
    public static final Codec<AddStellarRemnantLootModifier> CODEC =
            RecordCodecBuilder.create(instance ->
                    codecStart(instance)
                            .and(
                                    instance.group(
                                            Codec.floatRange(
                                                            0.0F,
                                                            1.0F
                                                    )
                                                    .fieldOf("chance")
                                                    .forGetter(
                                                            modifier ->
                                                                    modifier.chance
                                                    ),
                                            Codec.intRange(
                                                            1,
                                                            64
                                                    )
                                                    .fieldOf("min")
                                                    .forGetter(
                                                            modifier ->
                                                                    modifier.min
                                                    ),
                                            Codec.intRange(
                                                            1,
                                                            64
                                                    )
                                                    .fieldOf("max")
                                                    .forGetter(
                                                            modifier ->
                                                                    modifier.max
                                                    )
                                    )
                            )
                            .apply(
                                    instance,
                                    AddStellarRemnantLootModifier::new
                            )
            );

    private final float chance;
    private final int min;
    private final int max;

    public AddStellarRemnantLootModifier(
            LootItemCondition[] conditions,
            float chance,
            int min,
            int max
    ) {
        super(conditions);

        this.chance = Mth.clamp(
                chance,
                0.0F,
                1.0F
        );

        this.min = Math.max(1, min);
        this.max = Math.max(this.min, max);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        if (context.getRandom().nextFloat() < chance) {
            int count = Mth.nextInt(
                    context.getRandom(),
                    min,
                    max
            );

            generatedLoot.add(
                    new ItemStack(
                            CWBlocks.STELLAR_REMNANT.get(),
                            count
                    )
            );
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}