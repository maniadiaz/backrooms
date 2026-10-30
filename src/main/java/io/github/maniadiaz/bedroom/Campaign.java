package io.github.maniadiaz.bedroom;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

final class Campaign {
    static final ResourceKey<Level> LEVEL_1=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID,"level_1"));
    static boolean dungeon(ResourceKey<Level> key){return key.equals(DevelopmentCommands.LEVEL_0)||key.equals(LEVEL_1);}
    static ServerLevel level(MinecraftServer server,int index){return server.getLevel(index==0?DevelopmentCommands.LEVEL_0:LEVEL_1);}
    static int ceiling(Level level){return level.dimension().equals(LEVEL_1)?72:68;}
    static int lampY(Level level){return level.dimension().equals(LEVEL_1)?70:68;}
    static boolean exit(net.minecraft.world.level.block.state.BlockState state){return state.is(Bedroom.EXIT.get())||state.is(Bedroom.LOADING_EXIT.get());}
    private Campaign(){}
}
