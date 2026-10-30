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
                .then(Commands.literal("objective").executes(c -> objective(c.getSource().getPlayerOrException())))
                .then(Commands.literal("journal").executes(c->{LevelZeroQuest.journal(c.getSource().getPlayerOrException());return 1;}))
                .then(difficultyCommands())
                .then(Commands.literal("restart").requires(s -> s.hasPermission(2)
                        || (s.getEntity() instanceof ServerPlayer p && (canRestart(p) || (MatchData.get(p.server).phase != MatchData.Phase.LOBBY && MatchData.get(p.server).members.containsKey(p.getUUID())))))
                        .executes(c -> {
                            if (c.getSource().getEntity() instanceof ServerPlayer p && MatchData.get(p.server).phase != MatchData.Phase.LOBBY) requestVote(p.server);
                            else restart(c.getSource().getServer());
                            return 1;
                        }))
                .then(Commands.literal("vote")
                        .then(Commands.literal("restart").executes(c -> ballot(c.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("continue").executes(c -> ballot(c.getSource().getPlayerOrException(), false))))
                .then(Commands.literal("spectate").executes(c -> spectate(c.getSource().getPlayerOrException(), 1))
                        .then(Commands.literal("next").executes(c -> spectate(c.getSource().getPlayerOrException(), 1)))
                        .then(Commands.literal("previous").executes(c -> spectate(c.getSource().getPlayerOrException(), -1)))
                        .then(Commands.literal("off").executes(c -> stopWatching(c.getSource().getPlayerOrException()))))
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
        event.getDispatcher().register(Commands.literal("backrooms").redirect(event.getDispatcher().getRoot().getChild("bedroom")));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> difficultyCommands(){
        var command=Commands.literal("difficulty").executes(c->{
            var d=MatchData.get(c.getSource().getServer());
            c.getSource().sendSuccess(()->Component.literal("Dificultad: "+d.difficulty.id+" · "+d.difficulty.size+" × "+d.difficulty.size),false);return 1;
        });
        for(var option:MatchDifficulty.values())command.then(Commands.literal(option.id).executes(c->{
            var d=MatchData.get(c.getSource().getServer());var actor=c.getSource().getEntity();
            if(d.phase!=MatchData.Phase.LOBBY||(!c.getSource().hasPermission(2)&&(!(actor instanceof ServerPlayer p)||!p.getUUID().equals(d.host)))){
                c.getSource().sendFailure(Component.literal("Solo el anfitrión u operador puede cambiar la dificultad desde el lobby."));return 0;
            }
            d.difficulty=option;d.setDirty();
            c.getSource().getServer().getPlayerList().broadcastSystemMessage(Component.literal("BACKROOMS · "+option.id+" · "+option.size+" × "+option.size),false);return 1;
        }));return command;
    }
    static int objective(ServerPlayer p){
        var d=MatchData.get(p.server);if(!SurvivalSystem.active(p))return 0;
        if(d.level==0){LevelZeroQuest.journal(p);tell(p,MatchArena.direction(p,LevelZeroQuest.target(p,d)));return 1;}
        BlockPos target=MatchArena.exit(d);
        String title="SALA DE CONTROL / SALIDA";
        for(int i=0;i<3;i++)if(!d.mission.done(i)){target=MissionSystem.panel(d,i);title="PANEL "+(i+1);break;}
        tell(p,title+" · "+MatchArena.direction(p,target)+" · X "+target.getX()+" Z "+target.getZ());return 1;
    }

    static int join(ServerPlayer p) {
        MatchData d = MatchData.get(p.server);
        if (dimension(p.server) == null) { tell(p, "La dimensión Backrooms no está disponible."); return 0; }
        if (d.members.containsKey(p.getUUID())) { tell(p, "Ya perteneces a la partida. Usa /backrooms status."); return 0; }
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
        d.observers.remove(p.getUUID());
        d.cameras.remove(p.getUUID());
        d.setDirty();
        p.getInventory().clearContent();
        p.experienceLevel = 0; p.totalExperience = 0; p.experienceProgress = 0;
        apply(p, d, m, false);
        tell(p, "Lobby: botón sobre el bloque verde al fondo. El anfitrión inicia. /backrooms leave para salir.");
        tell(p,"Dificultad: "+d.difficulty.id+" · "+d.difficulty.size+" × "+d.difficulty.size+". Anfitrión: /backrooms difficulty principiante|facil|normal|dificil|hardcore");
        return 1;
    }

    static int leave(ServerPlayer p) {
        AtmosphereSystem.clear(p);
        FlashlightSystem.clear(p);
        MatchData d = MatchData.get(p.server);
        MatchData.Member m = d.members.get(p.getUUID());
        if (m == null) {
            CompoundTag external = d.waiting.remove(p.getUUID());
            d.observers.remove(p.getUUID()); d.cameras.remove(p.getUUID());
            if (external == null) return 0;
            returnExternal(p, external); d.setDirty(); BedroomNetwork.sync(p); return 1;
        }
        returnExternal(p, m.external);
        d.remove(p.getUUID());
        BedroomNetwork.sync(p);
        tell(p, "Saliste de Backrooms. Inventario externo restaurado.");
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
        AtmosphereSystem.clear(p);
        FlashlightSystem.clear(p);
        p.closeContainer(); p.getInventory().clearContent(); p.removeAllEffects(); p.clearFire();
        p.setGameMode(GameType.ADVENTURE); p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(5);
        p.experienceLevel = 0; p.totalExperience = 0; p.experienceProgress = 0;
        move(p, MatchRooms.LOBBY, true, false);
        BedroomNetwork.sync(p);
        MatchAnnouncements.show(p, "ESPERA AL EQUIPO", "Espera al próximo inicio de partida.", net.minecraft.ChatFormatting.GOLD);
    }

    static boolean canRestart(ServerPlayer p) {
        return p.hasPermissions(2) || p.server.isSingleplayerOwner(p.getGameProfile())
                || p.getUUID().equals(MatchData.get(p.server).host);
    }

    static void requestVote(MinecraftServer server) {
        MatchData d = MatchData.get(server);
        if (d.phase == MatchData.Phase.LOBBY) return;
        Set<UUID> eligible = online(server); eligible.retainAll(d.members.keySet());
        if (!d.vote.start(eligible)) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) if (member(player)) {
            MatchAnnouncements.show(player, "¿VOLVEMOS?", "Voten: reiniciar o continuar sin él.", net.minecraft.ChatFormatting.GOLD);
            if (eligible.contains(player.getUUID())) {
                player.sendSystemMessage(Component.literal("¿Reiniciar la partida? ")
                        .append(Component.literal("[REINICIAR] ").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/backrooms vote restart"))))
                        .append(Component.literal("[SEGUIR]").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/backrooms vote continue")))));
            }
        }
    }

    private static int ballot(ServerPlayer p, boolean restart) {
        MatchData d = MatchData.get(p.server);
        if (!d.vote.cast(p.getUUID(), restart)) { tell(p, "No tienes una votación disponible."); return 0; }
        tell(p, restart ? "Votaste reiniciar." : "Votaste seguir.");
        resolveVote(p.server, d.vote.result());
        return 1;
    }

    private static boolean resolveVote(MinecraftServer server, RestartVote.Result result) {
        if (result == RestartVote.Result.PENDING) return false;
        MatchData d = MatchData.get(server); d.vote.clear();
        if (result == RestartVote.Result.RESTART) { restart(server); return true; }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (member(p))
            MatchAnnouncements.show(p, "SEGUIMOS", "La partida continúa. No te rindas.", net.minecraft.ChatFormatting.AQUA);
        return false;
    }

    private static java.util.List<ServerPlayer> cameraTargets(ServerPlayer p, MatchData d) {
        return d.members.values().stream().filter(m -> m.state == MatchData.State.ACTIVE)
                .map(m -> p.server.getPlayerList().getPlayer(m.id)).filter(java.util.Objects::nonNull).toList();
    }

    private static int spectate(ServerPlayer p, int direction) {
        MatchData d = MatchData.get(p.server);
        MatchData.Member m = d.members.get(p.getUUID());
        if (!d.waiting.containsKey(p.getUUID()) && (m == null || m.state != MatchData.State.SPECTATING)) return 0;
        var targets = cameraTargets(p, d);
        if (targets.isEmpty()) { tell(p, "No hay compañeros activos para observar."); return 0; }
        UUID selected = nextCamera(targets.stream().map(ServerPlayer::getUUID).toList(), d.cameras.get(p.getUUID()), direction);
        ServerPlayer target = targets.stream().filter(t -> t.getUUID().equals(selected)).findFirst().orElseThrow();
        if (d.waiting.containsKey(p.getUUID())) d.observers.add(p.getUUID());
        d.cameras.put(p.getUUID(), target.getUUID());
        followCamera(p, d); BedroomNetwork.sync(p);
        tell(p, "Observando a " + target.getScoreboardName() + ". Cambia con /backrooms spectate next o previous.");
        return 1;
    }

    static UUID nextCamera(java.util.List<UUID> targets, UUID current, int direction) {
        if (targets.isEmpty()) return null;
        int index = targets.indexOf(current);
        return targets.get(Math.floorMod(index < 0 ? 0 : index + direction, targets.size()));
    }

    private static int stopWatching(ServerPlayer p) {
        MatchData d = MatchData.get(p.server);
        if (!d.waiting.containsKey(p.getUUID())) return 0;
        d.observers.remove(p.getUUID()); d.cameras.remove(p.getUUID()); waitingLobby(p); return 1;
    }

    private static void followCamera(ServerPlayer p, MatchData d) {
        var targets = cameraTargets(p, d);
        ServerPlayer target = targets.stream().filter(t -> t.getUUID().equals(d.cameras.get(p.getUUID())))
                .findFirst().orElse(targets.isEmpty() ? null : targets.get(0));
        p.setGameMode(GameType.SPECTATOR);
        if (target != null) {
            d.cameras.put(p.getUUID(), target.getUUID());
            if (p.level() != target.level()) p.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            else p.setPos(target.getX(), target.getY(), target.getZ());
            p.setCamera(target);
        } else {
            p.setCamera(p); p.teleportTo(dimension(p.server), MatchRooms.LOBBY + 2.5, 65, 2.5, 0, 0);
        }
    }

    static void restart(MinecraftServer server) {
        Map<UUID, ServerPlayer> connected = new java.util.LinkedHashMap<>();
        server.getPlayerList().getPlayers().forEach(p -> connected.put(p.getUUID(), p));
        restart(server, connected);
    }

    static void restart(MinecraftServer server, Map<UUID, ServerPlayer> connected) {
        MatchData data = MatchData.get(server);
        data.reset();
        data.tick(connected.keySet());
        for (MatchData.Member member : data.members.values()) {
            ServerPlayer player = connected.get(member.id);
            if (player != null) apply(player, data, member, false);
        }
        for (UUID id : java.util.List.copyOf(data.waiting.keySet())) {
            ServerPlayer player = connected.get(id);
            if (player == null) continue;
            if (data.members.size() < 4) join(player);
            else waitingLobby(player);
        }
        for (ServerPlayer player : connected.values()) if (member(player)) {
            BedroomNetwork.sync(player);
            MatchAnnouncements.show(player, "REINICIANDO", "De vuelta al lobby. Reúne al equipo.", net.minecraft.ChatFormatting.GOLD);
        }
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
        ServerLevel level = Campaign.level(p.server,data.level);
        if (level == null) return;
        int x = MissionSystem.anchor(data) + 8;
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

    static void apply(ServerPlayer p, MatchData d, MatchData.Member m, boolean restoreInventory) {
        AtmosphereSystem.clear(p);
        FlashlightSystem.clear(p);
        p.removeAllEffects();
        p.clearFire();
        p.setHealth(p.getMaxHealth());
        p.setGameMode(m.state == MatchData.State.SPECTATING ? GameType.SPECTATOR : GameType.ADVENTURE);
        if (m.state == MatchData.State.ACTIVE || m.state == MatchData.State.REST) {
            if (restoreInventory) restore(p, m.inventory);
            for(int slot=0;slot<p.getInventory().getContainerSize();slot++) {
                var stack=p.getInventory().getItem(slot);
                if(stack.is(Bedroom.FUSE.get())||stack.is(Bedroom.RELAY.get())) {
                    var tag=stack.getTag();int index=tag==null?-1:tag.getInt("Index");
                    if(index<0||index>2||!MissionSystem.valid(stack,d,index))p.getInventory().setItem(slot,net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
            p.setHealth(p.getMaxHealth());
            if (m.state == MatchData.State.REST) move(p, MatchRooms.REST, false, false);
            else moveArena(p, d);
        } else if(m.state==MatchData.State.VICTORY){
            p.getInventory().clearContent();Celebration.ensure(dimension(p.server));move(p,Celebration.ROOM,false,false);
        } else {
            p.getInventory().clearContent();
            move(p, m.state == MatchData.State.LOBBY ? MatchRooms.LOBBY : MatchRooms.WHITE + m.slot * 32, true, m.state == MatchData.State.LIMBO);
        }
        if (m.state == MatchData.State.LOBBY || m.state == MatchData.State.REST || m.state==MatchData.State.VICTORY) {
            p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(5);
        }
        if (m.state == MatchData.State.LOBBY) {
            p.experienceLevel = 0; p.totalExperience = 0; p.experienceProgress = 0;
        }
        m.location = location(p);
        d.setDirty();
        BedroomNetwork.sync(p);
        MatchAnnouncements.state(p, m, d.level);
        tell(p, "Backrooms: " + m.state + (m.state == MatchData.State.LIMBO ? " — 30 segundos: busca el botón de salida." : ""));
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
        Map<UUID,ServerPlayer> players=new java.util.LinkedHashMap<>();
        p.server.getPlayerList().getPlayers().forEach(player->players.put(player.getUUID(),player));
        return complete(p,successor,players);
    }
    static boolean complete(ServerPlayer p, boolean successor,Map<UUID,ServerPlayer> players) {
        MatchData d = MatchData.get(p.server);
        if (d.phase != MatchData.Phase.ACTIVE || !d.members.containsKey(p.getUUID())
                || d.members.get(p.getUUID()).state != MatchData.State.ACTIVE) return false;
        d.members.values().forEach(m -> {
            ServerPlayer player = players.get(m.id);
            if (player != null && m.state == MatchData.State.ACTIVE) m.inventory = snapshot(player);
        });
        boolean advance=d.level!=1;
        if(d.level==0&&d.quest.ready())for(var member:d.members.values()){var player=players.get(member.id);if(player!=null){var advancement=p.server.getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"level_zero_complete"));if(advancement!=null)player.getAdvancements().award(advancement,"escape");}}
        if (!(advance?d.complete(p.getUUID(),true):d.win(p.getUUID()))) return false;
        for (MatchData.Member m : d.members.values()) {
            ServerPlayer player = players.get(m.id);
            if (player != null) {
                apply(player, d, m, true);
                MatchAnnouncements.show(player, advance?"NIVEL SUPERADO":"¡ESCAPARON!", advance ? "Equipo reunido. Próximo destino: "+Campaign.name(d.level)+"." : "Campaña completa. Han escapado de Backrooms.", net.minecraft.ChatFormatting.GREEN);
            }
        }
        tell(p,advance?"Nivel superado. Descanso de 90 segundos antes de "+Campaign.name(d.level)+".":"¡Victoria! Celebren: regreso al lobby en 20 segundos o con el botón.");
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
        if (Campaign.dungeon(p.level().dimension()) && p.distanceToSqr(event.getPos().getCenter()) <= 25) {
            if (m.state == MatchData.State.ACTIVE && Campaign.exit(p.level().getBlockState(event.getPos()))) {
                handled = true;
                tell(p,MissionSystem.exitReady(d)?"Mantén agacharse (Shift) y camina hacia delante contra esta pared durante 2 segundos.":"La pared no responde. Completa las misiones y consulta /backrooms journal.");
            } else
            if(m.state==MatchData.State.VICTORY&&event.getPos().equals(MatchRooms.button(Celebration.ROOM))){
                handled=true;if(p.getUUID().equals(d.host))restart(p.server);
            }else if (m.state == MatchData.State.LOBBY && event.getPos().equals(MatchRooms.button(MatchRooms.LOBBY))) {
                handled = true;
                if (!(dimension(p.server).getChunkSource().getGenerator() instanceof Level0Generator)) {
                    tell(p, "Este mundo conserva el generador antiguo. Crea un mundo nuevo para jugar al Nivel 0 procedural.");
                } else if (d.start(p.getUUID(), online(p.server))) {
                    MissionSystem.prepare(p.server,d);
                    for (MatchData.Member other : d.members.values()) {
                        ServerPlayer player = p.server.getPlayerList().getPlayer(other.id);
                        if (player != null) {
                            player.getInventory().clearContent();
                            player.getInventory().add(new net.minecraft.world.item.ItemStack(Bedroom.NUTRITION_BAR.get(), 3));
                            player.getInventory().add(new net.minecraft.world.item.ItemStack(Bedroom.ALMOND_WATER.get()));
                            player.getInventory().add(new net.minecraft.world.item.ItemStack(Bedroom.FLASHLIGHT.get()));
                            apply(player, d, other, false);
                        }
                    }
                } else tell(p, "Solo el anfitrión puede iniciar y todos deben estar conectados.");
            } else if (m.state == MatchData.State.LIMBO) {
                if (event.getPos().equals(MatchRooms.button(MatchRooms.WHITE + m.slot * 32))) {
                    handled = true;
                    if (d.escape(p.getUUID())) {
                        apply(p, d, m, true);
                        MatchAnnouncements.show(p, "HAS VUELTO", "Aún tienes una oportunidad.", net.minecraft.ChatFormatting.GREEN);
                    }
                }
            } else if (m.state == MatchData.State.ACTIVE && event.getPos().equals(MatchRooms.button(MatchRooms.arena(d)))) {
                handled = true;
                tell(p,"La salida ahora requiere tres paneles reparados y atravesar la pared anómala.");
            } else if (m.state == MatchData.State.REST && event.getPos().equals(MatchRooms.button(MatchRooms.REST))) {
                handled = true;
                if (d.resume(p.getUUID())) { MissionSystem.prepare(p.server,d); for (MatchData.Member other : d.members.values()) {
                    ServerPlayer player = p.server.getPlayerList().getPlayer(other.id);
                    if (player != null) apply(player, d, other, false);
                }
                }
            }
        }
        if(!handled)handled=MissionSystem.interact(p,event.getPos(),event.getHand());
        // Mission inserts are validated here; ordinary placement remains denied.
        if (!handled && m.state == MatchData.State.ACTIVE && (p.getItemInHand(event.getHand()).isEdible() || p.getItemInHand(event.getHand()).getItem() instanceof SurvivalItems.Supply)
                && !p.level().getBlockState(event.getPos()).is(net.minecraft.tags.BlockTags.create(
                        ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "containers")))) {
            // Deny the wall/floor interaction while allowing the normal food-use fallback.
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            return;
        }
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
            if (LevelZeroQuest.hidden(p)||MatchData.get(p.server).waiting.containsKey(p.getUUID()) || (m != null && (m.state != MatchData.State.ACTIVE||MatchData.get(p.server).exitDelay>0))) event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p && (member(p)
                || (Campaign.dungeon(p.level().dimension()) && !p.isCreative()))) event.setCanceled(true);
    }

    @SubscribeEvent public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && (member(p)
                || (Campaign.dungeon(p.level().dimension()) && !p.isCreative()))) event.setCanceled(true);
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
        if (Campaign.dungeon(event.getLevel().dimension())) event.getAffectedBlocks().clear();
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MatchData d = MatchData.get(p.server);
            CompoundTag pending = d.pendingReturns.remove(p.getUUID());
            if (pending != null) {
                returnExternal(p, pending);
                d.setDirty();
                if (BedroomConfig.AUTO_LOBBY.get()) join(p);
                if (d.phase != MatchData.Phase.LOBBY) requestVote(p.server);
                return;
            }
            MatchData.Member m = d.members.get(p.getUUID());
            if (m != null) {
                // Migration of an older saved run: returning never resumes progress.
                d.waiting.put(p.getUUID(), m.external.copy());
                d.remove(p.getUUID());
                join(p);
                if (d.phase != MatchData.Phase.LOBBY) requestVote(p.server);
            } else if (d.waiting.containsKey(p.getUUID())) waitingLobby(p);
            else if (BedroomConfig.AUTO_LOBBY.get()) join(p);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            AtmosphereSystem.clear(p);
            FlashlightSystem.clear(p);
            MatchData d = MatchData.get(p.server);
            MatchData.Member m = d.members.get(p.getUUID());
            if (m != null) {
                d.pendingReturns.put(p.getUUID(), m.external.copy());
                d.remove(p.getUUID());
            }
            d.observers.remove(p.getUUID()); d.cameras.remove(p.getUUID());
            d.setDirty();
        }
    }

    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && BedroomConfig.AUTO_LOBBY.get() && !member(p)) join(p);
    }

    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        AtmosphereSystem.clearAll();
        FlashlightSystem.clearAll();
        MatchData data = MatchData.get(event.getServer());
        data.stopping = true;
        data.setDirty();
    }

    @SubscribeEvent public static void started(net.minecraftforge.event.server.ServerStartedEvent event) {
        // A loaded world is a new attempt, never a checkpoint for an interrupted run.
        if (MatchData.get(event.getServer()).phase != MatchData.Phase.LOBBY) restart(event.getServer());
    }

    @SubscribeEvent public static void finishedUse(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer p && SurvivalSystem.active(p))
            MatchData.get(p.server).members.get(p.getUUID()).survival.quietTicks = 0;
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
            if (MatchData.get(p.server).waiting.containsKey(p.getUUID()) || (m != null && (m.state != MatchData.State.ACTIVE||((event.getEntity().getItem().is(Bedroom.FUSE.get())||event.getEntity().getItem().is(Bedroom.RELAY.get()))||event.getEntity().getItem().is(Bedroom.RELAY.get()))))) {
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
        if (d.vote.ticks > 0) {
            Set<UUID> voters = new java.util.HashSet<>(connected); voters.retainAll(d.members.keySet());
            if (resolveVote(server, d.vote.tick(voters))) return;
        }
        Map<UUID, MatchData.State> before = new HashMap<>();
        MatchData.Phase previousPhase = d.phase;
        d.members.values().forEach(m -> before.put(m.id, m.state));
        d.tick(connected);
        if(previousPhase==MatchData.Phase.REST&&d.phase==MatchData.Phase.ACTIVE)MissionSystem.prepare(server,d);
        for (UUID id : java.util.List.copyOf(d.waiting.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            if (d.phase == MatchData.Phase.LOBBY && d.members.size() < 4) join(player);
            else {
                if (!d.observers.contains(id) && player.isSpectator()) waitingLobby(player);
                if (d.observers.contains(id)) followCamera(player, d);
                if (server.getTickCount() % 20 == 0) BedroomNetwork.sync(player);
            }
        }
        for (MatchData.Member m : d.members.values()) {
            ServerPlayer p = server.getPlayerList().getPlayer(m.id);
            if (p == null) continue;
            if(m.state==MatchData.State.ACTIVE&&p.serverLevel()==Campaign.level(server,d.level))MatchArena.contain(p,d);
            SurvivalSystem.tick(p, m, d);
            if(m.state==MatchData.State.ACTIVE)LevelZeroQuest.tick(p);else d.quest.hiding.remove(p.getUUID());
            if (before.get(m.id) != m.state) apply(p, d, m, true);
            if (previousPhase == MatchData.Phase.ACTIVE && d.phase == MatchData.Phase.LOBBY)
                MatchAnnouncements.show(p, "DERROTA", "Todos cayeron. Vuelvan a intentarlo.", net.minecraft.ChatFormatting.RED);
            if (m.state == MatchData.State.SPECTATING) followCamera(p, d);
            if (server.getTickCount() % 20 == 0) {
                m.location = location(p); d.setDirty();
                if (m.state == MatchData.State.ACTIVE || m.state == MatchData.State.REST) {
                    // Do not close menus during periodic persistence.
                    m.inventory = inventorySnapshot(p); d.setDirty();
                }
                BedroomNetwork.sync(p);
            }
        }
    }
}
