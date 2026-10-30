package io.github.maniadiaz.bedroom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID,value=Dist.CLIENT)
public final class RecoveryClient {
    private static final ResourceLocation DOOR=ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"textures/environment/recovery_door.png");
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!BedroomHud.recovering())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var target=BedroomHud.recoveryDoor();var camera=event.getCamera().getPosition();var pose=event.getPoseStack();pose.pushPose();
        pose.translate(target.x-camera.x,66.5-camera.y,target.z-camera.z);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
        var buffers=mc.renderBuffers().bufferSource();var type=RenderType.entityTranslucent(DOOR);var out=buffers.getBuffer(type);
        vertex(out,pose,-.65F,-1.5F,0,1);vertex(out,pose,.65F,-1.5F,1,1);vertex(out,pose,.65F,1.5F,1,0);vertex(out,pose,-.65F,1.5F,0,0);
        buffers.endBatch(type);pose.popPose();
    }
    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,com.mojang.blaze3d.vertex.PoseStack pose,float x,float y,float u,float v){out.vertex(pose.last().pose(),x,y,0).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.last().normal(),0,0,1).endVertex();}
    private RecoveryClient(){}
}
