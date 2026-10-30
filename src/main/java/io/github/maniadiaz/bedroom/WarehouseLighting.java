package io.github.maniadiaz.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Upgrade already-generated warehouse lamps after chunk loading, never force-load terrain. */
@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID)
public final class WarehouseLighting {
    private static final java.util.Set<Long> pending=java.util.concurrent.ConcurrentHashMap.newKeySet();
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(event.getLevel() instanceof ServerLevel level&&level.dimension().equals(Campaign.LEVEL_1))
            pending.add(event.getChunk().getPos().toLong());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){pending.clear();}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||pending.isEmpty())return;
        var level=event.getServer().getLevel(Campaign.LEVEL_1);if(level==null)return;
        for(int i=0;i<2&&!pending.isEmpty();i++){
            var iterator=pending.iterator();long key=iterator.next();iterator.remove();
            var pos=new net.minecraft.world.level.ChunkPos(key);
            var chunk=level.getChunkSource().getChunkNow(pos.x,pos.z);
            if(chunk!=null)upgrade(level,chunk);
        }
    }
    static void upgrade(ServerLevel level,LevelChunk chunk){
        var origin=chunk.getPos().getWorldPosition();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            BlockPos old=new BlockPos(origin.getX()+x,68,origin.getZ()+z),raised=old.above(2);
            var state=level.getBlockState(old);
            if(!state.is(Bedroom.LIGHT.get())&&!state.is(Bedroom.DEAD_LIGHT.get())&&!state.is(Blocks.SEA_LANTERN))continue;
            var destination=level.getBlockState(raised);
            if(!destination.isAir()&&!destination.is(Blocks.CHAIN))continue;
            if(state.is(Bedroom.LIGHT.get()))state=state.setValue(FluorescentBlock.FAULT,0);
            level.setBlock(raised,state,3);
            level.removeBlock(old,false);
            if(level.getBlockState(old.above()).is(Blocks.CHAIN))level.removeBlock(old.above(),false);
        }
    }
    private WarehouseLighting(){}
}
