package de.tomalbrc.chopchop.util;

import de.tomalbrc.chopchop.Chopchop;
import de.tomalbrc.chopchop.impl.TreeData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

public class MiningCache {
    private final WeakHashMap<Player, Map<BlockPos, Entry>> cache = new WeakHashMap<>();

    public TreeData computeIfAbsent(Player player, BlockPos pos, ServerLevel level, BlockState state) {
        Map<BlockPos, Entry> perPlayer = cache.computeIfAbsent(player, _ -> new Object2ObjectOpenHashMap<>());

        Entry entry = perPlayer.get(pos);
        if (entry != null && entry.state.getBlock() == state.getBlock()) {
            return entry.treeData;
        }

        TreeData treeData = Chopchop.gatherData(state, pos, level, player);
        perPlayer.put(pos.immutable(), new Entry(state, treeData));
        return treeData;
    }

    public void invalidate(Player player, BlockPos pos) {
        Map<BlockPos, Entry> perPlayer = cache.get(player);
        if (perPlayer != null) {
            perPlayer.remove(pos);
        }
    }

    public void invalidateAll(Player player) {
        cache.remove(player);
    }

    private record Entry(BlockState state, TreeData treeData) {
    }
}
