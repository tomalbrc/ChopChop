package de.tomalbrc.chopchop.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.tomalbrc.chopchop.Chopchop;
import de.tomalbrc.chopchop.enchantment.Enchantments;
import net.minecraft.advancements.predicates.*;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public class TreeConfig {
    public final boolean enabled;
    public final boolean requireTool;
    public final ItemPredicate allowedToolFilter;
    public final Type type;

    public final BlockPredicate logFilter;
    public final Optional<BlockPredicate> leavesFilter;
    public final Optional<Algorithm> algorithm;

    public enum Type implements StringRepresentable {
        GENERIC("generic"),
        VERTICAL("vertical"),
        CHORUS("chorus"),
        RED_MUSHROOM("red_mushroom"),
        BROWN_MUSHROOM("brown_mushroom");

        final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public @NonNull String getSerializedName() {
            return name;
        }
    }

    private TreeConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.requireTool = builder.requireTool;
        this.allowedToolFilter = builder.allowedToolFilter;
        this.type = builder.type;
        this.logFilter = builder.logFilter;
        this.leavesFilter = builder.leavesFilter;
        this.algorithm = builder.algorithm;
    }

    public static final MapCodec<TreeConfig> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("enabled").orElse(true).forGetter(c -> c.enabled),
                    StringRepresentable.fromEnum(Type::values).fieldOf("type").forGetter(c -> c.type),
                    Codec.BOOL.fieldOf("requireTool").orElse(false).forGetter(c -> c.requireTool),
                    ItemPredicate.CODEC.fieldOf("allowedToolFilter")
                            .orElse(ItemPredicate.Builder.item().build())
                            .forGetter(c -> c.allowedToolFilter),
                    BlockPredicate.CODEC.fieldOf("logFilter")
                            .orElse(BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, BlockTags.LOGS).build())
                            .forGetter(c -> c.logFilter),
                    BlockPredicate.CODEC.optionalFieldOf("leavesFilter")
                            .forGetter(c -> c.leavesFilter),
                    Algorithm.CODEC.optionalFieldOf("algorithm")
                            .forGetter(c -> c.algorithm)
            ).apply(instance, (enabled, type, reqTool, toolFilter, logFilter, leavesFilter, algorithm) ->
                    new Builder(type)
                            .enabled(enabled)
                            .requireTool(reqTool)
                            .allowedToolFilter(toolFilter)
                            .logFilter(logFilter)
                            .leavesFilter(leavesFilter.orElse(null))
                            .algorithm(algorithm)
                            .build()
            )
    );

    public static class Builder {
        private final Type type;
        private boolean enabled = true;
        private boolean requireTool = false;
        private ItemPredicate allowedToolFilter = ItemPredicate.Builder.item().withComponents(DataComponentMatchers.Builder.components().partial(DataComponentPredicates.ENCHANTMENTS, EnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(Chopchop.SERVER.registryAccess().getOrThrow(Enchantments.TREE_FELLER), MinMaxBounds.Ints.atLeast(1))))).build()).build();
        private BlockPredicate logFilter = null;
        private Optional<BlockPredicate> leavesFilter = Optional.empty();
        private Optional<Algorithm> algorithm = Optional.empty();

        public Builder(Type type) {
            this.type = type;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder requireTool(boolean requireTool) {
            this.requireTool = requireTool;
            return this;
        }

        public Builder allowedToolFilter(ItemPredicate filter) {
            this.allowedToolFilter = filter;
            return this;
        }

        public Builder logFilter(BlockPredicate filter) {
            this.logFilter = filter;
            return this;
        }


        public Builder leavesFilter(Optional<BlockPredicate> filter) {
            this.leavesFilter = filter;
            return this;
        }

        public Builder leavesFilter(BlockPredicate filter) {
            this.leavesFilter = Optional.ofNullable(filter);
            return this;
        }

        public Builder algorithm(Optional<Algorithm> algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder algorithm(Algorithm algorithm) {
            this.algorithm = Optional.ofNullable(algorithm);
            return this;
        }


        public TreeConfig build() {
            return new TreeConfig(this);
        }
    }

    public static class Algorithm {
        public int maxLeavesRadius = 7;
        public int maxLogAmount = 256;
        public boolean shouldIgnorePersistentLeaves = true;

        public Algorithm() {}

        public Algorithm(int maxLeavesRadius, int maxLogAmount, boolean shouldIgnorePersistentLeaves) {
            this.maxLeavesRadius = maxLeavesRadius;
            this.maxLogAmount = maxLogAmount;
            this.shouldIgnorePersistentLeaves = shouldIgnorePersistentLeaves;
        }

        public static final Codec<Algorithm> CODEC = RecordCodecBuilder.create(inst ->
                inst.group(
                        Codec.INT.fieldOf("maxLeavesRadius").orElse(7).forGetter(a -> a.maxLeavesRadius),
                        Codec.INT.fieldOf("maxLogAmount").orElse(256).forGetter(a -> a.maxLogAmount),
                        Codec.BOOL.fieldOf("shouldIgnorePersistentLeaves").orElse(true).forGetter(a -> a.shouldIgnorePersistentLeaves)
                ).apply(inst, Algorithm::new)
        );
    }
}