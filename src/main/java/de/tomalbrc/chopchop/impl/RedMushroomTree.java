package de.tomalbrc.chopchop.impl;

import de.tomalbrc.chopchop.config.ModConfig;
import de.tomalbrc.chopchop.util.Util;
import de.tomalbrc.chopchop.config.TreeConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class RedMushroomTree implements TreeType {
	private static final BlockPos[] CAP_SCAN_OFFSET = new BlockPos[] {
			new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0),
			new BlockPos(0, 0, -1), new BlockPos(0, 0, 1),
			new BlockPos(-1, -1, 0), new BlockPos(1, -1, 0),
			new BlockPos(0, -1, -1), new BlockPos(0, -1, 1)
	};

    final TreeConfig config;

    public RedMushroomTree(TreeConfig config) {
        this.config = config;
    }

	@Override
	public TreeData gatherTreeData(BlockPos blockPos, ServerLevel level, Player player) {
		if (getConfig().requireTool && !getConfig().allowedToolFilter.test(player.getMainHandItem())) return null;

		blockPos = blockPos.immutable();
		TreeData.Builder builder = TreeData.builder();

		Map<BlockPos, TreeData.BlockInfo> stemBlocks = gatherStemBlocks(level, blockPos);
		Map<BlockPos, TreeData.BlockInfo> capBlocks = new Object2ObjectOpenHashMap<>();

		stemBlocks.keySet().forEach(stemPos -> capBlocks.putAll(gatherCapBlocks(level, stemPos.above())));
		if (capBlocks.isEmpty()) return null;

		Map<BlockPos, TreeData.BlockInfo> blocks = new Object2ObjectArrayMap<>();
		blocks.putAll(stemBlocks);
		blocks.putAll(capBlocks);

		Map<BlockPos, List<ItemStack>> drops = new Object2ObjectArrayMap<>();
		if (level instanceof ServerLevel serverLevel) {
			for (Map.Entry<BlockPos, TreeData.BlockInfo> entry : blocks.entrySet()) {
				List<ItemStack> items = Block.getDrops(entry.getValue().blockState(), serverLevel, entry.getKey(), null, player, player.getMainHandItem());
				drops.put(entry.getKey(), items);
			}
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

	private Map<BlockPos, TreeData.BlockInfo> gatherStemBlocks(Level level, BlockPos startPos) {
		Map<BlockPos, TreeData.BlockInfo> blocks = new Object2ObjectOpenHashMap<>();
		Stack<BlockPos> toVisit = new Stack<>();
		Set<BlockPos> visited = new HashSet<>();

		toVisit.add(startPos);
		while (!toVisit.isEmpty()) {
			BlockPos current = toVisit.pop();
			if (visited.contains(current)) {
				continue;
			}
			visited.add(current);

			BlockState currentState = level.getBlockState(current);
			if (isStem(currentState)) {
				blocks.put(current, new TreeData.BlockInfo(currentState, Util.brightness(level, currentState, current)));

				BlockPos neighbor = current.above();
				if (!visited.contains(neighbor)) {
					toVisit.add(neighbor);
				}
			}
		}
		return blocks;
	}

	private Map<BlockPos, TreeData.BlockInfo> gatherCapBlocks(Level level, BlockPos startPos) {
		Map<BlockPos, TreeData.BlockInfo> blocks = new Object2ObjectOpenHashMap<>();
		Queue<BlockSearchNode> toVisit = new LinkedList<>();
		Set<BlockPos> visited = new HashSet<>();

		toVisit.add(new BlockSearchNode(startPos, 1));
		while (!toVisit.isEmpty()) {
			BlockSearchNode node = toVisit.poll();
			BlockPos current = node.position();

			if (visited.contains(current) || node.distance() > 6) {
				continue;
			}
			visited.add(current);

			BlockState currentState = level.getBlockState(current);
			if (isLeaf(currentState, current)) {
				blocks.put(current, new TreeData.BlockInfo(currentState, Util.brightness(level, currentState, current)));

				for (BlockPos offset : CAP_SCAN_OFFSET) {
					BlockPos neighbor = current.offset(offset);
					if (!visited.contains(neighbor)) {
						toVisit.add(new BlockSearchNode(neighbor, node.distance() + 1));
					}
				}
			}
		}
		return blocks;
	}

	public TreeConfig getConfig() {
		return config;
	}
}