package io.github.maniadiaz.bedroom;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID)
public final class MatchController {
    private MatchController() {}

    static boolean member(ServerPlayer player) {
        MatchData d = MatchData.get(player.server);
        return d.members.containsKey(player.getUUID()) || d.waiting.containsKey(player.getUUID());
    }
    private static Set<UUID> online(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).collect(Collectors.toSet());
    }
    private static void tell(ServerPlayer p, String text) { p.sendSystemMessage(Component.literal(text)); }
    private static ServerLevel dimension(MinecraftServer server) { return server.getLevel(DevelopmentCommands.LEVEL_0); }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bedroom")
                .then(Commands.literal("lobby").executes(c -> join(c.getSource().getPlayerOrException())))
                .then(Commands.literal("leave").executes(c -> leave(c.getSource().getPlayerOrException())))
                .then(Commands.literal("status").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MatchData d = MatchData.get(p.server);
                    MatchData.Member m = d.members.get(p.getUUID());
                    tell(p, "Partida: " + d.phase + " | Jugadores: " + d.members.size() + "/4 | En espera: " + d.waiting.size() + (m == null ? "" : " | " + m.state + " | Muertes: " + m.deaths));
                    return 1;
                }))
                .then(Commands.literal("testdeath").requires(s -> s.hasPermission(2) && BedroomConfig.DEVELOPMENT_COMMANDS.get()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return death(p) ? 1 : 0;
                }))
                .then(Commands.literal("testnext").requires(s -> s.hasPermission(2) && BedroomConfig.DEVELOPMENT_COMMANDS.get()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return complete(p, true) ? 1 : 0;
                })));
    }

    static int join(ServerPlayer p) {
        MatchData d = MatchData.get(p.server);
        if (dimension(p.server) == null) { tell(p, "La dimensión Bedroom no está disponible."); return 0; }
        if (d.members.containsKey(p.getUUID())) { tell(p, "Ya perteneces a la partida. Usa /bedroom status."); return 0; }
        CompoundTag external = d.waiting.get(p.getUUID());
        if (external == null) external = externalSnapshot(p);
        if (!d.join(p.getUUID())) {
            d.waiting.put(p.getUUID(), external);
            d.setDirty();
            waitingLobby(p);
            tell(p, "Estás en el lobby de espera. Entrarás al grupo cuando haya puesto libre y no haya partida en curso.");
            return 1;
        }
        MatchData.Member m = d.members.get(p.getUUID());
        m.external = external;
        d.waiting.remove(p.getUUID());
        d.setDirty();
        p.getInventory().clearContent();
        p.experienceLevel = 0; p.totalExperience = 0; p.experienceProgress = 0;
        apply(p, d, m, false);
        tell(p, "Lobby: botón sobre el bloque verde al fondo. El anfitrión inicia. /bedroom leave para salir.");
        return 1;
    }

    static int leave(ServerPlayer p) {
        MatchData d = MatchData.get(p.server);
        MatchData.Member m = d.members.get(p.getUUID());
        if (m == null) {
            CompoundTag external = d.waiting.remove(p.getUUID());
            if (external == null) return 0;
            returnExternal(p, external); d.setDirty(); return 1;
        }
        returnExternal(p, m.external);
        d.remove(p.getUUID());
        tell(p, "Saliste de Bedroom. Inventario externo restaurado.");
        return 1;
    }

    private static void returnExternal(ServerPlayer p, CompoundTag external) {
        ResourceLocation id = ResourceLocation.tryParse(external.getString("dimension"));
        ServerLevel target = id == null ? null : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (target == null) target = p.server.overworld();
        p.setCamera(p);
        restore(p, external);
        p.setGameMode(GameType.byId(external.getInt("mode")));
        p.teleportTo(target, external.getDouble("x"), external.getDouble("y"), external.getDouble("z"), external.getFloat("yaw"), external.getFloat("pitch"));
        p.fallDistance = 0;
    }

    private static CompoundTag snapshot(ServerPlayer p) {
        p.closeContainer();
        return inventorySnapshot(p);
    }

    private static CompoundTag externalSnapshot(ServerPlayer p) {
        CompoundTag t = snapshot(p);
        t.putString("dimension", p.level().dimension().location().toString());
        t.putDouble("x", p.getX()); t.putDouble("y", p.getY()); t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot()); t.putFloat("pitch", p.getXRot());
        t.putInt("mode", p.gameMode.getGameModeForPlayer().getId());
        return t;
    }

    private static void waitingLobby(ServerPlayer p) {
        p.closeContainer(); p.getInventory().clearContent(); p.removeAllEffects(); p.clearFire();
        p.setGameMode(GameType.ADVENTURE); p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(5);
        p.experienceLevel = 0; p.totalExperience = 0; p.experienceProgress = 0;
        move(p, MatchRooms.LOBBY, true, false);
    }

    private static CompoundTag inventorySnapshot(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.put("items", p.getInventory().save(new ListTag()));
        t.putInt("food", p.getFoodData().getFoodLevel());
        t.putFloat("saturation", p.getFoodData().getSaturationLevel());
        t.putFloat("health", p.getHealth());
        t.putInt("xpLevel", p.experienceLevel); t.putInt("xpTotal", p.totalExperience); t.putFloat("xp", p.experienceProgress);
        ListTag effects = new ListTag();
        p.getActiveEffects().forEach(effect -> effects.add(effect.save(new CompoundTag())));
        t.put("effects", effects);
        return t;
    }

    private static void restore(ServerPlayer p, CompoundTag t) {
        p.closeContainer();
        p.getInventory().clearContent();
        p.getInventory().load(t.getList("items", Tag.TAG_COMPOUND));
        p.getFoodData().setFoodLevel(t.contains("food") ? t.getInt("food") : 20);
        p.getFoodData().setSaturation(t.getFloat("saturation"));
        p.setHealth(t.contains("health") ? Math.max(1, t.getFloat("health")) : p.getMaxHealth());
        p.experienceLevel = t.getInt("xpLevel"); p.totalExperience = t.getInt("xpTotal"); p.experienceProgress = t.getFloat("xp");
        p.removeAllEffects();
        ListTag effects = t.getList("effects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effects.size(); i++) {
            var effect = net.minecraft.world.effect.MobEffectInstance.load(effects.getCompound(i));
            if (effect != null) p.addEffect(effect);
        }
        p.getInventory().setChanged();
        p.inventoryMenu.broadcastChanges();
    }

    private static void move(ServerPlayer p, int x, boolean white, boolean maze) {
        ServerLevel level = dimension(p.server);
        if (level == null) return;
        MatchRooms.ensure(level, x, white, maze);
        BlockPos pos = MatchRooms.spawn(x);
        p.setCamera(p);
        p.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        p.fallDistance = 0;
        p.setDeltaMovement(0, 0, 0);
    }

    private static void moveArena(ServerPlayer p, MatchData data) {
        ServerLevel level = dimension(p.server);
        if (level == null) return;
        int x = MatchRooms.arena(data) + 8;
        level.getChunkAt(new BlockPos(x, 65, 8));
        p.setCamera(p);
        p.teleportTo(level, x + 0.5, 65, 8.5, 0, 0);
        p.fallDistance = 0; p.setDeltaMovement(0, 0, 0);
    }

    private static CompoundTag location(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putDouble("x", p.getX()); t.putDouble("y", p.getY()); t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot()); t.putFloat("pitch", p.getXRot());
        return t;
    }

    private static void apply(ServerPlayer p, MatchData d, MatchData.Member m, boolean restoreInventory) {
        p.removeAllEffects();
        p.clearFire();
        p.setHealth(p.getMaxHealth());
        p.setGameMode(m.state == MatchData.State.SPECTATING ? GameType.SPECTATOR : GameType.ADVENTURE);
        if (m.state == MatchData.State.ACTIVE || m.state == MatchData.State.REST) {
            if (restoreInventory) restore(p, m.inventory);
            p.setHealth(p.getMaxHealth());
            if (m.state == MatchData.State.REST) move(p, MatchRooms.REST, false, false);
            else moveArena(p, d);
        } else {
            p.getInventory().clearContent();
            move(p, m.state == MatchData.State.LOBBY ? MatchRooms.LOBBY : MatchRooms.WHITE + m.slot * 32, true, m.state == MatchData.State.LIMBO);
        }
        if (m.state == MatchData.State.LOBBY || m.state == MatchData.State.REST) {
            p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(5);
        }
        if (m.state == MatchData.State.LOBBY) {
            p.experienceLevel = 0; p.totalExperience = 0; p.experienceProgress = 0;
        }
        m.location = location(p);
        d.setDirty();
        tell(p, "Bedroom: " + m.state + (m.state == MatchData.State.LIMBO ? " — 30 segundos: busca el botón de salida." : ""));
    }

    static boolean death(ServerPlayer p) {
        MatchData d = MatchData.get(p.server);
        MatchData.Member m = d.members.get(p.getUUID());
        if (m == null || m.state != MatchData.State.ACTIVE || d.phase != MatchData.Phase.ACTIVE) return false;
        m.inventory = snapshot(p);
        if (!d.die(p.getUUID())) return false;
        apply(p, d, m, false);
        return true;
    }

    static boolean complete(ServerPlayer p, boolean successor) {
        MatchData d = MatchData.get(p.server);
        if (d.phase != MatchData.Phase.ACTIVE || !d.members.containsKey(p.getUUID())
                || d.members.get(p.getUUID()).state != MatchData.State.ACTIVE) return false;
        d.members.values().forEach(m -> {
            ServerPlayer player = p.server.getPlayerList().getPlayer(m.id);
            if (player != null && m.state == MatchData.State.ACTIVE) m.inventory = snapshot(player);
        });
        if (!d.complete(p.getUUID(), successor)) return false;
        for (MatchData.Member m : d.members.values()) {
            ServerPlayer player = p.server.getPlayerList().getPlayer(m.id);
            if (player != null) apply(player, d, m, true);
        }
        tell(p, successor ? "Transición de prueba: descanso de 90 segundos. No es el Nivel 1." : "Nivel 0 completado. Regreso provisional al lobby.");
        return true;
    }

    @SubscribeEvent public static void buttons(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        MatchData d = MatchData.get(p.server);
        MatchData.Member m = d.members.get(p.getUUID());
        if (m == null) {
            if (d.waiting.containsKey(p.getUUID())) {
                event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        boolean handled = false;
        if (p.level().dimension().equals(DevelopmentCommands.LEVEL_0) && p.distanceToSqr(event.getPos().getCenter()) <= 25) {
            if (m.state == MatchData.State.ACTIVE && p.level().getBlockState(event.getPos()).is(Bedroom.EXIT.get())) {
                handled = true;
                complete(p, false);
            } else
            if (m.state == MatchData.State.LOBBY && event.getPos().equals(MatchRooms.button(MatchRooms.LOBBY))) {
                handled = true;
                if (!(dimension(p.server).getChunkSource().getGenerator() instanceof Level0Generator)) {
                    tell(p, "Este mundo conserva el generador antiguo. Crea un mundo nuevo para jugar al Nivel 0 procedural.");
                } else if (d.start(p.getUUID(), online(p.server))) {
                    for (MatchData.Member other : d.members.values()) {
                        ServerPlayer player = p.server.getPlayerList().getPlayer(other.id);
                        if (player != null) { player.getInventory().clearContent(); apply(player, d, other, false); }
                    }
                } else tell(p, "Solo el anfitrión puede iniciar y todos deben estar conectados.");
            } else if (m.state == MatchData.State.LIMBO) {
                if (event.getPos().equals(MatchRooms.button(MatchRooms.WHITE + m.slot * 32))) {
                    handled = true;
                    if (d.escape(p.getUUID())) apply(p, d, m, true);
                }
            } else if (m.state == MatchData.State.ACTIVE && event.getPos().equals(MatchRooms.button(MatchRooms.arena(d)))) {
                handled = true;
                complete(p, false);
            } else if (m.state == MatchData.State.REST && event.getPos().equals(MatchRooms.button(MatchRooms.REST))) {
                handled = true;
                if (d.resume(p.getUUID())) for (MatchData.Member other : d.members.values()) {
                    ServerPlayer player = p.server.getPlayerList().getPlayer(other.id);
                    if (player != null) apply(player, d, other, false);
                }
            }
        }
        // No placement/containers in non-active states; all mission placements remain denied until registered.
        if (handled || !p.level().getBlockState(event.getPos()).is(net.minecraft.tags.BlockTags.create(
                ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "containers"))) || m.state != MatchData.State.ACTIVE) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && member(p)) {
            event.setCanceled(true);
            if (!death(p)) p.setHealth(p.getMaxHealth());
        }
    }

    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MatchData.Member m = MatchData.get(p.server).members.get(p.getUUID());
            if (MatchData.get(p.server).waiting.containsKey(p.getUUID()) || (m != null && m.state != MatchData.State.ACTIVE)) event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p && (member(p)
                || (p.level().dimension().equals(DevelopmentCommands.LEVEL_0) && !p.isCreative()))) event.setCanceled(true);
    }

    @SubscribeEvent public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && (member(p)
                || (p.level().dimension().equals(DevelopmentCommands.LEVEL_0) && !p.isCreative()))) event.setCanceled(true);
    }

    @SubscribeEvent public static void itemUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MatchData.Member m = MatchData.get(p.server).members.get(p.getUUID());
            if (MatchData.get(p.server).waiting.containsKey(p.getUUID()) || (m != null && (m.state != MatchData.State.ACTIVE || p.getItemInHand(event.getHand()).getItem() instanceof net.minecraft.world.item.BucketItem))) event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void entityUse(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer p && member(p)) event.setCanceled(true);
    }

    @SubscribeEvent public static void entityUseSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer p && member(p)) event.setCanceled(true);
    }

    @SubscribeEvent public static void explosions(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        if (event.getLevel().dimension().equals(DevelopmentCommands.LEVEL_0)) event.getAffectedBlocks().clear();
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MatchData d = MatchData.get(p.server);
            CompoundTag pending = d.pendingReturns.remove(p.getUUID());
            if (pending != null) {
                returnExternal(p, pending);
                d.setDirty();
                if (BedroomConfig.AUTO_LOBBY.get()) join(p);
                return;
            }
            MatchData.Member m = d.members.get(p.getUUID());
            if (m != null) {
                CompoundTag last = m.location.copy();
                apply(p, d, m, true);
                int room = m.state == MatchData.State.LIMBO ? MatchRooms.WHITE + m.slot * 32 : MatchRooms.arena(d);
                double x = last.getDouble("x"), y = last.getDouble("y"), z = last.getDouble("z");
                boolean withinArea = m.state == MatchData.State.ACTIVE ? Math.abs(x) < 29000000 && Math.abs(z) < 29000000
                        : x > room && x < room + 20 && z > 0 && z < 20;
                if ((m.state == MatchData.State.ACTIVE || m.state == MatchData.State.LIMBO)
                        && withinArea && y >= 65 && y < 68
                        && dimension(p.server).getBlockState(BlockPos.containing(x, y, z)).isAir()
                        && dimension(p.server).getBlockState(BlockPos.containing(x, y + 1, z)).isAir()) {
                    p.teleportTo(dimension(p.server), x, y, z, last.getFloat("yaw"), last.getFloat("pitch"));
                    m.location = location(p);
                }
            } else if (d.waiting.containsKey(p.getUUID())) waitingLobby(p);
            else if (BedroomConfig.AUTO_LOBBY.get()) join(p);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MatchData d = MatchData.get(p.server);
            MatchData.Member m = d.members.get(p.getUUID());
            if (m != null && (m.state == MatchData.State.ACTIVE || m.state == MatchData.State.REST)) {
                m.inventory = snapshot(p); d.setDirty();
            }
            if (m != null) { m.location = location(p); d.setDirty(); }
        }
    }

    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && BedroomConfig.AUTO_LOBBY.get() && !member(p)) join(p);
    }

    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        MatchData data = MatchData.get(event.getServer());
        data.stopping = true;
        data.setDirty();
    }

    @SubscribeEvent public static void pickup(net.minecraftforge.event.entity.player.EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MatchData.Member m = MatchData.get(p.server).members.get(p.getUUID());
            if (MatchData.get(p.server).waiting.containsKey(p.getUUID()) || (m != null && m.state != MatchData.State.ACTIVE)) event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void toss(net.minecraftforge.event.entity.item.ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p) {
            MatchData.Member m = MatchData.get(p.server).members.get(p.getUUID());
            if (MatchData.get(p.server).waiting.containsKey(p.getUUID()) || (m != null && m.state != MatchData.State.ACTIVE)) {
                p.getInventory().placeItemBackInInventory(event.getEntity().getItem());
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        MatchData d = MatchData.get(server);
        if ((d.members.isEmpty() && d.waiting.isEmpty()) || d.stopping) return;
        Set<UUID> connected = online(server);
        if (d.awaitingPlayers) {
            if (d.members.keySet().stream().noneMatch(connected::contains)) return;
            d.awaitingPlayers = false;
        }
        // Saving and quitting an integrated server must not resolve the solo game as a defeat.
        if (!server.isDedicatedServer() && connected.isEmpty()) return;
        Map<UUID, MatchData.State> before = new HashMap<>();
        d.members.values().forEach(m -> before.put(m.id, m.state));
        d.tick(connected);
        for (UUID id : java.util.List.copyOf(d.waiting.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            if (d.phase == MatchData.Phase.LOBBY && d.members.size() < 4) join(player);
            else if (server.getTickCount() % 20 == 0) player.displayClientMessage(Component.literal("Lobby: esperando un puesto para la siguiente partida (" + d.members.size() + "/4)"), true);
        }
        for (MatchData.Member m : d.members.values()) {
            ServerPlayer p = server.getPlayerList().getPlayer(m.id);
            if (p == null) continue;
            if (before.get(m.id) != m.state) apply(p, d, m, true);
            if (m.state == MatchData.State.SPECTATING) {
                ServerPlayer target = d.members.values().stream().filter(other -> other.state == MatchData.State.ACTIVE)
                        .map(other -> server.getPlayerList().getPlayer(other.id)).filter(java.util.Objects::nonNull).findFirst().orElse(null);
                if (target != null) {
                    if (p.level() != target.level()) p.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
                    else p.setPos(target.getX(), target.getY(), target.getZ());
                    p.setCamera(target);
                } else {
                    p.setCamera(p);
                    p.teleportTo(dimension(server), MatchRooms.WHITE + m.slot * 32 + 2.5, 65, 2.5, 0, 0);
                }
            }
            if (server.getTickCount() % 20 == 0) {
                m.location = location(p); d.setDirty();
                if (m.state == MatchData.State.ACTIVE || m.state == MatchData.State.REST) {
                    // Do not close menus during periodic persistence.
                    m.inventory = inventorySnapshot(p); d.setDirty();
                }
                String text = m.state == MatchData.State.LIMBO ? "Limbo: " + ((m.limboTicks + 19) / 20) + " s"
                        : m.state == MatchData.State.REST ? "Descanso: " + ((d.restTicks + 19) / 20) + " s"
                        : "Bedroom: " + m.state + " | Muertes " + m.deaths + "/3";
                p.displayClientMessage(Component.literal(text), true);
            }
        }
    }
}
