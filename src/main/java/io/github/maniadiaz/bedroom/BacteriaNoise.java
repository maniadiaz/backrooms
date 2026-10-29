package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Capture the location of an interaction, rather than tracking a silent player afterward. */
@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID)
public final class BacteriaNoise {
    private record Noise(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 position, long tick) { }
    private static final Map<UUID,Noise> noises = new HashMap<>();
    static void record(ServerPlayer p) {
        if (SurvivalSystem.active(p) && !p.isSpectator()) noises.put(p.getUUID(),new Noise(p.level().dimension(),p.position(),p.level().getGameTime()));
    }
    static Vec3 heard(BacteriaEntity entity, ServerPlayer p) {
        Noise noise = noises.get(p.getUUID());
        if (noise == null || !noise.dimension.equals(entity.level().dimension()) || entity.level().getGameTime()-noise.tick > 20
                || entity.position().distanceToSqr(noise.position) > 225) return null;
        return noise.position;
    }
    @SubscribeEvent public static void hitBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p) record(p);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (MatchData.get(event.getServer()).phase != MatchData.Phase.ACTIVE) noises.clear();
        else noises.entrySet().removeIf(entry -> {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(entry.getKey());
            return p == null || !SurvivalSystem.active(p) || p.level().getGameTime()-entry.getValue().tick > 20;
        });
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { noises.clear(); }
    private BacteriaNoise() { }
}
