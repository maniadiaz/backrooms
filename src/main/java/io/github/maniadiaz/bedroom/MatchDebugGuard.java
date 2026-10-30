package io.github.maniadiaz.bedroom;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Hide the vanilla coordinate/debug overlay for every participant and waiting observer. */
@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID,value=Dist.CLIENT)
public final class MatchDebugGuard {
    private static void clear(){if(BedroomHud.inMatch())Minecraft.getInstance().options.renderDebug=false;}
    @SubscribeEvent public static void key(InputEvent.Key event){clear();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase==TickEvent.Phase.END)clear();}
    @SubscribeEvent public static void overlay(RenderGuiOverlayEvent.Pre event){if(BedroomHud.inMatch()&&event.getOverlay().id().equals(VanillaGuiOverlay.DEBUG_TEXT.id()))event.setCanceled(true);}
    private MatchDebugGuard(){}
}
