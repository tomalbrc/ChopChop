package de.tomalbrc.chopchop.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.stream.Collectors;

public record TreeData(
        BlockPos origin,
        Direction direction,
        Map<BlockPos, BlockInfo> blocks,
        Map<BlockPos, List<ItemStack>> drops,
        List<AwardedStat> awardedStats,
        int toolDamage,
        MiningSpeedModifier miningSpeedModifier,
        FoodExhaustionModifier foodExhaustionModifier
) {
    public record BlockInfo(BlockState blockState, int lightLevel) {
        public static final Codec<BlockInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("block_state").forGetter(BlockInfo::blockState),
                Codec.INT.fieldOf("light_level").forGetter(BlockInfo::lightLevel)
        ).apply(instance, BlockInfo::new));
    }

    public static final MiningSpeedModifier IDENTITY_SPEED = ms -> ms;
    public static final FoodExhaustionModifier IDENTITY_EXHAUSTION = ex -> ex;

    public static final Codec<TreeData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("origin").forGetter(TreeData::origin),
                    Direction.CODEC.fieldOf("direction").forGetter(TreeData::direction),
                    Codec.pair(BlockPos.CODEC.fieldOf("pos").codec(), BlockInfo.CODEC.fieldOf("info").codec())
                            .listOf().xmap(
                                    list -> {
                                        Map<BlockPos, BlockInfo> map = new HashMap<>();
                                        for (var pair : list) map.put(pair.getFirst(), pair.getSecond());
                                        return map;
                                    },
                                    map -> map.entrySet().stream()
                                            .map(e -> Pair.of(e.getKey(), e.getValue()))
                                            .toList()
                            ).fieldOf("blocks").forGetter(TreeData::blocks),

                    Codec.pair(BlockPos.CODEC.fieldOf("pos").codec(), ItemStack.CODEC.listOf().fieldOf("stack").codec())
                            .listOf().xmap(
                                    list -> {
                                        Map<BlockPos, List<ItemStack>> map = new HashMap<>();
                                        for (var pair : list) map.put(pair.getFirst(), pair.getSecond());
                                        return map;
                                    },
                                    map -> map.entrySet().stream()
                                            .map(e -> Pair.of(e.getKey(), e.getValue()))
                                            .toList()
                            ).fieldOf("drops").forGetter(TreeData::drops),

                    Codec.INT.fieldOf("tool_damage").orElse(0).forGetter(TreeData::toolDamage)
            ).apply(instance, (origin, direction, blocks, drops, toolDamage) ->
                    new TreeData(origin, direction, blocks, drops, null, toolDamage,
                            IDENTITY_SPEED, IDENTITY_EXHAUSTION)
            )
    );

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<BlockPos, BlockInfo> blocks = new Object2ObjectOpenHashMap<>();
        private final Map<BlockPos, List<ItemStack>> drops = new Object2ObjectOpenHashMap<>();
        private final Map<Stat<?>, Integer> awardedStats = new HashMap<>();
        private int toolDamage = 0;
        private MiningSpeedModifier miningSpeedModifier = originalMiningSpeed -> originalMiningSpeed;
        private FoodExhaustionModifier foodExhaustionModifier = originalExhaustion -> originalExhaustion;
        private BlockPos origin = BlockPos.ZERO;
        private Direction dir = Direction.NORTH;

        private Builder() {
        }

        public Builder addBlock(BlockPos blockPos, BlockInfo blockInfo) {
            this.blocks.put(blockPos, blockInfo);
            return this;
        }

        public Builder addBlocks(Map<BlockPos, BlockInfo> blocks) {
            blocks.forEach(this::addBlock);
            return this;
        }

        public Builder addDrops(Map<BlockPos, List<ItemStack>> drops) {
            drops.forEach(this::addDrops);
            return this;
        }

        public Builder addDrops(BlockPos pos, List<ItemStack> drops) {
            List<ItemStack> cleanDrops = drops.stream()
                    .filter(stack -> !stack.isEmpty())
                    .toList();
            if (!cleanDrops.isEmpty()) {
                this.drops.computeIfAbsent(pos, _ -> new ObjectArrayList<>()).addAll(cleanDrops);
            }
            return this;
        }

        public Builder addAwardedStat(Stat<?> stat) {
            return this.addAwardedStat(stat, 1);
        }

        public Builder addAwardedStat(Stat<?> stat, int amount) {
            this.awardedStats.compute(stat, (stat1, oldAmount) -> oldAmount == null ? amount : oldAmount + amount);
            return this;
        }

        public <T> Builder addAwardedStats(Collection<Stat<T>> stats) {
            stats.forEach(this::addAwardedStat);
            return this;
        }

        public Builder setToolDamage(int toolDamage) {
            this.toolDamage = toolDamage;
            return this;
        }

        public Builder setMiningSpeedModifier(MiningSpeedModifier miningSpeedModifier) {
            this.miningSpeedModifier = miningSpeedModifier;
            return this;
        }

        public Builder setFoodExhaustionModifier(FoodExhaustionModifier foodExhaustionModifier) {
            this.foodExhaustionModifier = foodExhaustionModifier;
            return this;
        }

        public Builder setOrigin(BlockPos origin) {
            this.origin = origin;
            return this;
        }

        public Builder setDirection(Direction direction) {
            this.dir = direction;
            return this;
        }

        public TreeData build() {
            return new TreeData(
                    origin,
                    dir,
                    Collections.unmodifiableMap(blocks),
                    drops,
                    awardedStats.entrySet().stream().map(entry -> new AwardedStat(entry.getKey(), entry.getValue())).toList(),
                    toolDamage,
                    miningSpeedModifier,
                    foodExhaustionModifier
            );
        }

        public static List<ItemStack> collapseStacks(List<ItemStack> stacks) {
            Map<ItemVariant, Long> counts = new HashMap<>();
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) continue;
                ItemVariant variant = ItemVariant.of(stack);
                counts.put(variant, counts.getOrDefault(variant, 0L) + stack.getCount());
            }

            List<ItemStack> result = new ArrayList<>();
            counts.forEach((variant, total) -> {
                int max = variant.toStack().getMaxStackSize();
                while (total > 0) {
                    int take = (int) Math.min(total, max);
                    ItemStack copy = variant.toStack(take);
                    result.add(copy);
                    total -= take;
                }
            });

            return result;
        }
    }

    @FunctionalInterface
    public interface MiningSpeedModifier {
        float getMiningSpeed(float originalMiningSpeed);
    }

    @FunctionalInterface
    public interface FoodExhaustionModifier {
        float getExhaustion(float originalExhaustion);
    }


    public record AwardedStat(Stat<?> stat, int amount) {

    }
}