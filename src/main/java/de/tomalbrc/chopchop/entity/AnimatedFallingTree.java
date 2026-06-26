package de.tomalbrc.chopchop.entity;

import com.mojang.logging.LogUtils;
import de.tomalbrc.chopchop.config.ModConfig;
import de.tomalbrc.chopchop.impl.TreeData;
import de.tomalbrc.chopchop.poly.FallingTreeModel;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnimatedFallingTree extends FallingTree implements PolymerEntity {
    public static final String TREE_DATA_KEY = "TreeData";
    public static final String AGE_KEY = "Age";

    private TreeData treeData;
    private FallingTreeModel model;
    private int age = 0;

    public AnimatedFallingTree(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public void setModelData(TreeData treeData) {
        this.treeData = treeData;

        if (this.model == null) {
            this.model = new FallingTreeModel();
            EntityAttachment.ofTicking(this.model, this);
        }

        this.model.setDirection(treeData.direction());
        this.model.setModelData(treeData, this.position());
    }

    @Override
    public void breakBlocks(ServerLevel level) {
        for (Map.Entry<BlockPos, TreeData.BlockInfo> entry : treeData.blocks().entrySet()) {
            level.removeBlock(entry.getKey(), false);
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return EntityTypes.BLOCK_DISPLAY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        this.age = valueInput.getIntOr(AGE_KEY, 0);
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
        } catch (Exception e) {
            LogUtils.getLogger().error("Could not save tree data");
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.model.setTime(age, ModConfig.getInstance().animationDuration);

        if (this.age > ModConfig.getInstance().animationDuration) {
            this.spawnItems();
            this.spawnParticles();
            this.discard();
        } else {
            if (age < ModConfig.getInstance().animationDuration/4) {
                if (age % 10 == 0) {
                    level().playSound(null, treeData.origin(), SoundEvents.LEAF_LITTER_STEP, SoundSource.BLOCKS, 0.4f, 0.8f);
                }

                level().playSound(null, treeData.origin(), SoundEvents.CREAKING_HEART_HURT, SoundSource.BLOCKS, 0.05f, 0.8f);
            }

            if (age > ModConfig.getInstance().animationDuration/4 && age % 4 == 0) {
                level().playSound(null, treeData.origin(), SoundEvents.LEAF_LITTER_STEP, SoundSource.BLOCKS, 0.5f, 0.8f);
            }
        }

        this.age++;
    }

    private void spawnParticles() {
        for (Map.Entry<BlockPos, TreeData.BlockInfo> entry : treeData.blocks().entrySet()) {
            var p = Vec3.atCenterOf(entry.getKey()).subtract(Vec3.atCenterOf(treeData.origin()));
            p = p.xRot(85 * Mth.DEG_TO_RAD);
            p = p.yRot((-treeData.direction().toYRot()+180) * Mth.DEG_TO_RAD);
            var n = Vec3.atCenterOf(treeData.origin()).add(p);
            ((ServerLevel)level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, entry.getValue().blockState()), n.x, n.y, n.z, 10, 0, 0, 0, 0);
            level().playSound(null, treeData.origin(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 0.001f, 0.8f);
        }
    }

    protected void spawnItems() {
        var drops = TreeData.Builder.collapseStacks(this.treeData.drops().values().stream().flatMap(List::stream).collect(Collectors.toList()));

        for (ItemStack stack : drops) {
            double deltaX = Mth.nextDouble(level().getRandom(), -0.1, 0.1);
            double deltaY = 0.25;
            double deltaZ = Mth.nextDouble(level().getRandom(), -0.1, 0.1);

            ItemEntity entity = new ItemEntity(level(), getX(), getY() + EntityTypes.ITEM.getHeight() / 2, getZ(), stack, deltaX, deltaY, deltaZ);
            entity.setDefaultPickUpDelay();
            level().addFreshEntity(entity);
        }
    }
}
