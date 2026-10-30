package io.github.maniadiaz.bedroom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID)
public final class MatchHunger {
    static void keepFull(ServerPlayer p){if(MatchController.member(p)){p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(0);p.getFoodData().setExhaustion(0);}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.player instanceof ServerPlayer p)keepFull(p);}
    private MatchHunger(){}
}
