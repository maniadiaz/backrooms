package io.github.maniadiaz.bedroom;

import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

final class RestartVote {
    enum Result { PENDING, RESTART, CONTINUE }
    final Set<UUID> voters = new LinkedHashSet<>();
    final java.util.Map<UUID, Boolean> ballots = new HashMap<>();
    int ticks;
    boolean start(Set<UUID> eligible) {
        if (ticks > 0 || eligible.isEmpty()) return false;
        voters.addAll(eligible); ticks = 600; return true;
    }
    boolean cast(UUID id, boolean restart) {
        if (ticks <= 0 || !voters.contains(id)) return false;
        ballots.put(id, restart); return true;
    }
    int yes() { return (int) ballots.values().stream().filter(Boolean::booleanValue).count(); }
    Result result() {
        if (voters.isEmpty() || ticks <= 0) return Result.CONTINUE;
        int required = voters.size() / 2 + 1;
        if (yes() >= required) return Result.RESTART;
        if (ballots.size() - yes() > voters.size() - required) return Result.CONTINUE;
        return Result.PENDING;
    }
    Result tick(Set<UUID> connected) {
        voters.retainAll(connected); ballots.keySet().retainAll(voters);
        if (ticks > 0) ticks--;
        return result();
    }
    void clear() { ticks = 0; voters.clear(); ballots.clear(); }
}
