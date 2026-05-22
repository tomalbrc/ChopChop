package de.tomalbrc.chopchop.mixin;

import de.tomalbrc.chopchop.Chopchop;
import de.tomalbrc.chopchop.util.Util;
import de.tomalbrc.chopchop.impl.TreeData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void cc$customDestroyTime(BlockState blockState, Player player, BlockGetter blockGetter, BlockPos blockPos, CallbackInfoReturnable<Float> cir) {
        if (cir.getReturnValueF() <= 0f) {
            return;
        }

        if (!(blockGetter instanceof ServerLevel level)) {
            return;
        }

        if (!Util.canUse(player)) {
            return;
        }

        TreeData treeData = Chopchop.MINING_CACHE.computeIfAbsent(player, blockPos, level, blockState);
        if (treeData == null) {
            return;
        }

        cir.setReturnValue(treeData.miningSpeedModifier().getMiningSpeed(cir.getReturnValueF()));
    }
}
