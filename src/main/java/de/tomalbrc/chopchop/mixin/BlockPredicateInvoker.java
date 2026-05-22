package de.tomalbrc.chopchop.mixin;

import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockPredicate.class)
public interface BlockPredicateInvoker {
    @Invoker(value = "matchesState")
    boolean invokeMatchesState(BlockState state);
}
