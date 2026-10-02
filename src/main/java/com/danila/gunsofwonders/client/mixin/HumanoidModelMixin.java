package com.danila.gunsofwonders.client.mixin;

import com.danila.gunsofwonders.client.GunArmPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=HumanoidModel.class, remap=false)
abstract class HumanoidModelMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at=@At("TAIL"))
    private void gow$lockArms(HumanoidRenderState state, CallbackInfo ci) {
        GunArmPose.apply((HumanoidModel<?>)(Object)this, state);
    }
}
