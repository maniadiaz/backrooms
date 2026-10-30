package io.github.maniadiaz.bedroom;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CompassClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(Bedroom.ANOMALOUS_COMPASS.get(), ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "angle"),
                (stack, level, entity, seed) -> {
                    if (level == null || entity == null || !Campaign.dungeon(level.dimension())) return .0F;
                    int rx = Math.floorDiv(entity.getBlockX(), 128), rz = Math.floorDiv(entity.getBlockZ(), 128);
                    double best = Double.MAX_VALUE, dx = 0, dz = 0;
                    for (int x = rx - 1; x <= rx + 1; x++) for (int z = rz - 1; z <= rz + 1; z++) {
                        double tx = x * 128 + 115.5 - entity.getX(), tz = z * 128 + 115.5 - entity.getZ();
                        if (tx * tx + tz * tz < best) { best = tx * tx + tz * tz; dx = tx; dz = tz; }
                    }
                    if(stack.hasTag()&&stack.getTag().contains("ExitX")){
                        dx=stack.getTag().getInt("ExitX")+.5-entity.getX();dz=stack.getTag().getInt("ExitZ")+.5-entity.getZ();best=dx*dx+dz*dz;
                    }
                    if (best >= 900) return (float) ((level.getGameTime() % 160) / 160.0);
                    double angle = Math.atan2(-dx, dz) / (Math.PI * 2) - entity.getYRot() / 360.0;
                    return (float) (angle - Math.floor(angle));
                }));
    }
    private CompassClient() {}
}
