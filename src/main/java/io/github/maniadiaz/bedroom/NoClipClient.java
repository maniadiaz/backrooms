package io.github.maniadiaz.bedroom;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID,value=Dist.CLIENT)
public final class NoClipClient {
    private static boolean latched,last;
    private static int clock,blackTicks;
    static void blackout(int ticks){blackTicks=Math.max(0,Math.min(40,ticks));}
    static boolean black(){return blackTicks>0;}
    static void reset(){latched=false;last=false;clock=0;blackTicks=0;}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){reset();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.isPaused())return;
        if(blackTicks>0)blackTicks--;
        if(mc.player==null||mc.level==null){reset();return;}
        boolean forward=BedroomHud.activeLevel()&&mc.screen==null&&mc.options.keyUp.isDown();
        // Vanilla cancels sprint on a solid wall; retain the run intent while forward remains held.
        if(!forward)latched=false;
        else if(mc.player.isSprinting()||mc.options.keySprint.isDown())latched=true;
        boolean pressing=forward&&latched;
        if(pressing!=last||++clock%10==0){BedroomNetwork.intent(pressing);last=pressing;}
    }
    private NoClipClient(){}
}
