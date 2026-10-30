package io.github.maniadiaz.bedroom;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Visible dust cone complements the collision-clipped server block light. */
@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID,value=Dist.CLIENT)
public final class FlashlightClient {
    private static boolean lit(ItemStack s){return s.is(Bedroom.FLASHLIGHT.get())&&SurvivalItems.on(s)&&SurvivalItems.charge(s)>0;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!AtmosphereClient.scene())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var pose=event.getPoseStack();pose.pushPose();Vec3 camera=event.getCamera().getPosition();
        pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var buffer=buffers.getBuffer(RenderType.lightning());
        for(var p:mc.level.players()){
            if(p.isSpectator()||!p.isAlive())continue;
            boolean main=lit(p.getMainHandItem());if(!main&&!lit(p.getOffhandItem()))continue;
            Vec3 forward=p.getViewVector(event.getPartialTick()).normalize();
            Vec3 side=forward.cross(new Vec3(0,1,0)).normalize();
            if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
            Vec3 up=side.cross(forward).normalize();
            boolean right=(p.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT)==main;
            Vec3 start=p.getEyePosition(event.getPartialTick()).add(side.scale(right?.25:-.25)).add(0,-.23,0).add(forward.scale(.35));
            // Clip from the eye first so the hand offset cannot project through a wall.
            var eyeHit=mc.level.clip(new ClipContext(p.getEyePosition(event.getPartialTick()),start,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(eyeHit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)continue;
            Vec3 end=mc.level.clip(new ClipContext(start,start.add(forward.scale(8)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
            double radius=Math.min(.85,start.distanceTo(end)*.105);
            for(int i=0;i<16;i++){
                double a=i*Math.PI/8,b=(i+1)*Math.PI/8;
                Vec3 ra=side.scale(Math.cos(a)).add(up.scale(Math.sin(a))),rb=side.scale(Math.cos(b)).add(up.scale(Math.sin(b)));
                Vec3 ea=mc.level.clip(new ClipContext(start,end.add(ra.scale(radius)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
                Vec3 eb=mc.level.clip(new ClipContext(start,end.add(rb.scale(radius)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
                vertex(buffer,pose,start.add(ra.scale(.035)),.09F);vertex(buffer,pose,ea,.012F);
                vertex(buffer,pose,eb,.012F);vertex(buffer,pose,start.add(rb.scale(.035)),.09F);
            }
        }
        buffers.endBatch(RenderType.lightning());pose.popPose();
    }
    private static void vertex(VertexConsumer out,com.mojang.blaze3d.vertex.PoseStack pose,Vec3 p,float alpha){
        out.vertex(pose.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(1F,.94F,.72F,alpha).endVertex();
    }
    private FlashlightClient(){}
}
