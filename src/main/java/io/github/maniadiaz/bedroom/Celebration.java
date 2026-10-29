package io.github.maniadiaz.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID)
final class Celebration {
    static final int ROOM=144;
    static void ensure(ServerLevel level){
        BlockPos marker=new BlockPos(ROOM,63,0);
        if(level.getBlockState(marker).is(Blocks.LODESTONE))return;
        for(int x=0;x<=20;x++)for(int z=0;z<=20;z++){
            level.setBlock(new BlockPos(ROOM+x,64,z),(x==0||x==20||z==0||z==20?Blocks.GOLD_BLOCK:Blocks.QUARTZ_BLOCK).defaultBlockState(),3);
            for(int y=65;y<=78;y++){
                boolean edge=x==0||x==20||z==0||z==20;
                Block block=y==78?(x%4==0&&z%4==0?Blocks.SEA_LANTERN:Blocks.BLUE_CONCRETE):edge?(y%4==0?Blocks.CYAN_CONCRETE:Blocks.WHITE_CONCRETE):Blocks.AIR;
                level.setBlock(new BlockPos(ROOM+x,y,z),block.defaultBlockState(),3);
            }
        }
        level.setBlock(MatchRooms.button(ROOM).below(),Blocks.LIME_CONCRETE.defaultBlockState(),3);
        level.setBlock(MatchRooms.button(ROOM),Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE,AttachFace.FLOOR),3);
        level.setBlock(marker,Blocks.LODESTONE.defaultBlockState(),3);
    }
    static void firework(ServerLevel level,int side){
        var stack=new ItemStack(Items.FIREWORK_ROCKET);var fireworks=stack.getOrCreateTagElement("Fireworks");fireworks.putByte("Flight",(byte)0);
        var explosion=new CompoundTag();explosion.putByte("Type",(byte)1);explosion.putIntArray("Colors",new int[]{0xF4CC48,0x66DDCC,0xF0F0DC});explosion.putBoolean("Trail",true);
        var list=new ListTag();list.add(explosion);fireworks.put("Explosions",list);
        level.addFreshEntity(new FireworkRocketEntity(level,ROOM+(side%2==0?7.5:13.5),66,10.5,stack));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var d=MatchData.get(event.getServer());if(d.phase==MatchData.Phase.VICTORY&&d.restTicks%60==0){
            var level=Campaign.level(event.getServer(),0);if(level!=null){ensure(level);firework(level,d.restTicks/60);}
        }
    }
    private Celebration(){}
}
