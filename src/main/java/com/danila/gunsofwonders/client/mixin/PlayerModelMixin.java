package com.danila.gunsofwonders.client.mixin;

import com.danila.gunsofwonders.client.GunArmPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PlayerModel.class, remap=false)
abstract class PlayerModelMixin {
    @Inject(method="translateToHand(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at=@At("HEAD"), cancellable=true)
    private void gow$authoredWrist(AvatarRenderState state, HumanoidArm arm, PoseStack pose, CallbackInfo ci) {
        if (!GunArmPose.hasGun(state)) return;
        PlayerModel model = (PlayerModel)(Object)this;
        model.root().translateAndRotate(pose);
        model.getArm(arm).translateAndRotate(pose);
        ci.cancel();
    }
}
