package io.github.maniadiaz.bedroom;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BacteriaClient {
    private static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"bacteria"),"main");
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(LAYER,Model::layer);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(BacteriaEntity.TYPE.get(),Renderer::new);}
    static final class Renderer extends MobRenderer<BacteriaEntity,Model>{
        Renderer(EntityRendererProvider.Context c){super(c,new Model(c.bakeLayer(LAYER)),.45F);}
        @Override public ResourceLocation getTextureLocation(BacteriaEntity e){return ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"textures/entity/bacteria.png");}
    }
    static final class Model extends EntityModel<BacteriaEntity>{
        private final ModelPart root,body,head;
        private final ModelPart[] arms=new ModelPart[2],elbows=new ModelPart[2],legs=new ModelPart[2],knees=new ModelPart[2];
        Model(ModelPart root){
            this.root=root;body=root.getChild("body");head=body.getChild("head");
            for(int i=0;i<2;i++){arms[i]=body.getChild("arm"+i);elbows[i]=arms[i].getChild("elbow");legs[i]=root.getChild("leg"+i);knees[i]=legs[i].getChild("knee");}
        }
        static LayerDefinition layer(){
            var mesh=new MeshDefinition();var root=mesh.getRoot();
            var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,64).addBox(-.8F,-4,-.7F,1.6F,18,1.4F),PartPose.offset(0,-10,0));
            for(int i=0;i<7;i++)body.addOrReplaceChild("vertebra"+i,CubeListBuilder.create().texOffs(64,0).addBox(-1.2F,-.6F,-1,2.4F,1.2F,2),PartPose.offset(0,-3+i*2.3F,0));
            for(int side:new int[]{-1,1}){
                body.addOrReplaceChild("clavicle"+side,CubeListBuilder.create().texOffs(64,0).addBox(-3,-.6F,-.7F,6,1.2F,1.4F),PartPose.offsetAndRotation(side*3,-3,-.3F,0,0,side*.14F));
                for(int i=0;i<5;i++){
                    var rib=body.addOrReplaceChild("rib"+side+"_"+i,CubeListBuilder.create().texOffs(0,0).addBox(0,-.45F,-.5F,4.3F-i*.32F,.9F,1),PartPose.offsetAndRotation(side*.5F,i*1.8F-2,.2F,0,side*.25F,side<0?3.14F:.13F));
                    rib.addOrReplaceChild("curve",CubeListBuilder.create().texOffs(0,0).addBox(-.45F,0,-.4F,.9F,2.5F,.8F),PartPose.offsetAndRotation(3.2F-i*.25F,0,0,-.6F,0,.55F));
                }
            }
            body.addOrReplaceChild("pelvis",CubeListBuilder.create().texOffs(0,0).addBox(-3,-1,-1.4F,6,2,2.8F),PartPose.offset(0,13,0));
            var head=body.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,64).addBox(-1.5F,-6,-1.5F,3,6,3),PartPose.offsetAndRotation(.4F,-7,-.3F,.08F,0,.12F));
            head.addOrReplaceChild("crest",CubeListBuilder.create().texOffs(0,0).addBox(-3.7F,-3,-1.8F,7.4F,3,3.6F),PartPose.offsetAndRotation(0,-2,0,0,0,-.18F));
            for(int i=0;i<5;i++)head.addOrReplaceChild("crown"+i,CubeListBuilder.create().texOffs(64,64).addBox(-.35F,-2,-.35F,.7F,3,.7F),PartPose.offsetAndRotation(-2.5F+i*1.2F,-4,0,0,0,(i-2)*.2F));
            for(int i=0;i<2;i++){
                float side=i==0?-1:1;
                var arm=body.addOrReplaceChild("arm"+i,CubeListBuilder.create().texOffs(0,0).addBox(-.9F,0,-.9F,1.8F,15,1.8F),PartPose.offsetAndRotation(side*5.3F,-3,0,0,0,-side*.1F));
                arm.addOrReplaceChild("cable",CubeListBuilder.create().texOffs(0,64).addBox(-.35F,0,-.35F,.7F,14,.7F),PartPose.offsetAndRotation(side*.8F,1,-1.1F,0,0,side*.04F));
                var elbow=arm.addOrReplaceChild("elbow",CubeListBuilder.create().texOffs(0,0).addBox(-.65F,0,-.65F,1.3F,14,1.3F),PartPose.offsetAndRotation(0,14.5F,0,-.12F,0,side*.07F));
                var palm=elbow.addOrReplaceChild("palm",CubeListBuilder.create().texOffs(64,0).addBox(-1.2F,0,-.65F,2.4F,3,1.3F),PartPose.offset(0,13.5F,0));
                for(int f=0;f<4;f++){
                    var finger=palm.addOrReplaceChild("finger"+f,CubeListBuilder.create().texOffs(0,64).addBox(-.24F,0,-.24F,.48F,3.2F,.48F),PartPose.offsetAndRotation(-1+f*.65F,2.5F,0,0,0,(f-1.5F)*.16F));
                    finger.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(64,0).addBox(-.18F,0,-.18F,.36F,2.5F,.36F),PartPose.offsetAndRotation(0,3,0,-.35F,0,0));
                }
                var leg=root.addOrReplaceChild("leg"+i,CubeListBuilder.create().texOffs(0,0).addBox(-.9F,0,-.9F,1.8F,10,1.8F),PartPose.offsetAndRotation(side*2,3,0,-.07F,0,side*.025F));
                var knee=leg.addOrReplaceChild("knee",CubeListBuilder.create().texOffs(64,0).addBox(-.6F,0,-.6F,1.2F,10,1.2F),PartPose.offsetAndRotation(0,10,0,.1F,0,0));
                knee.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(0,64).addBox(-.8F,0,-2.5F,1.6F,1,3.5F),PartPose.offset(0,10,0));
            }
            return LayerDefinition.create(mesh,128,128);
        }
        @Override public void setupAnim(BacteriaEntity e,float limb,float amount,float age,float yaw,float pitch){
            root.getAllParts().forEach(ModelPart::resetPose);
            boolean alert=e.state()==BacteriaEntity.State.ALERT,chase=e.state()==BacteriaEntity.State.CHASING;
            body.zRot=Mth.sin(age*(alert?.8F:.065F))*(alert?.04F:.012F);body.xRot=e.onClimbable()?-.3F:chase?.1F:.035F;
            head.yRot=yaw*Mth.DEG_TO_RAD*.6F;head.xRot+=pitch*Mth.DEG_TO_RAD*.25F;head.zRot+=Mth.sin(age*.11F)*.05F;
            for(int i=0;i<2;i++){
                float cycle=Mth.cos(limb*(chase?.9F:.55F)+i*Mth.PI)*amount;
                legs[i].xRot+=cycle*.65F;knees[i].xRot+=Math.max(0,-cycle)*.7F;
                arms[i].xRot=-cycle*.45F+(chase?-.2F:0);arms[i].zRot+=Mth.sin(age*.08F+i)*.035F;elbows[i].xRot-=chase?.3F:.05F;
            }
        }
        @Override public void renderToBuffer(PoseStack p,VertexConsumer b,int light,int overlay,float r,float g,float blue,float a){root.render(p,b,light,overlay,r,g,blue,a);}
    }
}
