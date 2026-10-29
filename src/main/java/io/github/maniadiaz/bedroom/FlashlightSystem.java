package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;

final class FlashlightSystem {
    private record Point(ServerLevel level, BlockPos pos) {}
    private static final Map<UUID, Set<Point>> beams = new HashMap<>();
    private static final Map<Point, Set<UUID>> owners = new HashMap<>();
    static ItemStack held(ServerPlayer p) {
        for (ItemStack stack : new ItemStack[] { p.getMainHandItem(), p.getOffhandItem() })
            if (stack.is(Bedroom.FLASHLIGHT.get()) && SurvivalItems.on(stack)) return stack;
        return ItemStack.EMPTY;
    }
    static void tick(ServerPlayer p) {
        ItemStack lamp = held(p);
        if (lamp.isEmpty() || SurvivalItems.charge(lamp) <= 0) { clear(p); return; }
        lamp.getOrCreateTag().putInt("Charge", SurvivalItems.charge(lamp) - 1);
        if (SurvivalItems.charge(lamp) == 0) { lamp.getOrCreateTag().putBoolean("On", false); clear(p); return; }
        if (p.server.getTickCount() % 10 != 0) return;
        project(p);
    }
    static void project(ServerPlayer p) {
        clear(p);
        Set<Point> points = new HashSet<>();
        var from = p.getEyePosition();
        for (float yaw : new float[] { -.17F, 0, .17F }) {
            var direction = p.getLookAngle().yRot(yaw);
            var hit = p.level().clip(new ClipContext(from, from.add(direction.scale(8)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            double distance = Math.max(.2, from.distanceTo(hit.getLocation()) - .25);
            for (int step : new int[] {1, 3, 5, 7}) {
                BlockPos pos = BlockPos.containing(from.add(direction.scale(Math.min(step, distance))));
                if (!p.level().getBlockState(pos).isAir()) continue;
                Point point = new Point(p.serverLevel(), pos);
                points.add(point); owners.computeIfAbsent(point, key -> new HashSet<>()).add(p.getUUID());
                p.serverLevel().setBlock(pos, Bedroom.BEAM_LIGHT.get().defaultBlockState(), 3);
                p.serverLevel().scheduleTick(pos, Bedroom.BEAM_LIGHT.get(), 20);
            }
        }
        beams.put(p.getUUID(), points);
    }
    static void clear(ServerPlayer p) { clear(p.getUUID()); }
    private static void clear(UUID id) {
        for (Point point : beams.getOrDefault(id, Set.of())) {
            Set<UUID> users = owners.get(point);
            if (users == null) continue;
            users.remove(id);
            if (users.isEmpty()) {
                owners.remove(point);
                if (point.level.getBlockState(point.pos).is(Bedroom.BEAM_LIGHT.get())) point.level.removeBlock(point.pos, false);
            }
        }
        beams.remove(id);
    }
    static void clearAll() { for (UUID id : List.copyOf(beams.keySet())) clear(id); }
    static final class BeamLight extends AirBlock {
        BeamLight() { super(Properties.of().air().replaceable().noCollission().noOcclusion().noLootTable().lightLevel(s -> 12)); }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!owners.containsKey(new Point(level, pos))) level.removeBlock(pos, false);
            else level.scheduleTick(pos, this, 20);
        }
    }
    private FlashlightSystem() {}
}
