package com.danila.gunsofwonders.client;

import com.danila.gunsofwonders.GunsOfWonders;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid=GunsOfWonders.ID, value=Dist.CLIENT)
public final class GunClient {
    private static final Map<String,GunMesh> MESHES=new ConcurrentHashMap<>();
    static GunMesh mesh(String id) { return MESHES.get(id); }
    @SubscribeEvent
    public static void models(ModelEvent.ModifyBakingResult event) {
        MESHES.clear();
        for (String id : GunsOfWonders.GUN_IDS) {
            GunMesh mesh=GunMesh.load(id); MESHES.put(id,mesh);
            event.getResults().itemStackModels().put(Identifier.fromNamespaceAndPath(GunsOfWonders.ID,id),
                (state,stack,resolver,context,level,owner,seed) -> {
                    var layer=state.newLayer(); layer.setUsesBlockLight(false);
                    boolean slim = owner != null && owner.asLivingEntity() instanceof AbstractClientPlayer player
                        && player.getSkin().model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
                    var renderer=new InventoryRenderer(mesh,context,slim);
                    layer.setupSpecialModel(renderer,Boolean.TRUE);
                    layer.setExtents(() -> new Vector3fc[]{new Vector3f(0,0,0),new Vector3f(1,1,1)});
                });
        }
    }
    @SubscribeEvent
    public static boolean hand(RenderHandEvent event) {
        var player=Minecraft.getInstance().player;
        if (player==null) return false;
        boolean mainGun=player.getMainHandItem().getItem() instanceof GunsOfWonders.GunItem;
        if (mainGun && event.getHand()==InteractionHand.OFF_HAND) return true;
        if (!(event.getItemStack().getItem() instanceof GunsOfWonders.GunItem gun)) return false;
        GunMesh mesh=MESHES.get(gun.model); if(mesh==null) return false;
        var pose=event.getPoseStack(); pose.pushPose();
        boolean left=(player.getMainArm()==HumanoidArm.LEFT) != (event.getHand()==InteractionHand.OFF_HAND);
        if(left) pose.scale(-1,1,1);
        mesh.idle(pose,(player.tickCount+event.getPartialTick())/20f);
        pose.translate(-mesh.camera[0]/16f,-mesh.camera[1]/16f,-mesh.camera[2]/16f);
        mesh.submit(pose,event.getNodeCollector(),event.getPackedLight(),player.getSkin().body().texturePath());
        pose.popPose(); return true;
    }
    private record InventoryRenderer(GunMesh mesh, ItemDisplayContext context, boolean slim) implements SpecialModelRenderer<Boolean> {
        public Boolean extractArgument(ItemStack stack) { return Boolean.TRUE; }
        public void getExtents(Consumer<Vector3fc> out) { out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1,1)); }
        public void submit(Boolean arg, PoseStack pose, SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline) {
            pose.pushPose();
            float[] lo=mesh.bounds[0],hi=mesh.bounds[1];
            if(context==ItemDisplayContext.GUI || context==ItemDisplayContext.FIXED || context==ItemDisplayContext.GROUND) {
                float size=Math.max(hi[2]-lo[2],Math.max(hi[0]-lo[0],hi[1]-lo[1]));
                float scale=(context==ItemDisplayContext.GROUND?9f:14f)/size;
                pose.translate(.5f,.5f,.5f);pose.mulPose(Axis.YP.rotationDegrees(90));
                if(context==ItemDisplayContext.GUI) pose.mulPose(Axis.XP.rotationDegrees(-15));
                pose.scale(scale,scale,scale);
                pose.translate(-(lo[0]+hi[0])/32f,-(lo[1]+hi[1])/32f,-(lo[2]+hi[2])/32f);
            } else if(context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                // Undo item-layer offsets and rotations, then mount the gun at the real wrist.
                pose.translate(.5f,.5f,.5f);
                pose.translate((context.leftHand()?1f:-1f)/16f,-2f/16f,10f/16f);
                pose.mulPose(Axis.YP.rotationDegrees(-180));
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.translate(GunArmPose.wristX(context.leftHand(),slim)/16f,10f/16f,0);
                pose.scale(1f/GunArmPose.widthScale(mesh.rightArm,slim),1,1);
                pose.mulPose(GunArmPose.gripRotation(mesh,context.leftHand()));
                if (context.leftHand()) pose.scale(-1,1,1);
                pose.translate(-mesh.grip[0]/16f,-mesh.grip[1]/16f,-mesh.grip[2]/16f);
            } else {
                pose.translate(.5f,.35f,.5f);pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.scale(.65f,.65f,.65f);pose.translate(-mesh.grip[0]/16f,-mesh.grip[1]/16f,-mesh.grip[2]/16f);
            }
            mesh.submit(pose,collector,light,null);pose.popPose();
        }
    }
}
