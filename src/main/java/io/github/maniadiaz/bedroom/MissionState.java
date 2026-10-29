package io.github.maniadiaz.bedroom;

import net.minecraft.nbt.CompoundTag;

/** Shared objective progress. Leases invalidate recovered copies of lost mission items. */
final class MissionState {
    int installed;
    final int[] leases = new int[3];
    boolean ready() { return installed == 7; }
    int count() { return Integer.bitCount(installed & 7); }
    boolean done(int index) { return (installed & (1 << index)) != 0; }
    boolean install(int index,int lease) {
        if (index<0 || index>2 || done(index) || lease<=0 || leases[index]!=lease) return false;
        installed |= 1 << index; return true;
    }
    CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putInt("installed",installed); tag.putIntArray("leases",leases); return tag;
    }
    static MissionState load(CompoundTag tag) {
        MissionState state = new MissionState(); state.installed = tag.getInt("installed") & 7;
        int[] values = tag.getIntArray("leases");
        for (int i=0;i<Math.min(3,values.length);i++) state.leases[i] = Math.max(0,values[i]);
        return state;
    }
}
