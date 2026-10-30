package io.github.maniadiaz.bedroom;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID,value=Dist.CLIENT)
public final class NoClipClient {
    private static boolean last;
    private static int clock,blackTicks;
    static void blackout(int ticks){blackTicks=Math.max(0,Math.min(40,ticks));}
    static boolean black(){return blackTicks>0;}
    static void reset(){last=false;clock=0;blackTicks=0;}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){reset();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.isPaused())return;
        if(blackTicks>0)blackTicks--;
        if(mc.player==null||mc.level==null){reset();return;}
        boolean forward=BedroomHud.activeLevel()&&mc.screen==null&&mc.options.keyUp.isDown();
        // Use the assigned sneak control, including toggle sneak and remapped keys.
        boolean pressing=forward&&mc.options.keyShift.isDown();
        if(pressing!=last||++clock%10==0){BedroomNetwork.intent(pressing);last=pressing;}
    }
    private NoClipClient(){}
}
