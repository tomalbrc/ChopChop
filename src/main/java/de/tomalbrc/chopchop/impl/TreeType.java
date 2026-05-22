package de.tomalbrc.chopchop.impl;

import de.tomalbrc.chopchop.config.TreeConfig;
import de.tomalbrc.chopchop.mixin.BlockPredicateInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public interface TreeType {
    default boolean isStem(BlockState blockState) {
        return BlockPredicateInvoker.class.cast(getConfig().logFilter).invokeMatchesState(blockState);
    }
    default boolean isLeaf(BlockState blockState, BlockPos blockPos) {
        return getConfig().leavesFilter.isPresent() && BlockPredicateInvoker.class.cast(getConfig().leavesFilter.orElseThrow()).invokeMatchesState(blockState);
    }

    TreeData gatherTreeData(BlockPos blockPos, ServerLevel level, Player player);

    TreeConfig getConfig();
}