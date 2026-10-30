package io.github.maniadiaz.bedroom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Separate from live match state so a fresh run or lobby reset never deletes the save slot. */
final class CheckpointData extends SavedData {
    CompoundTag snapshot=new CompoundTag();
    int highestRound;
    static CheckpointData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(CheckpointData::load,CheckpointData::new,"backrooms_checkpoint");}
    private static CheckpointData load(CompoundTag t){var d=new CheckpointData();d.snapshot=t.getCompound("snapshot").copy();d.highestRound=t.getInt("highestRound");return d;}
    @Override public CompoundTag save(CompoundTag t){t.put("snapshot",snapshot.copy());t.putInt("highestRound",highestRound);return t;}
}
