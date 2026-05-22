package de.tomalbrc.chopchop.impl;

import de.tomalbrc.chopchop.config.ModConfig;
import de.tomalbrc.chopchop.util.Util;
import de.tomalbrc.chopchop.config.TreeConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ChorusTree implements TreeType {
	private static final Direction[] HORIZONTAL_DIRECTIONS = Direction.Plane.HORIZONTAL.stream().toList().toArray(new Direction[0]);

    private final TreeConfig config;

    public ChorusTree(TreeConfig config) {
        this.config = config;
    }

    @Override
	public boolean isStem(BlockState blockState) {
        if (!ModConfig.getInstance().animated)
            return false;

        return TreeType.super.isStem(blockState);
	}

	@Override
	public TreeData gatherTreeData(BlockPos blockPos, ServerLevel level, Player player) {
		if (getConfig().requireTool && !getConfig().allowedToolFilter.test(player.getMainHandItem())) return null;

		blockPos = blockPos.immutable();
		TreeData.Builder builder = TreeData.builder();

		Map<BlockPos, TreeData.BlockInfo> blockPosSet = gatherBlocks(level, blockPos, builder, player);
		return builder
				.setOrigin(blockPos)
				.setDirection(Util.direction(player, blockPos))
				.addBlocks(blockPosSet)
				.setToolDamage(blockPosSet.size())
				.setFoodExhaustionModifier(originalExhaustion -> originalExhaustion * blockPosSet.size())
				.setMiningSpeedModifier(originalMiningSpeed -> {
					float speedMultiplication = ModConfig.getInstance().speedMultiplication;
					float multiplyAmount = Math.min(ModConfig.getInstance().maxSpeedMultiplication, ((float) blockPosSet.size() - 1f));
					return originalMiningSpeed / (multiplyAmount * speedMultiplication + 1f);
				})
				.build();
	}

	private Map<BlockPos, TreeData.BlockInfo> gatherBlocks(Level level, BlockPos startPos, TreeData.Builder builder, Player player) {
		Map<BlockPos, TreeData.BlockInfo> blocks = new Object2ObjectOpenHashMap<>();
		Queue<BlockPos> toVisit = new LinkedList<>();
		Set<BlockPos> visited = new HashSet<>();

		toVisit.add(startPos);
		while (!toVisit.isEmpty()) {
			BlockPos current = toVisit.poll();
			if (visited.contains(current)) {
				continue;
			}
			visited.add(current);

			BlockState currentState = level.getBlockState(current);
			if (isLeaf(currentState, current)) {
				blocks.put(current, new TreeData.BlockInfo(currentState, Util.brightness(level, currentState, current)));
				continue;
			}

			if (isStem(currentState)) {
				blocks.put(current, new TreeData.BlockInfo(currentState, Util.brightness(level, currentState, current)));
				builder.addAwardedStat(Stats.BLOCK_MINED.get(currentState.getBlock()));

				if (level instanceof ServerLevel serverLevel)
					builder.addDrops(current, Block.getDrops(currentState, serverLevel, current, null, player, player.getMainHandItem()));

				for (BlockPos neighbor : gatherValidBlocksAround(level, current)) {
					if (!visited.contains(neighbor)) {
						toVisit.add(neighbor);
					}
				}
			}
		}
		return blocks;
	}

	private List<BlockPos> gatherValidBlocksAround(Level level, BlockPos blockPos) {
		List<BlockPos> blocks = new ArrayList<>();
		for (Direction direction : HORIZONTAL_DIRECTIONS) {
			BlockPos neighborPos = blockPos.relative(direction);
			if (isStem(level.getBlockState(neighborPos.below())))
				continue;
			BlockState blockState = level.getBlockState(neighborPos);
			if (isStem(blockState) || isLeaf(blockState, neighborPos))
				blocks.add(neighborPos);
		}

		BlockPos neighborPos = blockPos.above();
		BlockState blockState = level.getBlockState(neighborPos);
		if (isStem(blockState) || isLeaf(blockState, neighborPos))
			blocks.add(neighborPos);
		return blocks;
	}

	public TreeConfig getConfig() {
		return config;
	}
}