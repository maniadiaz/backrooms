package io.github.maniadiaz.bedroom;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CameraClient {
    private static final KeyMapping NEXT = new KeyMapping("key.bedroom.next_camera", org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_BRACKET, "key.categories.bedroom");
    private static final KeyMapping PREVIOUS = new KeyMapping("key.bedroom.previous_camera", org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_BRACKET, "key.categories.bedroom");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(NEXT); event.register(PREVIOUS); }
    @Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT)
    public static final class Keys {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (NEXT.consumeClick()) if (mc.player != null && mc.screen == null && BedroomHud.canCycleCamera()) mc.player.connection.sendCommand("bedroom spectate next");
            while (PREVIOUS.consumeClick()) if (mc.player != null && mc.screen == null && BedroomHud.canCycleCamera()) mc.player.connection.sendCommand("bedroom spectate previous");
        }
    }
    private CameraClient() {}
}
