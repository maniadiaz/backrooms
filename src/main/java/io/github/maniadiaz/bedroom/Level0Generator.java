package io.github.maniadiaz.bedroom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class Level0Generator extends FlatLevelSource {
    public static final Codec<Level0Generator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.CODEC.fieldOf("biome").forGetter(g -> g.settings().getBiome())
    ).apply(instance, Level0Generator::new));
    protected volatile Level0Layout layout = new Level0Layout(0);

    public Level0Generator(Holder<Biome> biome) {
        super(new FlatLevelGeneratorSettings(Optional.of(net.minecraft.core.HolderSet.direct(List.of())), biome, List.of()));
    }
    @Override protected Codec<? extends ChunkGenerator> codec() { return CODEC; }
    @Override public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structures, RandomState random, long seed) {
        layout = new Level0Layout(seed);
        return super.createState(structures, random, seed);
    }

    BlockState stateAt(int x, int y, int z) {
        if (y == 0) return Blocks.BEDROCK.defaultBlockState();
        if(y==62||y==63)return Bedroom.CARPET.get().defaultBlockState();
        if(y==69||y==70)return Bedroom.CEILING.get().defaultBlockState();
        if (y == 64) return (layout.wet(x, z) ? Bedroom.WET_CARPET.get() : Bedroom.CARPET.get()).defaultBlockState();
        if (y == 68) return (layout.lamp(x, z) ? (layout.deadLamp(x, z) ? Bedroom.DEAD_LIGHT.get() : Bedroom.LIGHT.get()) : Bedroom.CEILING.get()).defaultBlockState();
        if (y == 65 && layout.supply(x, z)) return (layout.hash(x, z, 90) % 2 == 0 ? Bedroom.CARDBOARD_BOX.get() : Bedroom.FILING_CABINET.get()).defaultBlockState();
        if (y >= 65 && y <= 67 && layout.wall(x, z)) {
            if (layout.exit(x, z)) return Bedroom.EXIT.get().defaultBlockState();
            return Bedroom.WALL.get().defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }
    protected int ceiling(){return 68;}

    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        // Each call writes only its own chunk; no shared random state or neighbour mutations.
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = chunk.getPos().getMinBlockX() + dx, z = chunk.getPos().getMinBlockZ() + dz;
            chunk.setBlockState(pos.set(x, 0, z), Blocks.BEDROCK.defaultBlockState(), false);
            for (int y = 62; y <= ceiling()+2; y++) chunk.setBlockState(pos.set(x, y, z), stateAt(x, y, z), false);
            if (layout.supply(x, z)) {
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.putString("id", "bedroom:supply_container"); tag.putInt("x", x); tag.putInt("y", 65); tag.putInt("z", z);
                tag.putString("LootTable", "bedroom:chests/supplies"); tag.putLong("LootTableSeed", layout.hash(x, z, 91));
                chunk.setBlockEntityNbt(tag);
            }
        }
        Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        return CompletableFuture.completedFuture(chunk);
    }
    @Override public int getMinY() { return 0; }
    @Override public int getGenDepth() { return 128; }
    @Override public int getSeaLevel() { return 0; }
    @Override public int getSpawnHeight(LevelHeightAccessor level) { return 65; }
    @Override public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) { return ceiling()+3; }
    @Override public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] column = new BlockState[128];
        for (int y = 0; y < column.length; y++) column[y] = stateAt(x, y, z);
        return new NoiseColumn(0, column);
    }
}
