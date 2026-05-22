package de.tomalbrc.chopchop.impl;

import de.tomalbrc.chopchop.config.ModConfig;
import de.tomalbrc.chopchop.util.Util;
import de.tomalbrc.chopchop.config.TreeConfig;
import de.tomalbrc.chopchop.mixin.BlockPredicateInvoker;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;

public class GenericTree implements TreeType {
    final TreeConfig config;

    public GenericTree(TreeConfig config) {
        this.config = config;
    }

    @Override
    public TreeData gatherTreeData(BlockPos blockPos, ServerLevel level, Player player) {
        if (getConfig().requireTool && !getConfig().allowedToolFilter.test(player.getMainHandItem())) return null;

        blockPos = blockPos.immutable();
        TreeData.Builder builder = TreeData.builder();

        if (!isLogBlock(level.getBlockState(blockPos))) {
            return null;
        }

        Map<BlockPos, TreeData.BlockInfo> logs = gatherLogs(level, blockPos);
        if (logs.isEmpty()) {
            return null;
        }

        Map<BlockPos, TreeData.BlockInfo> leaves = new Object2ObjectOpenHashMap<>();
        for (BlockPos logPos : logs.keySet()) {
            leaves.putAll(gatherLeavesAroundLog(level, logPos));
        }
        if (leaves.isEmpty()) {
            return null;
        }

        Map<BlockPos, TreeData.BlockInfo> adjacent = gatherAdjacentBlocks(level, logs, leaves);

        Map<BlockPos, TreeData.BlockInfo> allBlocks = new Object2ObjectOpenHashMap<>(logs);
        allBlocks.putAll(leaves);
        allBlocks.putAll(adjacent);

        Map<BlockPos, List<ItemStack>> drops = new Object2ObjectOpenHashMap<>();
        for (BlockPos block : allBlocks.keySet()) {
            BlockState blockState = level.getBlockState(block);
            if (level instanceof ServerLevel serverLevel) {
                drops.put(block, Block.getDrops(blockState, serverLevel, block, null, player, player.getMainHandItem()));
            }
        }

        return builder
                .setOrigin(blockPos)
                .setDirection(Util.direction(player, blockPos))
                .addBlocks(allBlocks)
                .setToolDamage(logs.size())
                .setFoodExhaustionModifier(originalExhaustion -> originalExhaustion * logs.size())
                .addDrops(drops)
                .setMiningSpeedModifier(originalMiningSpeed -> {
                    float speedMultiplication = ModConfig.getInstance().speedMultiplication;
                    float multiplyAmount = Math.min(ModConfig.getInstance().maxSpeedMultiplication, ((float) logs.size() - 1f));
                    return originalMiningSpeed / (multiplyAmount * speedMultiplication + 1f);
                })
                .addAwardedStats(logs.values().stream().map(info -> Stats.BLOCK_MINED.get(info.blockState().getBlock())).toList())
                .build();
    }

    private Map<BlockPos, TreeData.BlockInfo> gatherLogs(Level level, BlockPos startPos) {
        Map<BlockPos, TreeData.BlockInfo> logs = new Object2ObjectOpenHashMap<>();
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
            if (isLogBlock(currentState)) {
                logs.put(current, new TreeData.BlockInfo(currentState, Util.brightness(level, currentState, current)));

                if (logs.size() > getConfig().algorithm.orElseThrow().maxLogAmount) {
                    throw new IllegalStateException();
                }

                for (BlockPos offset : BlockPos.betweenClosed(-1, 0, -1, 1, 1, 1)) {
                    BlockPos neighbor = current.offset(offset);
                    if (!visited.contains(neighbor)) {
                        toVisit.add(neighbor);
                    }
                }
            }
        }
        return logs;
    }

    private Map<BlockPos, TreeData.BlockInfo> gatherLeavesAroundLog(Level level, BlockPos logPos) {
        Map<BlockPos, TreeData.BlockInfo> leaves = new Object2ObjectOpenHashMap<>();
        Queue<BlockSearchNode> toVisit = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        for (Direction direction : Direction.values()) {
            BlockPos neighbor = logPos.relative(direction);
            toVisit.add(new BlockSearchNode(neighbor, 1));
        }

        while (!toVisit.isEmpty()) {
            BlockSearchNode node = toVisit.poll();
            BlockPos current = node.position();

            BlockState currentState = level.getBlockState(current);
            OptionalInt optionalDistanceAt = LeavesBlock.getOptionalDistanceAt(currentState);
            if (node.distance() != optionalDistanceAt.orElse(0)) {
                continue;
            }
            if (visited.contains(current) || node.distance() > getConfig().algorithm.orElseThrow().maxLeavesRadius) {
                continue;
            }
            visited.add(current);

            if (isLeafBlock(currentState)) {
                leaves.put(current, new TreeData.BlockInfo(currentState, Util.brightness(level, currentState, current)));

                for (Direction direction : Direction.values()) {
                    BlockPos nextPos = current.relative(direction);
                    if (!visited.contains(nextPos)) {
                        toVisit.add(new BlockSearchNode(nextPos, node.distance() + 1));
                    }
                }
            }
        }

        return leaves;
    }

    private Map<BlockPos, TreeData.BlockInfo> gatherAdjacentBlocks(Level level, Map<BlockPos, TreeData.BlockInfo> logs, Map<BlockPos, TreeData.BlockInfo> leaves) {
        Map<BlockPos, TreeData.BlockInfo> adjacentBlocks = new Object2ObjectOpenHashMap<>();
        Map<BlockPos, TreeData.BlockInfo> allTreeBlocks = new Object2ObjectOpenHashMap<>(logs);
        allTreeBlocks.putAll(leaves);

        for (BlockPos blockPos : allTreeBlocks.keySet()) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = blockPos.relative(dir);
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.is(Blocks.VINE)) {
                    adjacentBlocks.putAll(gatherVines(level, neighbor));
                } else if (neighborState.is(Blocks.BEE_NEST)) {
                    adjacentBlocks.put(neighbor, new TreeData.BlockInfo(neighborState, Util.brightness(level, neighborState, neighbor)));
                } else if (neighborState.is(Blocks.COCOA)) {
                    adjacentBlocks.put(neighbor, new TreeData.BlockInfo(neighborState, Util.brightness(level, neighborState, neighbor)));
                }
            }
        }
        return adjacentBlocks;
    }

    private Map<BlockPos, TreeData.BlockInfo> gatherVines(Level level, BlockPos startPos) {
        Map<BlockPos, TreeData.BlockInfo> vines = new Object2ObjectOpenHashMap<>();
        Stack<BlockPos> toVisit = new Stack<>();
        Set<BlockPos> visited = new HashSet<>();

        toVisit.push(startPos);
        while (!toVisit.isEmpty()) {
            BlockPos current = toVisit.pop();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            BlockState currentState = level.getBlockState(current);
            if (currentState.is(Blocks.VINE)) {
                vines.put(current, new TreeData.BlockInfo(currentState, Util.brightness(level, currentState, current)));

                BlockPos neighbor = current.below();
                if (!visited.contains(neighbor)) {
                    toVisit.push(neighbor);
                }
            }
        }
        return vines;
    }

    private boolean isLogBlock(BlockState blockState) {
        return BlockPredicateInvoker.class.cast(getConfig().logFilter).invokeMatchesState(blockState);
    }

    private boolean isLeafBlock(BlockState blockState) {
        if (getConfig().leavesFilter.isEmpty()) return false;

        if (getConfig().algorithm.orElseThrow().shouldIgnorePersistentLeaves && blockState.hasProperty(BlockStateProperties.PERSISTENT) && blockState.getValue(BlockStateProperties.PERSISTENT))
            return false;

        return BlockPredicateInvoker.class.cast(getConfig().leavesFilter.orElseThrow()).invokeMatchesState(blockState);
    }

    public TreeConfig getConfig() {
        return config;
    }
}