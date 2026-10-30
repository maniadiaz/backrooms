package io.github.maniadiaz.bedroom;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT)
public final class SmilerClient {
    private static final Map<Integer, Hiss> sounds = new HashMap<>();
    private static ClientLevel world;
    static void reset() {
        var manager = Minecraft.getInstance().getSoundManager();
        for (Hiss sound : sounds.values()) { sound.finish(); manager.stop(sound); }
        sounds.clear();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (world != mc.level) { reset(); world = mc.level; }
        var camera = mc.getCameraEntity();
        if (!AtmosphereClient.scene() || camera == null || mc.options.getSoundSourceVolume(SoundSource.HOSTILE) <= 0
                || mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0) { reset(); return; }
        var nearby = mc.level.getEntitiesOfClass(SmilerEntity.class, camera.getBoundingBox().inflate(22), e -> !e.isRemoved() && e.distanceToSqr(camera) <= 484);
        Set<Integer> ids = new HashSet<>();
        for (SmilerEntity entity : nearby) {
            if (entity.distanceToSqr(camera) > 400 && !sounds.containsKey(entity.getId())) continue;
            ids.add(entity.getId());
            Hiss current = sounds.get(entity.getId());
            if (current == null || current.isStopped()) {
                Hiss next = new Hiss(entity); sounds.put(entity.getId(), next); mc.getSoundManager().play(next);
            }
        }
        sounds.entrySet().removeIf(entry -> {
            if (ids.contains(entry.getKey())) return false;
            entry.getValue().finish(); mc.getSoundManager().stop(entry.getValue()); return true;
        });
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); world = null; }
    private static final class Hiss extends AbstractTickableSoundInstance {
        private final SmilerEntity entity;
        Hiss(SmilerEntity entity) {
            super(BedroomSounds.SMILER_HISS.get(), SoundSource.HOSTILE, RandomSource.create());
            this.entity = entity; looping = true; delay = 0; relative = false; attenuation = SoundInstance.Attenuation.LINEAR;
            update();
        }
        private void update() {
            var camera = Minecraft.getInstance().getCameraEntity();
            double distance = camera == null ? 20 : entity.distanceTo(camera);
            float closeness = (float)Math.max(0, 1 - distance / 20);
            volume = .15F + .65F * closeness * closeness; pitch = .85F + .2F * closeness;
            x = entity.getX(); y = entity.getEyeY(); z = entity.getZ();
        }
        @Override public void tick() {
            if (entity.isRemoved() || entity.level() != Minecraft.getInstance().level || !AtmosphereClient.scene()) { stop(); return; }
            update();
        }
        void finish() { stop(); }
    }
    @Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(SmilerEntity.TYPE.get(), Renderer::new); }
        @SubscribeEvent public static void reload(net.minecraftforge.client.event.sound.SoundEngineLoadEvent event) { reset(); }
    }
    static final class Renderer extends EntityRenderer<SmilerEntity> {
        private static final ResourceLocation FACE = ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "textures/entity/smiler.png");
        private static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"textures/entity/smiler_body.png");
        Renderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
        @Override public ResourceLocation getTextureLocation(SmilerEntity entity) { return FACE; }
        @Override public void render(SmilerEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
            poses.pushPose();
            double age=entity.tickCount+partial;
            poses.translate(0, 1.05 + Math.sin(age * .055) * .045, 0);
            poses.mulPose(entityRenderDispatcher.cameraOrientation());
            poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            float radius=.72F+(float)Math.sin(age*.045)*.012F;
            var shell=buffers.getBuffer(RenderType.entityCutoutNoCull(BODY));
            for(int ring=0;ring<12;ring++)for(int segment=0;segment<24;segment++){
                sphere(shell,poses.last(),ring,segment,radius,light);
                sphere(shell,poses.last(),ring+1,segment,radius,light);
                sphere(shell,poses.last(),ring+1,segment+1,radius,light);
                sphere(shell,poses.last(),ring,segment+1,radius,light);
            }
            VertexConsumer buffer = buffers.getBuffer(RenderType.eyes(FACE));
            var pose = poses.last();
            vertex(buffer, pose, -.66F, -.56F, 0, 1); vertex(buffer, pose, .66F, -.56F, 1, 1);
            vertex(buffer, pose, .66F, .56F, 1, 0); vertex(buffer, pose, -.66F, .56F, 0, 0);
            poses.popPose();
            super.render(entity, yaw, partial, poses, buffers, light);
        }
        private void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v) {
            buffer.vertex(pose.pose(), x,y,.75F).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(),0,0,1).endVertex();
        }
        private static void sphere(VertexConsumer out,PoseStack.Pose pose,int ring,int segment,float radius,int light){
            double latitude=Math.PI*ring/12,longitude=Math.PI*2*segment/24;
            float x=(float)(Math.sin(latitude)*Math.cos(longitude)),y=(float)Math.cos(latitude),z=(float)(Math.sin(latitude)*Math.sin(longitude));
            out.vertex(pose.pose(),x*radius,y*radius*1.08F,z*radius).color(255,255,255,255).uv(segment/24F,ring/12F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),x,y,z).endVertex();
        }
    }
}
