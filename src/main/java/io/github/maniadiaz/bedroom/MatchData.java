package io.github.maniadiaz.bedroom;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent server-authoritative rules, independent of teleportation and rendering. */
public final class MatchData extends SavedData {
    enum Phase { LOBBY, ACTIVE, REST }
    enum State { LOBBY, ACTIVE, LIMBO, SPECTATING, REST }
    static final int LIMBO_TICKS = 600;
    static final int REST_TICKS = 1800;
    final Map<UUID, Member> members = new LinkedHashMap<>();
    final Map<UUID, CompoundTag> pendingReturns = new LinkedHashMap<>();
    final Map<UUID, CompoundTag> waiting = new LinkedHashMap<>();
    Phase phase = Phase.LOBBY;
    UUID host;
    int round;
    int level;
    int restTicks;
    boolean stopping;
    boolean awaitingPlayers;

    static final class Member {
        final UUID id;
        State state = State.LOBBY;
        int deaths;
        int slot;
        int limboTicks;
        CompoundTag external = new CompoundTag();
        CompoundTag inventory = new CompoundTag();
        CompoundTag location = new CompoundTag();
        Member(UUID id) { this.id = id; }
    }

    static MatchData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MatchData::load, MatchData::new, "bedroom_match");
    }

    boolean join(UUID id) {
        if (members.containsKey(id)) return true;
        if (phase != Phase.LOBBY || members.size() >= 4) return false;
        Member member = new Member(id);
        for (int slot = 0; slot < 4; slot++) {
            final int candidate = slot;
            if (members.values().stream().noneMatch(m -> m.slot == candidate)) { member.slot = slot; break; }
        }
        members.put(id, member);
        if (host == null) host = id;
        setDirty();
        return true;
    }

    boolean start(UUID actor, Set<UUID> online) {
        if (phase != Phase.LOBBY || !actor.equals(host) || members.isEmpty()
                || !online.containsAll(members.keySet())) return false;
        round++;
        level = 0;
        phase = Phase.ACTIVE;
        members.values().forEach(m -> { m.state = State.ACTIVE; m.deaths = 0; m.limboTicks = 0; m.inventory = new CompoundTag(); });
        setDirty();
        return true;
    }

    boolean die(UUID id) {
        Member m = members.get(id);
        if (phase != Phase.ACTIVE || m == null || m.state != State.ACTIVE) return false;
        m.deaths++;
        m.state = m.deaths <= 2 ? State.LIMBO : State.SPECTATING;
        m.limboTicks = m.state == State.LIMBO ? LIMBO_TICKS : 0;
        setDirty();
        return true;
    }

    boolean escape(UUID id) {
        Member m = members.get(id);
        if (phase != Phase.ACTIVE || m == null || m.state != State.LIMBO || m.limboTicks <= 0) return false;
        m.state = State.ACTIVE;
        m.limboTicks = 0;
        setDirty();
        return true;
    }

    boolean complete(UUID id, boolean testSuccessor) {
        Member m = members.get(id);
        if (phase != Phase.ACTIVE || m == null || m.state != State.ACTIVE) return false;
        if (!testSuccessor) reset();
        else {
            phase = Phase.REST;
            restTicks = REST_TICKS;
            level++;
            members.values().forEach(p -> { p.state = State.REST; p.deaths = 0; p.limboTicks = 0; });
            setDirty();
        }
        return true;
    }

    boolean resume(UUID actor) {
        if (phase != Phase.REST || !actor.equals(host)) return false;
        phase = Phase.ACTIVE;
        restTicks = 0;
        members.values().forEach(m -> m.state = State.ACTIVE);
        setDirty();
        return true;
    }

    void reset() {
        phase = Phase.LOBBY;
        level = 0;
        restTicks = 0;
        members.values().forEach(m -> { m.state = State.LOBBY; m.deaths = 0; m.limboTicks = 0; m.inventory = new CompoundTag(); });
        setDirty();
    }

    void remove(UUID id) {
        members.remove(id);
        if (id.equals(host)) host = members.keySet().stream().findFirst().orElse(null);
        if (members.isEmpty()) reset();
        setDirty();
    }

    void tick(Set<UUID> online) {
        if (phase == Phase.LOBBY) {
            for (UUID id : java.util.List.copyOf(members.keySet())) {
                if (!online.contains(id)) {
                    pendingReturns.put(id, members.get(id).external.copy());
                    remove(id);
                }
            }
        }
        if (host == null || !online.contains(host)) {
            UUID replacement = members.keySet().stream().filter(online::contains).findFirst().orElse(host);
            if (!java.util.Objects.equals(host, replacement)) { host = replacement; setDirty(); }
        }
        if (phase == Phase.ACTIVE) {
            members.values().forEach(m -> {
                if (m.state == State.LIMBO && --m.limboTicks <= 0) { m.limboTicks = 0; m.state = State.SPECTATING; }
            });
            boolean recoverable = members.values().stream().anyMatch(m ->
                    (m.state == State.ACTIVE && online.contains(m.id)) || (m.state == State.LIMBO && m.limboTicks > 0));
            if (!recoverable) reset();
            setDirty();
        } else if (phase == Phase.REST) {
            if (--restTicks <= 0) {
                phase = Phase.ACTIVE;
                restTicks = 0;
                members.values().forEach(m -> m.state = State.ACTIVE);
            }
            setDirty();
        }
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putString("phase", phase.name());
        if (host != null) tag.putUUID("host", host);
        tag.putInt("round", round);
        tag.putInt("level", level);
        tag.putInt("restTicks", restTicks);
        ListTag list = new ListTag();
        members.values().forEach(m -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", m.id);
            t.putString("state", m.state.name());
            t.putInt("deaths", m.deaths);
            t.putInt("slot", m.slot);
            t.putInt("limboTicks", m.limboTicks);
            t.put("external", m.external.copy());
            t.put("inventory", m.inventory.copy());
            t.put("location", m.location.copy());
            list.add(t);
        });
        tag.put("members", list);
        ListTag returns = new ListTag();
        pendingReturns.forEach((id, external) -> {
            CompoundTag t = new CompoundTag(); t.putUUID("id", id); t.put("external", external.copy()); returns.add(t);
        });
        tag.put("pendingReturns", returns);
        ListTag queue = new ListTag();
        waiting.forEach((id, external) -> {
            CompoundTag t = new CompoundTag(); t.putUUID("id", id); t.put("external", external.copy()); queue.add(t);
        });
        tag.put("waiting", queue);
        return tag;
    }

    static MatchData load(CompoundTag tag) {
        MatchData data = new MatchData();
        data.phase = Phase.valueOf(tag.getString("phase"));
        data.awaitingPlayers = data.phase != Phase.LOBBY;
        data.host = tag.hasUUID("host") ? tag.getUUID("host") : null;
        data.round = tag.getInt("round");
        data.level = tag.getInt("level");
        data.restTicks = tag.getInt("restTicks");
        ListTag list = tag.getList("members", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Member m = new Member(t.getUUID("id"));
            m.state = State.valueOf(t.getString("state"));
            m.deaths = t.getInt("deaths");
            m.slot = t.getInt("slot");
            m.limboTicks = t.getInt("limboTicks");
            m.external = t.getCompound("external").copy();
            m.inventory = t.getCompound("inventory").copy();
            m.location = t.getCompound("location").copy();
            data.members.put(m.id, m);
        }
        ListTag returns = tag.getList("pendingReturns", Tag.TAG_COMPOUND);
        for (int i = 0; i < returns.size(); i++) {
            CompoundTag t = returns.getCompound(i);
            data.pendingReturns.put(t.getUUID("id"), t.getCompound("external").copy());
        }
        ListTag queue = tag.getList("waiting", Tag.TAG_COMPOUND);
        for (int i = 0; i < queue.size(); i++) {
            CompoundTag t = queue.getCompound(i);
            data.waiting.put(t.getUUID("id"), t.getCompound("external").copy());
        }
        return data;
    }
}
