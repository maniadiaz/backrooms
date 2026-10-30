package io.github.maniadiaz.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** A finite play space over the procedural background, with an actual solid perimeter. */
final class MatchArena {
    static int size(MatchData d){return d.difficulty.size;}
    static int origin(MatchData d){return MissionSystem.anchor(d);}
    static boolean contains(MatchData d,BlockPos p){
        int x=p.getX()-origin(d),z=p.getZ();
        return x>0&&x<size(d)-1&&z>0&&z<size(d)-1;
    }
    static int aisle(int coordinate){return Math.floorDiv(coordinate,16)*16+7;}
    static BlockPos exit(MatchData d){return new BlockPos(origin(d)+aisle(size(d)-25),65,aisle(size(d)-25)+3);}
    static void prepare(ServerLevel level,MatchData d){
        int n=size(d),x=origin(d),top=Campaign.ceiling(level);
        var wall=(d.level==0?Bedroom.WALL.get():net.minecraft.world.level.block.Blocks.IRON_BLOCK).defaultBlockState();
        for(int i=0;i<n;i++)for(int y=65;y<top;y++){
            level.setBlock(new BlockPos(x+i,y,0),wall,2);
            level.setBlock(new BlockPos(x+i,y,n-1),wall,2);
            level.setBlock(new BlockPos(x,y,i),wall,2);
            level.setBlock(new BlockPos(x+n-1,y,i),wall,2);
        }
        BlockPos exit=exit(d);
        // Control alcove opens onto the globally connected cross corridors.
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int y=65;y<top;y++)
            level.setBlock(new BlockPos(exit.getX()+dx,y,exit.getZ()+dz),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        for(int y=65;y<=67;y++)level.setBlock(new BlockPos(exit.getX(),y,exit.getZ()),
                (d.level==1?Bedroom.LOADING_EXIT.get():Bedroom.EXIT.get()).defaultBlockState(),3);
        level.setBlock(new BlockPos(exit.getX(),Campaign.lampY(level),exit.getZ()),net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState(),3);
    }
    static void contain(ServerPlayer p,MatchData d){
        if(!contains(d,p.blockPosition())){
            double x=Math.max(origin(d)+1.5,Math.min(origin(d)+size(d)-2.5,p.getX()));
            double z=Math.max(1.5,Math.min(size(d)-2.5,p.getZ()));
            p.teleportTo(p.serverLevel(),x,65,z,p.getYRot(),p.getXRot());
        }
    }
    static String direction(ServerPlayer p,BlockPos target){
        double dx=target.getX()+.5-p.getX(),dz=target.getZ()+.5-p.getZ();
        double relative=Math.atan2(-dx,dz)*180/Math.PI-p.getYRot();
        relative=net.minecraft.util.Mth.wrapDegrees((float)relative);
        String heading=Math.abs(relative)<35?"DELANTE":Math.abs(relative)>145?"DETRÁS":relative>0?"DERECHA":"IZQUIERDA";
        return heading+" · "+Math.round(Math.sqrt(dx*dx+dz*dz))+" m";
    }
    private MatchArena(){}
}
