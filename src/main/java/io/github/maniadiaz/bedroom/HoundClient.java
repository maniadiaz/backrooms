package io.github.maniadiaz.bedroom;

import com.mojang.blaze3d.vertex.PoseStack;
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
public final class HoundClient {
    private static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"hound"),"main");
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(LAYER,Model::layer);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(HoundEntity.TYPE.get(),Renderer::new);}
    static final class Renderer extends MobRenderer<HoundEntity,Model>{
        Renderer(EntityRendererProvider.Context c){super(c,new Model(c.bakeLayer(LAYER)),.45F);}
        @Override public ResourceLocation getTextureLocation(HoundEntity e){return ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"textures/entity/hound.png");}
    }
    static final class Model extends EntityModel<HoundEntity>{
        private final ModelPart root,body,head,jaw;
        private final ModelPart[] legs=new ModelPart[4],knees=new ModelPart[4];
        Model(ModelPart root){
            this.root=root;body=root.getChild("body");head=root.getChild("head");jaw=head.getChild("jaw");
            for(int i=0;i<4;i++){legs[i]=root.getChild("leg"+i);knees[i]=legs[i].getChild("knee");}
        }
        static LayerDefinition layer(){
            var mesh=new MeshDefinition();var root=mesh.getRoot();
            var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-3,-4,-7,6,7,14),PartPose.offsetAndRotation(0,9,1,-.12F,0,0));
            body.addOrReplaceChild("hump",CubeListBuilder.create().texOffs(0,0).addBox(-3.8F,-6,-5,7.6F,5,8),PartPose.ZERO);
            body.addOrReplaceChild("haunch",CubeListBuilder.create().texOffs(0,0).addBox(-2.8F,-2,5,5.6F,5,5),PartPose.ZERO);
            for(int i=0;i<6;i++){
                body.addOrReplaceChild("vertebra"+i,CubeListBuilder.create().texOffs(64,0).addBox(-.7F,-1,-.7F,1.4F,2,1.4F),PartPose.offset(0,-4,-5+i*2));
                for(int s:new int[]{-1,1})body.addOrReplaceChild("rib"+i+"_"+s,CubeListBuilder.create().texOffs(64,0).addBox(-.5F,0,-.5F,1,5,1),PartPose.offsetAndRotation(s*3,-2,-5+i*1.8F,0,0,s*.17F));
            }
            var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-2.7F,-3,-5,5.4F,5,6),PartPose.offset(0,10,-7));
            head.addOrReplaceChild("mask",CubeListBuilder.create().texOffs(0,64).addBox(-3.8F,-4,-7,7.6F,6,5),PartPose.ZERO);
            for(int s:new int[]{-1,1}){
                head.addOrReplaceChild("brow"+s,CubeListBuilder.create().texOffs(64,64).addBox(-1.5F,-.6F,-.7F,3,1.2F,1.4F),PartPose.offsetAndRotation(s*1.9F,-2.4F,-7.2F,0,0,s*-.24F));
                head.addOrReplaceChild("socket"+s,CubeListBuilder.create().texOffs(0,64).addBox(-1,-.7F,-.3F,2,1.4F,.6F),PartPose.offset(s*1.9F,-1.2F,-7.4F));
                head.addOrReplaceChild("tusk"+s,CubeListBuilder.create().texOffs(64,0).addBox(-.45F,0,-.45F,.9F,3.4F,.9F),PartPose.offsetAndRotation(s*2.6F,1,-7,0,0,s*-.15F));
            }
            var jaw=head.addOrReplaceChild("jaw",CubeListBuilder.create().texOffs(0,0).addBox(-3,0,-7,6,1.4F,7),PartPose.offsetAndRotation(0,2,0,.3F,0,0));
            for(int i=0;i<6;i++)jaw.addOrReplaceChild("tooth"+i,CubeListBuilder.create().texOffs(64,0).addBox(-.35F,-1.4F,-.4F,.7F,1.6F,.8F),PartPose.offset(-2.4F+i*.95F,0,-6.5F));
            for(int i=0;i<4;i++){
                float side=i%2==0?-1:1;boolean front=i<2;
                var leg=root.addOrReplaceChild("leg"+i,CubeListBuilder.create().texOffs(0,0).addBox(-1.2F,0,-1.3F,2.4F,7,2.6F),PartPose.offsetAndRotation(side*3.7F,front?10:9,front?-5:7,front?-.25F:.4F,0,side*-.12F));
                var knee=leg.addOrReplaceChild("knee",CubeListBuilder.create().texOffs(0,0).addBox(-.7F,0,-.7F,1.4F,7,1.4F),PartPose.offsetAndRotation(0,7,0,front?.3F:-.4F,0,0));
                knee.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(0,0).addBox(-1.3F,-1,-2.6F,2.6F,1.5F,3.5F),PartPose.offset(0,7,0));
                for(int t=0;t<3;t++)knee.addOrReplaceChild("claw"+t,CubeListBuilder.create().texOffs(64,0).addBox(-.23F,-.5F,-2,.46F,1,2.4F),PartPose.offset(-.9F+t*.9F,7,-2.2F));
            }
            return LayerDefinition.create(mesh,128,128);
        }
        @Override public void setupAnim(HoundEntity e,float limb,float amount,float age,float yaw,float pitch){
            root.getAllParts().forEach(ModelPart::resetPose);
            boolean charge=e.visualState()==HoundEntity.State.CHARGING,stare=e.visualState()==HoundEntity.State.HESITATING;
            body.xRot=-.12F+(charge?.1F:0);body.y=9+Mth.sin(age*.12F)*.13F;
            head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD+(charge?-.12F:0);head.zRot=stare?Mth.sin(age*.09F)*.09F:0;
            jaw.xRot=(charge?.62F:stare?.45F:.24F)+Mth.sin(age*.23F)*.045F;
            for(int i=0;i<4;i++){
                float cycle=Mth.cos(limb*(charge?1.25F:.7F)+(i==0||i==3?0:Mth.PI))*amount;
                legs[i].xRot+=cycle*(charge?1.1F:.7F);knees[i].xRot+=Math.max(0,-cycle)*.65F;
            }
        }
        @Override public void renderToBuffer(PoseStack p,com.mojang.blaze3d.vertex.VertexConsumer b,int light,int overlay,float r,float g,float blue,float a){root.render(p,b,light,overlay,r,g,blue,a);}
    }
}
