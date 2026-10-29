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

@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD,value = Dist.CLIENT)
public final class BacteriaClient {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"bacteria"),"main");
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(LAYER,Model::layer); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(BacteriaEntity.TYPE.get(),Renderer::new); }
    static final class Renderer extends MobRenderer<BacteriaEntity,Model> {
        Renderer(EntityRendererProvider.Context context) { super(context,new Model(context.bakeLayer(LAYER)),.4F); }
        @Override public ResourceLocation getTextureLocation(BacteriaEntity entity) { return ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"textures/entity/bacteria.png"); }
    }
    static final class Model extends EntityModel<BacteriaEntity> {
        private final ModelPart root, body, head;
        private final ModelPart[] limbs = new ModelPart[4], wires = new ModelPart[4];
        Model(ModelPart root) {
            this.root = root; body = root.getChild("body"); head = root.getChild("head");
            for (int i = 0; i < 4; i++) { limbs[i] = root.getChild("limb"+i); wires[i] = body.getChild("wire"+i); }
        }
        static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition(); PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0)
                    .addBox(-2.5F,-2,-7,5,4,14),PartPose.offset(0,11,1));
            body.addOrReplaceChild("spine",CubeListBuilder.create().texOffs(0,20).addBox(-.8F,-3,-6,1.6F,1.5F,12),PartPose.ZERO);
            for (int i = 0; i < 5; i++) body.addOrReplaceChild("rib"+i,CubeListBuilder.create().texOffs(24,20)
                    .addBox(-3,-2.5F,-.5F,6,5,1),PartPose.offset(0,0,-5+i*2.5F));
            root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,36).addBox(-1.5F,-1,-4,3,3,5),PartPose.offset(0,13,-7));
            for (int i = 0; i < 4; i++) {
                float side = i%2 == 0 ? -1 : 1;
                PartDefinition limb = root.addOrReplaceChild("limb"+i,CubeListBuilder.create().texOffs(40,0)
                        .addBox(-.75F,0,-.75F,1.5F,11,1.5F),PartPose.offsetAndRotation(side*3.5F,11,i<2?-5:7,0,0,side*-.32F));
                limb.addOrReplaceChild("joint",CubeListBuilder.create().texOffs(46,0).addBox(-.6F,0,-.6F,1.2F,8,1.2F),PartPose.offsetAndRotation(0,10,0,-1.2F,0,0));
                PartDefinition wire = body.addOrReplaceChild("wire"+i,CubeListBuilder.create().texOffs(54,0)
                        .addBox(-.4F,0,-.4F,.8F,7,.8F),PartPose.offsetAndRotation(side*2,-1,i<2?-4:4,.6F,0,side*.8F));
                wire.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(58,0).addBox(-.35F,0,-.35F,.7F,6,.7F),PartPose.offsetAndRotation(0,6,0,-1.1F,0,0));
            }
            return LayerDefinition.create(mesh,64,64);
        }
        @Override public void setupAnim(BacteriaEntity entity,float limb,float amount,float age,float yaw,float pitch) {
            boolean alert = entity.state() == BacteriaEntity.State.ALERT;
            float pace = entity.state() == BacteriaEntity.State.CHASING ? 1.1F : .65F;
            body.y = 11 + Mth.sin(age*(alert?.9F:.12F))*(alert?.65F:.2F);
            body.zRot = Mth.sin(age*(alert?1.3F:.08F))*(alert?.12F:.025F);
            body.xRot=entity.onClimbable()?-.7F:0;
            head.yRot = yaw*Mth.DEG_TO_RAD*.5F; head.xRot = pitch*Mth.DEG_TO_RAD*.4F+.15F;
            for (int i = 0; i < 4; i++) {
                limbs[i].xRot = Mth.cos(limb*pace+(i==0||i==3?0:Mth.PI))*amount*.75F;
                wires[i].xRot = .6F + Mth.sin(age*.2F+i)*.16F + amount*.2F;
            }
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float r,float g,float b,float a) { root.render(pose,buffer,light,overlay,r,g,b,a); }
    }
}
