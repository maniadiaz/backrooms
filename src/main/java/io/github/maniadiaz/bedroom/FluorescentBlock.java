package io.github.maniadiaz.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

final class FluorescentBlock extends Block {
    static final IntegerProperty FAULT = IntegerProperty.create("fault", 0, 4);
    FluorescentBlock() {
        super(Properties.of().strength(1).sound(SoundType.GLASS).lightLevel(s -> emission(s.getValue(FAULT))));
        registerDefaultState(stateDefinition.any().setValue(FAULT, 0));
    }
    static int emission(int phase) { return phase == 0 || phase == 2 || phase == 4 ? 13 : 0; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FAULT); }
    static boolean fail(ServerLevel level, BlockPos pos, boolean rapid, int darkTicks) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Bedroom.LIGHT.get()) || state.getValue(FAULT) != 0) return false;
        level.setBlock(pos, state.setValue(FAULT, rapid ? 1 : 3), 3);
        level.scheduleTick(pos, Bedroom.LIGHT.get(), rapid ? 6 : Math.max(20, darkTicks));
        return true;
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int phase = state.getValue(FAULT);
        if (phase == 0) return;
        int next = switch (phase) { case 1 -> 2; case 2 -> 3; case 3 -> BedroomConfig.RAPID_FLICKER.get() ? 4 : 0; default -> 0; };
        level.setBlock(pos, state.setValue(FAULT, next), 3);
        if (next != 0) level.scheduleTick(pos, this, next == 3 ? 40 + random.nextInt(41) : 6);
    }
}
