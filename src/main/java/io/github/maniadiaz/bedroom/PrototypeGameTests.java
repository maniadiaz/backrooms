package io.github.maniadiaz.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PrototypeGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void dimensionAndRoom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        helper.assertTrue(level != null, "Nivel 0 must load on a dedicated server");
        DevelopmentCommands.ensureRoom(level);
        helper.assertTrue(!level.dimensionType().hasSkyLight(), "Nivel 0 must not have skylight");
        for (int y = 65; y <= 67; y++) {
            helper.assertTrue(level.getBlockState(new BlockPos(8, y, 8)).isAir(), "Three free interior blocks required");
            helper.assertTrue(level.getBlockState(new BlockPos(0, y, 8)).is(Bedroom.WALL.get()), "Room must have walls");
        }
        helper.assertTrue(level.getBlockState(new BlockPos(8, 64, 8)).is(Blocks.BROWN_WOOL), "Missing floor");
        helper.assertTrue(level.getBlockState(new BlockPos(8, 68, 8)).is(Blocks.SEA_LANTERN), "Missing ceiling light");
        BlockPos edit = new BlockPos(7, 65, 7);
        level.setBlock(edit, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        DevelopmentCommands.ensureRoom(level);
        helper.assertTrue(level.getBlockState(edit).is(Blocks.GOLD_BLOCK), "Entering again must preserve room edits");
        level.setBlock(edit, Blocks.AIR.defaultBlockState(), 3);
        helper.succeed();
    }
}
