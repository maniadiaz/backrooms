package io.github.maniadiaz.bedroom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public final class DarkroomsGenerator extends Level0Generator {
    static final Codec<DarkroomsGenerator> CODEC=RecordCodecBuilder.create(i->i.group(Biome.CODEC.fieldOf("biome").forGetter(g->g.settings().getBiome())).apply(i,DarkroomsGenerator::new));
    public DarkroomsGenerator(Holder<Biome> biome){super(biome);}
    @Override protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec(){return CODEC;}
    @Override BlockState stateAt(int x,int y,int z){
        var state=super.stateAt(x,y,z);
        if(state.is(Bedroom.LIGHT.get()))return Bedroom.DEAD_LIGHT.get().defaultBlockState();
        if(state.is(Bedroom.WALL.get()))return net.minecraft.world.level.block.Blocks.GRAY_CONCRETE.defaultBlockState();
        return state;
    }
}
