package de.tomalbrc.chopchop.entity;

import de.tomalbrc.chopchop.impl.TreeData;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class FallingTree extends Entity implements PolymerEntity {
    public FallingTree(EntityType<?> type, Level level) {
        super(type, level);
    }

    public abstract void setModelData(TreeData treeData);

    public abstract void breakBlocks(ServerLevel level);
}
