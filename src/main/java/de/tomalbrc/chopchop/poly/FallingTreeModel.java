package de.tomalbrc.chopchop.poly;

import com.mojang.math.Axis;
import de.tomalbrc.chopchop.impl.TreeData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Brightness;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

public class FallingTreeModel extends ElementHolder {
    private Direction direction = Direction.NORTH;

    public FallingTreeModel() {
        super();
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setModelData(TreeData treeData, Vec3 pos) {
        this.getElements().forEach(this::removeElement);

        for (Map.Entry<BlockPos, TreeData.BlockInfo> entry : treeData.blocks().entrySet()) {
            var blockDisplay = new BlockDisplayElement(entry.getValue().blockState());
            blockDisplay.setInvisible(true);
            blockDisplay.setBrightness(Brightness.unpack(entry.getValue().lightLevel()));

            Matrix4f matrix4f = new Matrix4f();
            matrix4f.rotateLocal(Axis.YP.rotationDegrees(direction.toYRot()).normalize());   // rotate around pivot
            matrix4f.translate(direction.getUnitVec3().scale(-0.5).toVector3f());       // push forward in rotated space
            matrix4f.translate(entry.getKey().getBottomCenter().subtract(pos).toVector3f()); // move to pivot in world
            matrix4f.translate(new Vector3f(-0.51f,0,-0.49f)); // 0.01 offset to prevent z-fight
            blockDisplay.setTransformation(matrix4f);

            blockDisplay.setTeleportDuration(2);

            blockDisplay.setOverridePos(pos.subtract(direction.getUnitVec3().scale(-0.5)));
            blockDisplay.setYaw(direction.toYRot());
            this.addElement(blockDisplay);
        }
    }

    public void setPitch(float pitch) {
        for (VirtualElement element : this.getElements()) {
            if (element instanceof DisplayElement displayElement)
                displayElement.setPitch(pitch);
        }
    }

    public void setTime(int time, int max) {
        if (time % 2 == 0) setPitch(getFallingTreeAngleBounce(time, max, 90));
    }

    public static float getFallingTreeAngleBounce(int tick, int duration, float maxAngleDeg) {
        float t = Math.min(1f, tick / (duration+8f));
        float bounce;
        if (t < 1f / 2.75f) {
            bounce = 7.5625f * t * t;
        } else if (t < 2f / 2.75f) {
            t -= 1.5f / 2.75f;
            bounce = 7.5625f * t * t + 0.75f;
        } else if (t < 2.5f / 2.75f) {
            t -= 2.25f / 2.75f;
            bounce = 7.5625f * t * t + 0.9375f;
        } else {
            t -= 2.625f / 2.75f;
            bounce = 7.5625f * t * t + 0.984375f;
        }

        return maxAngleDeg * bounce;
    }
}
