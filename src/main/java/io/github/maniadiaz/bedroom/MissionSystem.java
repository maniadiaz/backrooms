package io.github.maniadiaz.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class MissionSystem {
    static net.minecraft.world.item.Item item(MatchData d){return d.level==0?Bedroom.FUSE.get():Bedroom.RELAY.get();}
    static Block cacheBlock(MatchData d){return d.level==0?Bedroom.FUSE_CACHE.get():Bedroom.RELAY_CACHE.get();}
    static Block panelBlock(MatchData d){return d.level==0?Bedroom.FUSE_PANEL.get():Bedroom.RELAY_PANEL.get();}
    static String name(MatchData d){return d.level==0?"Fusible":"Relé de carga";}
    static int anchor(MatchData d) { return Math.floorDiv(MatchRooms.arena(d),128)*128; }
    static BlockPos cache(MatchData d,int i) {
        int n=MatchArena.size(d);
        int[] x={MatchArena.aisle(n/4),MatchArena.aisle(n*3/4),MatchArena.aisle(n/3)};
        int[] z={MatchArena.aisle(n/4),MatchArena.aisle(n/3),MatchArena.aisle(n*3/4)};
        return new BlockPos(anchor(d)+x[i],65,z[i]);
    }
    static BlockPos panel(MatchData d,int i) { return MatchArena.exit(d).offset(-3+i*3,0,-2); }
    static void prepare(net.minecraft.server.MinecraftServer server,MatchData d) {
        var level = Campaign.level(server,d.level); if (level==null) return;
        MatchArena.prepare(level,d);
        for (int i=0;i<3;i++) {
            level.getChunkAt(cache(d,i)); level.getChunkAt(panel(d,i));
            level.setBlock(cache(d,i),cacheBlock(d).defaultBlockState().setValue(Node.INDEX,i),3);
            level.setBlock(panel(d,i),panelBlock(d).defaultBlockState().setValue(Node.INDEX,i),3);
        }
    }
    static ItemStack fuse(MatchData d,int index,int lease) {
        ItemStack stack = new ItemStack(item(d)); var tag = stack.getOrCreateTag();
        tag.putInt("Round",d.round); tag.putInt("Level",d.level); tag.putInt("Index",index); tag.putInt("Lease",lease);
        stack.setHoverName(Component.literal(name(d)+" "+(index+1)+" / 3")); return stack;
    }
    static boolean valid(ItemStack stack,MatchData d,int index) {
        var tag = stack.getTag(); return stack.is(item(d)) && tag!=null && tag.contains("Index",Tag.TAG_INT)
                && tag.getInt("Round")==d.round && tag.getInt("Level")==d.level && tag.getInt("Index")==index
                && tag.getInt("Lease")>0 && tag.getInt("Lease")==d.mission.leases[index];
    }
    static boolean recoverable(ServerPlayer requester,MatchData d,int index) {
        for (var m:d.members.values()) {
            if (m.state==MatchData.State.LIMBO && m.limboTicks>0) {
                var list=m.inventory.getList("items",Tag.TAG_COMPOUND);
                for(int j=0;j<list.size();j++) if(valid(ItemStack.of(list.getCompound(j)),d,index)) return false;
            }
            if (m.state!=MatchData.State.ACTIVE) continue;
            var p=m.id.equals(requester.getUUID())?requester:requester.server.getPlayerList().getPlayer(m.id);
            if(p==null)continue;
            for(int j=0;j<p.getInventory().getContainerSize();j++) if(valid(p.getInventory().getItem(j),d,index))return false;
            if(valid(p.containerMenu.getCarried(),d,index))return false;
        }
        return true;
    }
    static boolean interact(ServerPlayer p,BlockPos pos,net.minecraft.world.InteractionHand hand) {
        if(!SurvivalSystem.active(p)||p.serverLevel()!=Campaign.level(p.server,MatchData.get(p.server).level)||p.distanceToSqr(pos.getCenter())>25)return false;
        var d=MatchData.get(p.server);
        for(int i=0;i<3;i++) {
            if(pos.equals(cache(d,i)) && p.level().getBlockState(pos).is(cacheBlock(d))) {
                if(d.mission.done(i)) { message(p,"Este componente ya está instalado."); return true; }
                if(!recoverable(p,d,i)) { message(p,"El equipo ya lleva este componente o puede recuperarlo del limbo."); return true; }
                int lease=d.mission.leases[i]+1;
                if(p.getInventory().add(fuse(d,i,lease))) {
                    d.mission.leases[i]=lease; d.setDirty();
                    p.level().setBlock(pos,p.level().getBlockState(pos).setValue(BlockStateProperties.POWERED,true),3);
                    message(p,name(d)+" "+(i+1)+" recuperado. Llévalo al panel del mismo número, en la sala de control. Usa /backrooms objective para orientarte.");
                } else message(p,"Necesitas un espacio libre en el inventario.");
                return true;
            }
            if(pos.equals(panel(d,i)) && p.level().getBlockState(pos).is(panelBlock(d))) {
                var stack=p.getItemInHand(hand);
                if(d.mission.done(i)) message(p,"Panel "+(i+1)+" ya reparado.");
                else if(!valid(stack,d,i)) message(p,"Este panel necesita el componente "+(i+1)+" de esta partida.");
                else if(d.mission.install(i,stack.getTag().getInt("Lease"))) {
                    stack.shrink(1); d.setDirty(); p.level().setBlock(pos,p.level().getBlockState(pos).setValue(BlockStateProperties.POWERED,true),3);
                    for(var m:d.members.values()) {
                        var other=m.id.equals(p.getUUID())?p:p.server.getPlayerList().getPlayer(m.id);
                        if(other!=null) { BedroomNetwork.sync(other); MatchAnnouncements.show(other,d.mission.ready()?"SALIDA HABILITADA":"ENERGÍA RESTAURADA",
                                d.mission.ready()?"Corre contra la pared anómala durante 2 segundos.":"Paneles reparados: "+d.mission.count()+" / 3",net.minecraft.ChatFormatting.GOLD); }
                    }
                }
                return true;
            }
        }
        return false;
    }
    private static void message(ServerPlayer p,String text) { p.displayClientMessage(Component.literal(text),true); }
    static final class Node extends Block {
        static final net.minecraft.world.level.block.state.properties.IntegerProperty INDEX=net.minecraft.world.level.block.state.properties.IntegerProperty.create("index",0,2);
        Node() { super(BlockBehaviour.Properties.of().strength(-1,3600000).sound(SoundType.METAL)); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED,false).setValue(INDEX,0)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(BlockStateProperties.POWERED,INDEX); }
    }
    private MissionSystem() { }
}
