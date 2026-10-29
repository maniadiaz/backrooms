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

@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HoundClient {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "hound"), "main");
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(LAYER, Model::layer); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(HoundEntity.TYPE.get(), Renderer::new); }
    static final class Renderer extends MobRenderer<HoundEntity, Model> {
        Renderer(EntityRendererProvider.Context context) { super(context, new Model(context.bakeLayer(LAYER)), .45F); }
        @Override public ResourceLocation getTextureLocation(HoundEntity entity) { return ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "textures/entity/hound.png"); }
    }
    static final class Model extends EntityModel<HoundEntity> {
        private final ModelPart root, head;
        private final ModelPart[] legs;
        Model(ModelPart root) { this.root = root; head = root.getChild("head"); legs = new ModelPart[]{root.getChild("leg0"), root.getChild("leg1"), root.getChild("leg2"), root.getChild("leg3")}; }
        static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition(); PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -7, 6, 6, 14), PartPose.offset(0, 10, 1));
            for (int i = 0; i < 4; i++) root.addOrReplaceChild("rib" + i, CubeListBuilder.create().texOffs(40, 0).addBox(-3.5F, -3.5F, 0, 7, 7, 1), PartPose.offset(0, 10, -3 + i * 2));
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 22).addBox(-3, -5, -5, 6, 6, 5), PartPose.offset(0, 10, -7));
            head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(24, 22).addBox(-3, 0, -6, 6, 2, 6), PartPose.offsetAndRotation(0, 0, 0, .3F, 0, 0));
            for (int i = 0; i < 4; i++) root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(48, 12).addBox(-1, 0, -1, 2, 14, 2), PartPose.offset(i % 2 == 0 ? -3 : 3, 10, i < 2 ? -4 : 6));
            return LayerDefinition.create(mesh, 64, 64);
        }
        @Override public void setupAnim(HoundEntity entity, float limb, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD; head.xRot = pitch * Mth.DEG_TO_RAD;
            for (int i = 0; i < legs.length; i++) legs[i].xRot = Mth.cos(limb * .8F + (i == 0 || i == 3 ? 0 : Mth.PI)) * amount;
        }
        @Override public void renderToBuffer(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) { root.render(pose, buffer, light, overlay, r, g, b, a); }
    }
}
