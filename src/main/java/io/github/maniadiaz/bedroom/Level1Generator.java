package io.github.maniadiaz.bedroom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Warehouses and loading aisles; global coordinates make adjacent chunks agree. */
public final class Level1Generator extends Level0Generator {
    static final Codec<Level1Generator> CODEC=RecordCodecBuilder.create(i->i.group(Biome.CODEC.fieldOf("biome").forGetter(g->g.settings().getBiome())).apply(i,Level1Generator::new));
    public Level1Generator(Holder<Biome> biome){super(biome);}
    @Override protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec(){return CODEC;}
    @Override protected int ceiling(){return 72;}
    static boolean aisle(int x,int z){int dx=Math.floorMod(x,16),dz=Math.floorMod(z,16);return dx==7||dx==8||dz==7||dz==8;}
    @Override BlockState stateAt(int x,int y,int z){
        if(y==0)return Blocks.BEDROCK.defaultBlockState();
        if(y==64)return(layout.wet(x,z)?Bedroom.WET_CONCRETE.get():Bedroom.CONCRETE.get()).defaultBlockState();
        if(y==72)return Bedroom.STEEL.get().defaultBlockState();
        if(y<65||y>71)return Blocks.AIR.defaultBlockState();
        if(layout.lamp(x,z)&&aisle(x,z)&&y>=70)return(y==70?(layout.deadLamp(x,z)?Bedroom.DEAD_LIGHT.get():Bedroom.LIGHT.get()):Blocks.CHAIN).defaultBlockState();
        if(layout.exit(x,z)&&y<=67)return Bedroom.LOADING_EXIT.get().defaultBlockState();
        if(y==65&&layout.supply(x,z))return Bedroom.FILING_CABINET.get().defaultBlockState();
        if(Math.floorMod(x,128)>=112&&Math.floorMod(z,128)>=112)return Blocks.AIR.defaultBlockState();
        if(aisle(x,z))return Blocks.AIR.defaultBlockState();
        int dx=Math.floorMod(x,32),dz=Math.floorMod(z,32);
        if(dx==0||dz==0)return Bedroom.STEEL.get().defaultBlockState();
        if(Math.floorMod(x,16)==0&&Math.floorMod(z,16)==0)return Bedroom.CONCRETE.get().defaultBlockState();
        if(y<=66&&Math.floorMod(x,16)>=2&&Math.floorMod(x,16)<=4&&Math.floorMod(z,16)>=2&&Math.floorMod(z,16)<=4)return Bedroom.LOADING_CRATE.get().defaultBlockState();
        if(y<=67&&Math.floorMod(x,16)==11&&Math.floorMod(z,16)>=10&&Math.floorMod(z,16)<=14)return Bedroom.LOADING_CRATE.get().defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }
}
