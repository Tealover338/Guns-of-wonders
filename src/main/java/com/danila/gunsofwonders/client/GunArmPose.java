package com.danila.gunsofwonders.client;

import com.danila.gunsofwonders.GunsOfWonders;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The authored rigid arm transforms, shared by avatar limbs and the held weapon. */
public final class GunArmPose implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose HOLD = HumanoidModel.ArmPose.create(
        "GOW_HOLD", true, true, (model, state, arm) -> {});

    @Override
    public HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity player, net.minecraft.world.InteractionHand hand, ItemStack stack) {
        return HOLD;
    }

    public static boolean hasGun(HumanoidRenderState state) {
        return state.rightHandItemStack.getItem() instanceof GunsOfWonders.GunItem
            || state.leftHandItemStack.getItem() instanceof GunsOfWonders.GunItem;
    }

    // Blockbench is Y-up; avatar model coordinates are Y-down and X-reversed.
    private static Vector3f toAvatar(float[] point, boolean mirrored) {
        return new Vector3f(mirrored ? point[0] : -point[0], -point[1], point[2]);
    }

    static Quaternionf rotation(GunMesh.Arm arm, boolean mirrored) {
        Vector3f x = toAvatar(arm.axes[0], mirrored);
        if (mirrored) x.negate(); // Reflect the local X axis as well to retain a proper rotation.
        return new Quaternionf().setFromNormalized(new Matrix3f(
            x, toAvatar(arm.axes[1], mirrored), toAvatar(arm.axes[2], mirrored)));
    }

    static Quaternionf gripRotation(GunMesh mesh, boolean mirrored) {
        return rotation(mesh.rightArm, mirrored).conjugate().mul(new Quaternionf().rotationZ((float)Math.PI));
    }

    static float wristX(boolean left, boolean slim) {
        return (left ? 1f : -1f) * (slim ? .5f : 1f);
    }

    static float widthScale(GunMesh.Arm arm, boolean slim) {
        return arm.width / (slim ? 3f : 4f);
    }

    private static Vector3f shoulder(GunMesh.Arm arm, boolean mirrored) {
        return toAvatar(arm.wrist, mirrored).sub(toAvatar(arm.axes[1], mirrored).mul(10f));
    }

    /** Called after vanilla walking, attacking and breathing have finished modifying the limbs. */
    public static void apply(HumanoidModel<?> model, HumanoidRenderState state) {
        if (!(state instanceof AvatarRenderState avatar) || !hasGun(state)) return;
        boolean mirrored = state.mainArm == HumanoidArm.LEFT;
        ItemStack stack = state.getMainHandItemStack();
        if (!(stack.getItem() instanceof GunsOfWonders.GunItem)) {
            mirrored = !mirrored;
            stack = mirrored ? state.leftHandItemStack : state.rightHandItemStack;
        }
        if (!(stack.getItem() instanceof GunsOfWonders.GunItem gun)) return;
        GunMesh mesh = GunClient.mesh(gun.model);
        if (mesh == null) return;
        boolean slim = model instanceof PlayerModel && avatar.skin.model() == PlayerModelType.SLIM;
        Vector3f offset = shoulder(mesh.rightArm, mirrored).add(shoulder(mesh.leftArm, mirrored)).mul(-.5f).add(0,2,0);
        place(mirrored ? model.leftArm : model.rightArm, mesh.rightArm, mirrored, mirrored, slim, offset);
        place(mirrored ? model.rightArm : model.leftArm, mesh.leftArm, mirrored, !mirrored, slim, offset);
    }

    private static void place(ModelPart part, GunMesh.Arm source, boolean mirrored, boolean left, boolean slim, Vector3f offset) {
        Quaternionf rotation = rotation(source, mirrored);
        float width = widthScale(source, slim);
        Vector3f wristOffset = new Vector3f(wristX(left, slim) * width, 10, 0).rotate(rotation);
        Vector3f pivot = toAvatar(source.wrist, mirrored).add(offset).sub(wristOffset);
        Vector3f euler = rotation.getEulerAnglesZYX(new Vector3f());
        part.x = pivot.x; part.y = pivot.y; part.z = pivot.z;
        part.xRot = euler.x; part.yRot = euler.y; part.zRot = euler.z;
        part.xScale = width; part.yScale = 1; part.zScale = 1;
    }
}
