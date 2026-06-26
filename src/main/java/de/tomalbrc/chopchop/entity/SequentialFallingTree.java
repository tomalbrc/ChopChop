package de.tomalbrc.chopchop.entity;

import com.mojang.logging.LogUtils;
import de.tomalbrc.chopchop.impl.TreeData;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SequentialFallingTree extends FallingTree implements PolymerEntity {
    private static final String TREE_DATA_KEY = "TreeData";
    private static final String BLOCK_INDEX_KEY = "BlockIndex";

    private TreeData treeData;
    private List<Map.Entry<BlockPos, TreeData.BlockInfo>> remainingBlocks;
    private Map<BlockPos, List<ItemStack>> remainingDrops;
    private int blockIndex = 0;

    public SequentialFallingTree(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void setModelData(TreeData treeData) {
        this.treeData = treeData;

        this.remainingBlocks = new ArrayList<>(treeData.blocks().entrySet());
        this.remainingBlocks.sort(Comparator.comparingInt(e -> e.getKey().distChessboard(treeData.origin())));

        this.remainingDrops = treeData.drops();

        this.blockIndex = 0;
    }

    @Override
    public void breakBlocks(ServerLevel level) {

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        this.blockIndex = valueInput.getIntOr(BLOCK_INDEX_KEY, 0);
        var res = valueInput.read(TREE_DATA_KEY, TreeData.CODEC);
        res.ifPresentOrElse(this::setModelData, () -> {
            LogUtils.getLogger().error("Could not read tree data");
            discard();
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        try {
            valueOutput.store(TREE_DATA_KEY, TreeData.CODEC, treeData);
            valueOutput.putInt(BLOCK_INDEX_KEY, blockIndex);
        } catch (Exception e) {
            LogUtils.getLogger().error("Could not save tree data");
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (treeData == null) {
            discard();
            return;
        }

        for (int i = 0; i < 9; i++) {
            if (blockIndex < remainingBlocks.size()) {
                var entry = remainingBlocks.get(blockIndex);
                BlockPos pos = entry.getKey();
                TreeData.BlockInfo info = entry.getValue();

                if (level() instanceof ServerLevel serverLevel && serverLevel.getBlockState(pos).getBlock() == info.blockState().getBlock()) {
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, info.blockState()),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            15, 0.2, 0.2, 0.2, 0.1
                    );

                    serverLevel.removeBlock(pos, false);
                    serverLevel.playSound(null, pos, info.blockState().getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8f, 1.0f);

                    var drops = remainingDrops.get(pos);
                    if (drops != null) {
                        for (ItemStack drop : drops) {
                            double x = treeData.origin().getX() + 0.5 + (random.nextDouble() - 0.5) * 2;
                            double z = treeData.origin().getZ() + 0.5 + (random.nextDouble() - 0.5) * 2;
                            double y = treeData.origin().getY() + random.nextDouble() * (treeData.blocks().size() / 5.0);

                            ItemEntity itemEntity = new ItemEntity(level(), x, y, z, drop);
                            itemEntity.setDefaultPickUpDelay();
                            level().addFreshEntity(itemEntity);
                        }
                    }
                }

                blockIndex++;
            }
        }

        if (blockIndex >= remainingBlocks.size()) {
            discard();
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return EntityTypes.BLOCK_DISPLAY;
    }
}