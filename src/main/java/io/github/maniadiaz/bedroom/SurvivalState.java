package io.github.maniadiaz.bedroom;

import net.minecraft.nbt.CompoundTag;

final class SurvivalState {
    double sanity = 100;
    int quietTicks;
    int energyTicks;
    int hallucinationTicks = 400;
    int panicTicks;
    boolean ownsSlow;
    boolean ownsBlind;

    void advance(boolean dark, boolean threat, boolean accompanied, boolean companyHelps) {
        advance(dark,threat,accompanied,companyHelps,0);
    }
    void advance(boolean dark,boolean threat,boolean accompanied,boolean companyHelps,double multiplier){
        quietTicks = Math.min(quietTicks + 1, 360000);
        double humPerSecond = quietTicks > 3600 ? .5 / 60 * (accompanied && companyHelps ? .5 : 1) : 0;
        double drain=multiplier==0?drainPerSecond(dark?.1:0,humPerSecond,threat?.1:0)
                : Math.min(.3*multiplier,(.035+(dark?.16:0)+(threat?.18:0)+humPerSecond*3)*multiplier);
        sanity = Math.max(0, sanity - drain / 20);
        if (energyTicks > 0 && --energyTicks == 0) recover(-5);
    }
    static double drainPerSecond(double... sources) {
        double total = 0; for (double source : sources) total += Math.max(0, source);
        return Math.min(.3, total);
    }
    void recover(double amount) { sanity = Math.max(0, Math.min(100, sanity + amount)); }
    CompoundTag save() {
        CompoundTag t = new CompoundTag(); t.putDouble("sanity", sanity); t.putInt("quiet", quietTicks);
        t.putInt("energy", energyTicks); t.putInt("hallucination", hallucinationTicks); t.putInt("panic", panicTicks);
        t.putBoolean("ownsSlow", ownsSlow); t.putBoolean("ownsBlind", ownsBlind); return t;
    }
    static SurvivalState load(CompoundTag t) {
        SurvivalState s = new SurvivalState();
        if (t.contains("sanity")) s.sanity = Math.max(0, Math.min(100, t.getDouble("sanity")));
        s.quietTicks = t.getInt("quiet"); s.energyTicks = Math.max(0, t.getInt("energy"));
        s.hallucinationTicks = t.contains("hallucination") ? t.getInt("hallucination") : 400;
        s.panicTicks = t.getInt("panic"); s.ownsSlow = t.getBoolean("ownsSlow"); s.ownsBlind = t.getBoolean("ownsBlind"); return s;
    }
}
