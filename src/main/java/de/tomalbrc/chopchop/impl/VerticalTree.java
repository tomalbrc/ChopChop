package de.tomalbrc.chopchop.impl;

import de.tomalbrc.chopchop.config.ModConfig;
import de.tomalbrc.chopchop.util.Util;
import de.tomalbrc.chopchop.config.TreeConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public class VerticalTree implements TreeType {
    final TreeConfig config;

    public VerticalTree(TreeConfig config) {
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

		Map<BlockPos, TreeData.BlockInfo> blocks = new Object2ObjectOpenHashMap<>();
		gatherBlocks(level, blockPos, blocks);

		Map<BlockPos, List<ItemStack>> drops = new Object2ObjectOpenHashMap<>();
		for (var entry : blocks.entrySet()) {
			List<ItemStack> items = Block.getDrops(entry.getValue().blockState(), level, entry.getKey(), null, player, player.getMainHandItem());
			drops.put(entry.getKey(), items);
		}

		return builder
				.setOrigin(blockPos)
				.setDirection(Util.direction(player, blockPos))
				.addBlocks(blocks)
				.setToolDamage(blocks.size())
				.setFoodExhaustionModifier(originalExhaustion -> originalExhaustion * blocks.size())
				.addDrops(drops)
				.setMiningSpeedModifier(originalMiningSpeed -> {
					float speedMultiplication = ModConfig.getInstance().speedMultiplication;
					float multiplyAmount = Math.min(ModConfig.getInstance().maxSpeedMultiplication, ((float) blocks.size() - 1f));
					return originalMiningSpeed / (multiplyAmount * speedMultiplication + 1f);
				})
				.addAwardedStats(blocks.values().stream().map(blockInfo -> Stats.BLOCK_MINED.get(blockInfo.blockState().getBlock())).toList())
				.build();
	}

	private void gatherBlocks(Level level, BlockPos blockPos, Map<BlockPos, TreeData.BlockInfo> stateMap) {
		BlockState blockState = level.getBlockState(blockPos);
		stateMap.put(blockPos, new TreeData.BlockInfo(blockState, Util.brightness(level, blockState, blockPos)));

		BlockPos neighborPos = blockPos.above();
		if (level.getBlockState(neighborPos).is(blockState.getBlock()))
			gatherBlocks(level, neighborPos, stateMap);
	}

	public TreeConfig getConfig() {
		return config;
	}
}