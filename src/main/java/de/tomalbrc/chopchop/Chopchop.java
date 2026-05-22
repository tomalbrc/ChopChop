package de.tomalbrc.chopchop;

import com.mojang.logging.LogUtils;
import de.tomalbrc.chopchop.config.ModConfig;
import de.tomalbrc.chopchop.entity.Entities;
import de.tomalbrc.chopchop.impl.TreeData;
import de.tomalbrc.chopchop.impl.TreeType;
import de.tomalbrc.chopchop.impl.TreeTypes;
import de.tomalbrc.chopchop.util.MiningCache;
import de.tomalbrc.chopchop.util.Util;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class Chopchop implements ModInitializer {
    public static MinecraftServer SERVER;
    public static Logger LOGGER = LogUtils.getLogger();
    public static final MiningCache MINING_CACHE = new MiningCache();

    @Override
    public void onInitialize() {
        Entities.init();

        PolymerBlockUtils.SERVER_SIDE_MINING_CHECK.register((blockState, blockPos, player) -> {
            if (!Util.canUse(player)) {
                return false;
            }
            return !TreeTypes.get(blockState).isEmpty();
        });

        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> {
            SERVER = minecraftServer;
            ModConfig.load();
        });

        PlayerBlockBreakEvents.BEFORE.register((level, player, blockPos, blockState, blockEntity) -> {
            boolean flag = true;

            if (level.isClientSide() || !Util.canUse(player))
                return true;

            try {
                var treeData = gatherData(blockState, blockPos, (ServerLevel) level, player);
                if (treeData != null) {
                    Util.minedTree((ServerLevel) level, treeData);
                    flag = !ModConfig.getInstance().animated;

                    for (TreeData.AwardedStat stat : treeData.awardedStats()) {
                        player.awardStat(stat.stat(), stat.amount());
                    }

                    var hand = player.getUsedItemHand();
                    var itemStack = player.getItemInHand(hand);

                    var damage = treeData.toolDamage();
                    itemStack.hurtAndBreak(damage, player, EquipmentSlot.MAINHAND);

                }
            } catch (Exception e) {
                Chopchop.LOGGER.warn("Failed to gather tree data.", e);
            } finally {
                if (player instanceof ServerPlayer serverPlayer) {
                    MINING_CACHE.invalidate(serverPlayer, blockPos);
                }
            }

            return flag;
        });
    }

    public static TreeData gatherData(BlockState blockState, BlockPos blockPos, ServerLevel level, Player player) {
        var types = TreeTypes.get(blockState);
        if (!types.isEmpty()) {
            for (TreeType type : types) {
                var treeData = type.gatherTreeData(blockPos, level, player);
                if (treeData != null && !treeData.blocks().isEmpty()) {
                    return treeData;
                }
            }
        }

        return null;
    }
}
