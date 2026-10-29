package io.github.maniadiaz.bedroom;

import net.minecraft.util.RandomSource;

/** Per-player timers advance only while the level is active. No audio volume affects these clocks. */
final class AmbientSchedule {
    int footsteps;
    int drip;
    int fault;
    boolean initialized;
    record Events(boolean footsteps, boolean drip, boolean fault) {}
    Events tick(RandomSource random) {
        if (!initialized) {
            footsteps = nextFootsteps(random); drip = nextDrip(random); fault = nextFault(random); initialized = true;
        }
        boolean stepsNow = --footsteps <= 0, dripNow = --drip <= 0, faultNow = --fault <= 0;
        if (stepsNow) footsteps = nextFootsteps(random);
        if (dripNow) drip = nextDrip(random);
        if (faultNow) fault = nextFault(random);
        return new Events(stepsNow, dripNow, faultNow);
    }
    static int nextFootsteps(RandomSource random) { return 900 + random.nextInt(901); }
    static int nextDrip(RandomSource random) { return 400 + random.nextInt(501); }
    static int nextFault(RandomSource random) { return 400 + random.nextInt(401); }
}
