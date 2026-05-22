package de.tomalbrc.chopchop.impl;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public class TreeTypes {
    public final static Set<TreeType> TYPES = new ObjectOpenHashSet<>();

    public static Set<TreeType> get(BlockState blockState) {
        Set<TreeType> types = new ObjectOpenHashSet<>();
        for (TreeType type : TreeTypes.TYPES) {
            if (type.isStem(blockState)) {
                types.add(type);
            }
        }

        return types;
    }
}
