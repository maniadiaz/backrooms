package io.github.maniadiaz.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;

final class MatchRooms {
    static final int LOBBY = 64;
    static final int REST = 104;
    static final int WHITE = 200;
    static int arena(MatchData data) { return 1048576 + data.round * 512 + data.level * 32; }
    static BlockPos button(int x) { return new BlockPos(x + 18, 66, 19); }
    static BlockPos continueButton(){return new BlockPos(LOBBY+14,66,19);}
    static void continueControl(ServerLevel level){var button=continueButton();level.setBlock(button.below(),Blocks.BLUE_CONCRETE.defaultBlockState(),3);level.setBlock(button,Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE,AttachFace.FLOOR),3);}
    static BlockPos spawn(int x) { return new BlockPos(x + 2, 65, 2); }

    static void ensure(ServerLevel level, int x, boolean white, boolean maze) {
        if(x==Celebration.ROOM){Celebration.ensure(level);return;}
        if (level.getBlockState(new BlockPos(x, 64, 0)).is(Blocks.BEDROCK)) {if(x==LOBBY)continueControl(level);return;}
        Block wall = white ? Blocks.WHITE_CONCRETE : Bedroom.WALL.get();
        for (int dx = 0; dx <= 20; dx++) for (int z = 0; z <= 20; z++) {
            level.setBlock(new BlockPos(x + dx, 64, z), (white ? Blocks.WHITE_CONCRETE : Blocks.BROWN_WOOL).defaultBlockState(), 3);
            level.setBlock(new BlockPos(x + dx, 68, z), ((dx % 4 == 2 && z % 4 == 2) ? Blocks.SEA_LANTERN : Blocks.WHITE_CONCRETE).defaultBlockState(), 3);
            boolean edge = dx == 0 || dx == 20 || z == 0 || z == 20;
            boolean divider = maze && dx > 0 && dx < 20 && dx % 4 == 0
                    && (dx % 8 == 4 ? z < 17 : z > 3);
            for (int y = 65; y <= 67; y++) level.setBlock(new BlockPos(x + dx, y, z),
                    (edge || divider ? wall : Blocks.AIR).defaultBlockState(), 3);
        }
        BlockPos button = button(x);
        level.setBlock(button.below(), Blocks.LIME_CONCRETE.defaultBlockState(), 3);
        level.setBlock(button, Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.FLOOR), 3);
        level.setBlock(new BlockPos(x, 64, 0), Blocks.BEDROCK.defaultBlockState(), 3);
        if(x==LOBBY)continueControl(level);
    }

    private MatchRooms() {}
}
