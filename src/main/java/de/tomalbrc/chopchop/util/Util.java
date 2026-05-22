package de.tomalbrc.chopchop.util;

import de.tomalbrc.chopchop.config.ModConfig;
import de.tomalbrc.chopchop.entity.FallingTree;
import de.tomalbrc.chopchop.entity.SequentialFallingTree;
import de.tomalbrc.chopchop.impl.TreeData;
import de.tomalbrc.chopchop.entity.Entities;
import de.tomalbrc.chopchop.entity.AnimatedFallingTree;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class Util {
    public static boolean canUse(Player player) {
        return !player.isSecondaryUseActive();
    }

    public static void minedTree(ServerLevel level, TreeData treeData) {
        FallingTree fallingTree = ModConfig.getInstance().animated ? new AnimatedFallingTree(Entities.FALLING_TREE, level) : new SequentialFallingTree(Entities.FALLING_TREE2, level);
        fallingTree.setPos(treeData.origin().getBottomCenter());
        fallingTree.setModelData(treeData);
        fallingTree.breakBlocks(level);
        level.addFreshEntity(fallingTree);
    }

    public static Direction direction(Player player, BlockPos blockPos) {
        var dirVec = player.position().subtract(blockPos.getBottomCenter());
        return Direction.fromYRot(-Math.atan2(dirVec.x, dirVec.z) * Mth.RAD_TO_DEG).getOpposite();
    }

    // TODO: shouldnt this be a vanilla thing?
    public static int brightness(Level level, BlockState blockState, BlockPos blockPos) {
        int sky = level.getBrightness(LightLayer.SKY, blockPos);
        int brightness = level.getBrightness(LightLayer.BLOCK, blockPos);

        if (!blockState.canOcclude()) {
            return new Brightness(brightness, sky).pack();
        }

        for (Direction direction : Direction.values()) {
            var pos = blockPos.relative(direction);
            var current = level.getBlockState(pos);
            if (!current.canOcclude()) {
                return new Brightness(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos)).pack();
            }
        }

        return new Brightness(brightness, sky).pack();
    }
}
