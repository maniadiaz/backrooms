package io.github.maniadiaz.bedroom;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Level0GameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void connectedHundredRoomsAcrossSeeds(GameTestHelper h) {
        for (long seed : new long[] {12, 8901, -731}) {
            Level0Layout layout = new Level0Layout(seed);
            int start = -80, end = 80; // 10x10 cells, including negative chunks and their seams.
            var queue = new ArrayDeque<BlockPos>();
            var visited = new HashSet<BlockPos>();
            BlockPos origin = new BlockPos(-72, 65, -72);
            visited.add(origin); queue.add(origin);
            while (!queue.isEmpty()) {
                BlockPos p = queue.remove();
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos n = p.relative(direction);
                    if (n.getX() < start || n.getX() >= end || n.getZ() < start || n.getZ() >= end
                            || layout.wall(n.getX(), n.getZ()) || !visited.add(n)) continue;
                    queue.add(n);
                }
            }
            for (int x = start + 1; x < end - 1; x++) for (int z = start + 1; z < end - 1; z++) {
                if (!layout.wall(x, z)) h.assertTrue(visited.contains(new BlockPos(x, 65, z)), "Disconnected walkable block at " + x + "," + z + " seed " + seed);
            }
            for (int cx = -5; cx < 5; cx++) for (int cz = -5; cz < 5; cz++)
                h.assertTrue(visited.contains(new BlockPos(cx * 16 + 8, 65, cz * 16 + 8)), "Room centre must be reachable");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void generatedChunksAndCodec(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        h.assertTrue(level.getChunkSource().getGenerator() instanceof Level0Generator, "Dimension must use registered Bedroom generator");
        Level0Generator generator = (Level0Generator) level.getChunkSource().getGenerator();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess());
        var encoded = Level0Generator.CODEC.encodeStart(ops, generator).result().orElseThrow();
        h.assertTrue(Level0Generator.CODEC.parse(ops, encoded).result().isPresent(), "Generator must survive codec round trip");
        for (int x : new int[] {-4097, -4096, 4095, 4096}) for (int z = 4096; z < 4112; z++) {
            h.assertTrue(level.getBlockState(new BlockPos(x, 64, z)).is(Bedroom.CARPET.get()) || level.getBlockState(new BlockPos(x, 64, z)).is(Bedroom.WET_CARPET.get()), "Floor must continue across chunk seams");
            h.assertTrue(!level.getBlockState(new BlockPos(x, 68, z)).isAir(), "Ceiling must continue across chunk seams");
            for (int y = 65; y <= 67; y++) h.assertTrue(level.getBlockState(new BlockPos(x, y, z)).equals(generator.stateAt(x, y, z)), "Generated geometry disagrees with layout");
        }
        BlockPos centre = new BlockPos(4104, 65, 4104);
        for (int y = 65; y <= 67; y++) h.assertTrue(level.getBlockState(new BlockPos(centre.getX(), y, centre.getZ())).isAir(), "Room centre needs three free interior blocks");
        BlockPos exit = new BlockPos(4211, 65, 4211); // 32*128+115
        h.assertTrue(level.getBlockState(exit).is(Bedroom.EXIT.get()), "Each region needs its temporary exit");
        var panel = Bedroom.PANEL.get().defaultBlockState().setValue(PanelBlock.AXIS, Direction.Axis.X);
        h.assertTrue(panel.getCollisionShape(level, centre).bounds().getZsize() == .25, "Panel collision must be four pixels thick");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deterministicAndSeeded(GameTestHelper h) {
        Level0Layout a = new Level0Layout(1), b = new Level0Layout(1), c = new Level0Layout(2);
        int changes = 0;
        var types = new HashSet<Integer>();
        for (int x = -64; x < 64; x++) for (int z = -64; z < 64; z++) {
            h.assertTrue(a.wall(x, z) == b.wall(x, z) && a.wet(x, z) == b.wet(x, z), "Same seed must reproduce layout");
            if (a.wall(x, z) != c.wall(x, z)) changes++;
            types.add(a.template(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
        }
        h.assertTrue(changes > 100 && types.size() == 5, "Seeds must change rooms and expose five templates");
        h.succeed();
    }
}
