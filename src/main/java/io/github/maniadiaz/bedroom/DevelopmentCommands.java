package io.github.maniadiaz.bedroom;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Server-only development tools. No gameplay entry or exit is implemented yet. */
public final class DevelopmentCommands {
    static final ResourceKey<Level> LEVEL_0 = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "level_0"));
    private static final String RETURN = "BedroomDevelopmentReturn";

    private DevelopmentCommands() {}

    public static void register(RegisterCommandsEvent event) {
        if (!BedroomConfig.DEVELOPMENT_COMMANDS.get()) return;
        event.getDispatcher().register(Commands.literal("bedroom")
                .then(Commands.literal("enter").requires(source -> source.hasPermission(2)).executes(context -> enter(context.getSource())))
                .then(Commands.literal("exit").requires(source -> source.hasPermission(2)).executes(context -> exit(context.getSource())))
                .then(Commands.literal("rescue").requires(source -> source.hasPermission(2)).executes(context -> rescue(context.getSource()))));
    }

    private static int enter(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (MatchController.member(player)) {
            source.sendFailure(Component.literal("Sal de la partida con /bedroom leave antes de usar comandos de desarrollo."));
            return 0;
        }
        ServerLevel level = source.getServer().getLevel(LEVEL_0);
        if (level == null) {
            source.sendFailure(Component.literal("No se encontró bedroom:level_0. Reinicia el servidor con el mod instalado."));
            return 0;
        }
        if (!player.level().dimension().equals(LEVEL_0)) {
            CompoundTag data = new CompoundTag();
            data.putString("dimension", player.level().dimension().location().toString());
            data.putDouble("x", player.getX());
            data.putDouble("y", player.getY());
            data.putDouble("z", player.getZ());
            data.putFloat("yaw", player.getYRot());
            data.putFloat("pitch", player.getXRot());
            player.getPersistentData().put(RETURN, data);
        }
        ensureRoom(level);
        player.teleportTo(level, 8.5, 65, 8.5, 0, 0);
        player.fallDistance = 0;
        source.sendSuccess(() -> Component.literal("Sala de prueba del Nivel 0. Regreso: /bedroom exit"), false);
        return 1;
    }

    private static int exit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (MatchController.member(player)) return MatchController.leave(player);
        if (!player.level().dimension().equals(LEVEL_0) || !player.getPersistentData().contains(RETURN)) {
            source.sendFailure(Component.literal("No hay un regreso guardado desde Nivel 0. Usa /bedroom rescue para volver al spawn."));
            return 0;
        }
        CompoundTag data = player.getPersistentData().getCompound(RETURN);
        ResourceLocation id = ResourceLocation.tryParse(data.getString("dimension"));
        ServerLevel target = id == null ? null : source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (target == null) {
            source.sendFailure(Component.literal("La dimensión de origen no está disponible. Usa /bedroom rescue."));
            return 0;
        }
        player.teleportTo(target, data.getDouble("x"), data.getDouble("y"), data.getDouble("z"), data.getFloat("yaw"), data.getFloat("pitch"));
        player.fallDistance = 0;
        player.getPersistentData().remove(RETURN);
        return 1;
    }

    private static int rescue(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (MatchController.member(player)) return MatchController.leave(player);
        ServerLevel target = source.getServer().overworld();
        BlockPos spawn = target.getSharedSpawnPos();
        // Vanilla's shared spawn is the administrator-defined emergency destination.
        player.teleportTo(target, spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, 0, 0);
        player.fallDistance = 0;
        player.getPersistentData().remove(RETURN);
        return 1;
    }

    static void ensureRoom(ServerLevel level) {
        // Marker avoids rebuilding the room and erasing creative edits on each visit.
        if (level.getBlockState(new BlockPos(0, 64, 0)).is(Blocks.BEDROCK)) return;
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                level.setBlock(new BlockPos(x, 64, z), Blocks.BROWN_WOOL.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 68, z),
                        (x % 4 == 0 && z % 4 == 0 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_QUARTZ).defaultBlockState(), 3);
                for (int y = 65; y <= 67; y++) {
                    level.setBlock(new BlockPos(x, y, z),
                            (x == 0 || z == 0 || x == 16 || z == 16 ? Bedroom.WALL.get() : Blocks.AIR).defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(new BlockPos(0, 64, 0), Blocks.BEDROCK.defaultBlockState(), 3);
    }
}
