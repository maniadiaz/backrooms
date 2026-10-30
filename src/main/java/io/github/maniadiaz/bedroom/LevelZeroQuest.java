package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class LevelZeroQuest {
    static final String[] LORE={
        "Si escuchas que el zumbido baja de tono, busca un casillero metálico. Métete dentro y mantén la respiración. El Smiler pasará de largo.",
        "Cuatro pilares: agua estancada, papel viejo, sangre seca y moho. Pulsa Azul, Amarillo, Rojo y Verde. Un error hará demasiado ruido.",
        "El aire quema en el sector confinado. Busca la llave inglesa en almacenamiento y abre la taquilla amarilla del sector este. Equipa casco, traje y botas antes de entrar.",
        "Atravesé la niebla verde con el traje puesto. La palanca de la consola central desbloquea la puerta blindada. Hay un Sabueso patrullando: no corras a ciegas.",
        "Dos rutas: la puerta blindada lleva a Zona de Carga; la pared inestable lleva a Darkrooms. Para atravesar la pared mantén agacharse (Shift) y avance durante dos segundos."
    };
    static BlockPos point(MatchData d,int x,int z){return new BlockPos(MissionSystem.anchor(d)+MatchArena.aisle(MatchArena.size(d)*x/100),65,MatchArena.aisle(MatchArena.size(d)*z/100));}
    static BlockPos puzzle(MatchData d){return point(d,35,35);}
    static BlockPos storage(MatchData d){return point(d,60,25);}
    static BlockPos locker(MatchData d){return point(d,15,30);}
    static BlockPos security(MatchData d){return point(d,85,45);}
    static BlockPos radiation(MatchData d){return point(d,65,75);}
    static BlockPos door(MatchData d){return point(d,85,85);}
    static BlockPos gate(MatchData d){return storage(d).offset(0,0,3);}
    static BlockPos note(MatchData d,int i){return switch(i){case 0->point(d,15,15);case 1->puzzle(d).offset(-3,0,-3);case 2->storage(d).offset(2,0,-2);case 3->radiation(d).offset(-3,0,-3);default->door(d).offset(-3,0,-2);};}
    static BlockPos color(MatchData d,int i){return puzzle(d).offset(i%2==0?-2:2,0,i<2?-2:2);}
    static boolean radiation(MatchData d,BlockPos p){var c=radiation(d);return Math.abs(p.getX()-c.getX())<=5&&Math.abs(p.getZ()-c.getZ())<=5;}
    static boolean protectedBySuit(ServerPlayer p){var d=MatchData.get(p.server);return valid(p.getItemBySlot(EquipmentSlot.HEAD),QuestContent.HELMET.get(),d)&&valid(p.getItemBySlot(EquipmentSlot.CHEST),QuestContent.SUIT.get(),d)&&valid(p.getItemBySlot(EquipmentSlot.FEET),QuestContent.BOOTS.get(),d);}
    static boolean hidden(ServerPlayer p){return MatchData.get(p.server).quest.hiding.containsKey(p.getUUID());}
    static boolean targetable(ServerPlayer p){return SurvivalSystem.active(p)&&!hidden(p);}
    static boolean valid(ItemStack s,Item item,MatchData d){return s.is(item)&&s.hasTag()&&s.getTag().getInt("QuestRound")==d.round;}
    static ItemStack item(Item item,MatchData d){var s=new ItemStack(item);s.getOrCreateTag().putInt("QuestRound",d.round);return s;}
    static boolean has(ServerPlayer p,Item item,MatchData d){for(int i=0;i<p.getInventory().getContainerSize();i++)if(valid(p.getInventory().getItem(i),item,d))return true;return false;}
    static boolean give(ServerPlayer p,Item item,MatchData d){return has(p,item,d)||p.getInventory().add(item(item,d));}
    static void say(ServerPlayer p,String s){p.sendSystemMessage(Component.literal(s));}
    static void announce(ServerPlayer p,String title,String subtitle){var d=MatchData.get(p.server);for(var m:d.members.values()){var other=m.id.equals(p.getUUID())?p:p.server.getPlayerList().getPlayer(m.id);if(other!=null)MatchAnnouncements.show(other,title,subtitle,net.minecraft.ChatFormatting.GOLD);}}
    static void room(ServerLevel level,BlockPos center,int radius){for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=0;y<3;y++)level.setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
    static void place(ServerLevel l,BlockPos p,Block b){l.setBlock(p,b.defaultBlockState(),3);}
    static void prepare(ServerLevel l,MatchData d){
        for(var center:List.of(puzzle(d),storage(d),security(d),radiation(d),door(d),locker(d)))room(l,center,4);
        var s=storage(d);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(Math.abs(x)==3||Math.abs(z)==3)for(int y=0;y<3;y++)place(l,s.offset(x,y,z),Bedroom.WALL.get());
        for(int y=0;y<3;y++)place(l,gate(d).above(y),d.quest.puzzle?Blocks.AIR:QuestContent.GATE.get());
        place(l,s.offset(-1,0,0),QuestContent.TOOLBOX.get());place(l,security(d),QuestContent.SECURITY.get());place(l,security(d).above(),QuestContent.SECURITY.get());
        place(l,locker(d),QuestContent.LOCKER.get());place(l,locker(d).above(),QuestContent.LOCKER.get());
        Block[] colors={QuestContent.BLUE.get(),QuestContent.YELLOW.get(),QuestContent.RED.get(),QuestContent.GREEN.get()};
        for(int i=0;i<4;i++){place(l,color(d,i),colors[i]);place(l,color(d,i).above(),Blocks.IRON_BLOCK);}
        for(int i=0;i<5;i++)place(l,note(d,i),QuestContent.NOTE.get());
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)place(l,radiation(d).offset(x,-1,z),Blocks.GREEN_TERRACOTTA);
        place(l,radiation(d),QuestContent.CONSOLE.get());
        for(int x=-4;x<=4;x++)for(int y=0;y<3;y++)place(l,door(d).offset(x,y,0),Math.abs(x)<=1?QuestContent.DOOR.get():Bedroom.WALL.get());
        // Restore the Glitch wall if a compact map's radiation room overlaps its alcove.
        for(int y=0;y<3;y++)place(l,MatchArena.exit(d).above(y),Bedroom.EXIT.get());
        place(l,puzzle(d).above(3),Bedroom.LIGHT.get());place(l,security(d).above(3),Bedroom.LIGHT.get());
    }
    static boolean interact(ServerPlayer p,BlockPos pos,net.minecraft.world.InteractionHand hand){
        var d=MatchData.get(p.server);if(d.level!=0||!SurvivalSystem.active(p)||p.level()!=Campaign.level(p.server,0)||p.distanceToSqr(pos.getCenter())>25||hidden(p))return false;
        var q=d.quest;
        for(int i=0;i<5;i++)if(pos.equals(note(d,i))&&p.level().getBlockState(pos).is(QuestContent.NOTE.get())){
            if(i>0&&q.stage()<i){say(p,"Completa primero la misión anterior.");return true;}
            q.notes|=1<<i;say(p,"NOTA #"+(i+1)+" — "+LORE[i]);d.setDirty();return true;
        }
        if(pos.equals(locker(d))||pos.equals(locker(d).above())){
            if(!q.note(0)||!q.water){say(p,"Lee la Nota #1 y bebe Agua de Almendras con cordura superior al 70%.");return true;}
            if(q.hiding.values().stream().anyMatch(h->h.pos.equals(locker(d)))){say(p,"Ese casillero está ocupado.");return true;}
            q.hiding.put(p.getUUID(),new QuestState.Hide(locker(d)));p.teleportTo(p.serverLevel(),locker(d).getX()+.5,65,locker(d).getZ()+.5,p.getYRot(),0);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,220,0,false,false));
            say(p,"ESCONDIDO — mantente dentro durante 10 segundos. Moverte fuera cancela el escondite.");return true;
        }
        for(int i=0;i<4;i++)if(pos.equals(color(d,i))){
            if(!q.tutorial()||!q.note(1)){say(p,"Encuentra y lee la Nota #2 después de superar el escondite.");return true;}
            if(q.puzzle)return true;
            if(i==q.sequence){if(++q.sequence==4){q.puzzle=true;q.sequence=0;for(int y=0;y<3;y++)place(p.serverLevel(),gate(d).above(y),Blocks.AIR);announce(p,"PANEL SECRETO ABIERTO","Almacenamiento accesible. Busca la caja de herramientas.");}}
            else{q.sequence=0;p.serverLevel().playSound(null,pos,BedroomSounds.BACTERIA_ALERT.get(),net.minecraft.sounds.SoundSource.HOSTILE,1.4F,.8F);spawn(p,2);say(p,"SECUENCIA INCORRECTA — el ruido ha atraído a Bacteria.");}
            d.setDirty();return true;
        }
        if(pos.equals(storage(d).offset(-1,0,0))){if(q.puzzle){give(p,QuestContent.WRENCH.get(),d);say(p,"Llave inglesa: abre la taquilla amarilla del este.");}else say(p,"El mantenimiento está cerrado.");return true;}
        if(pos.equals(security(d))||pos.equals(security(d).above())){
            if(!q.puzzle||!q.note(2)||!valid(p.getItemInHand(hand),QuestContent.WRENCH.get(),d)){say(p,"Lee la Nota #3 y usa la llave inglesa de esta partida.");return true;}
            boolean a=give(p,QuestContent.HELMET.get(),d),b=give(p,QuestContent.SUIT.get(),d),c=give(p,QuestContent.BOOTS.get(),d);
            say(p,a&&b&&c?"Traje recuperado. Equipa casco, traje y botas; cada compañero necesita su propio equipo.":"Haz espacio y vuelve a abrir la taquilla para recoger las piezas que faltan.");return true;
        }
        if(pos.equals(radiation(d))){
            if(!q.equipped||!q.note(3)||!protectedBySuit(p)){say(p,"Necesitas leer la Nota #4 y llevar las tres piezas Hazmat equipadas.");return true;}
            q.console=true;give(p,QuestContent.KEY.get(),d);d.setDirty();announce(p,"PUERTA BLINDADA DESBLOQUEADA","Recoge la Nota #5 y elige tu ruta.");return true;
        }
        if(Math.abs(pos.getX()-door(d).getX())<=1&&pos.getZ()==door(d).getZ()&&p.level().getBlockState(pos).is(QuestContent.DOOR.get())){
            if(!q.ready()||!valid(p.getItemInHand(hand),QuestContent.KEY.get(),d)){say(p,"Completa las cinco notas y usa la llave de la consola maestra.");return true;}
            if(d.exitDelay==0){d.nextDestination=1;d.exitDelay=20;for(var m:d.members.values()){var other=m.id.equals(p.getUUID())?p:p.server.getPlayerList().getPlayer(m.id);if(other!=null)BedroomNetwork.blackout(other);}}return true;
        }
        return false;
    }
    static void drank(ServerPlayer p){var d=MatchData.get(p.server);if(d.level==0&&!d.quest.water&&d.quest.note(0)&&d.members.get(p.getUUID()).survival.sanity>70){d.quest.water=true;AtmosphereSystem.tryFault(p);AtmosphereSystem.personal(p,BedroomSounds.BUZZ_CLICK.get(),p.position(),1.2F,.65F);var smiler=spawn(p,1);if(smiler!=null)d.quest.tutorialSmiler=smiler.getUUID();announce(p,"EL ZUMBIDO HA CAMBIADO","Busca el casillero y escóndete durante 10 segundos.");d.setDirty();}}
    static void tick(ServerPlayer p){
        var d=MatchData.get(p.server);if(d.level!=0||!SurvivalSystem.active(p)||p.level()!=Campaign.level(p.server,0))return;
        var q=d.quest;var hide=q.hiding.get(p.getUUID());
        if(hide!=null){
            if(p.distanceToSqr(hide.pos.getCenter())>2.8){q.hiding.remove(p.getUUID());p.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);}
            else if(++hide.ticks>=200){q.hiding.remove(p.getUUID());q.hidden=true;p.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);if(q.tutorialSmiler!=null){var e=p.serverLevel().getEntity(q.tutorialSmiler);if(e!=null)e.discard();q.tutorialSmiler=null;}p.teleportTo(p.serverLevel(),hide.pos.getX()+.5,65,hide.pos.getZ()+2.5,p.getYRot(),0);announce(p,"PRIMER ESCONDITE SUPERADO","Cordura y mapa mental del sector disponibles.");}
        }
        if(!q.equipped&&q.puzzle&&q.note(2)&&protectedBySuit(p)){q.equipped=true;announce(p,"PROTECCIÓN COMPLETA","Busca la consola de la zona radiactiva.");}
        environment(p,d,p.server.getTickCount());
        d.setDirty();
    }
    static void environment(ServerPlayer p,MatchData d,int now){
        if(radiation(d,p.blockPosition())){
            if(now%20==0){AtmosphereSystem.personal(p,BedroomSounds.GEIGER.get(),p.position(),.75F,protectedBySuit(p)?1:1.3F);if(!protectedBySuit(p))p.hurt(p.damageSources().magic(),3);}
            if(now%10==0)p.serverLevel().sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.25F,.8F,.1F),.8F),p.getX(),p.getY()+1,p.getZ(),5,3,.7,3,0);
            if(now%100==0)spawn(p,0);
        }
    }
    static Monster spawn(ServerPlayer p,int kind){
        var d=MatchData.get(p.server);var l=p.serverLevel();
        var existing=l.getEntitiesOfClass(Monster.class,p.getBoundingBox().inflate(MatchArena.size(d)),e->kind==0?e instanceof HoundEntity:kind==1?e instanceof SmilerEntity:e instanceof BacteriaEntity);
        if(!existing.isEmpty()){var e=existing.get(0);if(e instanceof BacteriaEntity b)b.investigate(p);return e;}
        for(int i=0;i<128;i++){
            double angle=p.getRandom().nextDouble()*Math.PI*2;var pos=BlockPos.containing(p.getX()+Math.cos(angle)*(8+i%12),65,p.getZ()+Math.sin(angle)*(8+i%12));
            if(!MatchArena.contains(d,pos)||!l.hasChunkAt(pos))continue;
            Monster e=kind==0?HoundEntity.TYPE.get().create(l):kind==1?SmilerEntity.TYPE.get().create(l):BacteriaEntity.TYPE.get().create(l);if(e==null)return null;
            e.moveTo(pos.getX()+.5,65,pos.getZ()+.5,0,0);e.setOnGround(true);
            var path=e.getNavigation().createPath(p,0);
            if(!l.getBlockState(pos.below()).isSolid()||!l.noCollision(e)||path==null||!path.canReach()){e.discard();continue;}
            if(e instanceof HoundEntity h){h.round=d.round;h.matchLevel=d.level;h.investigate(p);var speed=h.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);if(speed!=null)speed.setBaseValue(speed.getBaseValue()*1.15);}
            if(e instanceof SmilerEntity s){s.round=d.round;s.matchLevel=d.level;s.beginEncounter(p);}
            if(e instanceof BacteriaEntity b){b.round=d.round;b.matchLevel=d.level;b.investigate(p);}
            if(l.addFreshEntity(e))return e;e.discard();
        }return null;
    }
    static BlockPos target(MatchData d){var q=d.quest;if(!q.note(0))return note(d,0);if(!q.tutorial())return locker(d);if(!q.note(1))return note(d,1);if(!q.puzzle)return puzzle(d);if(!q.note(2))return storage(d);if(!q.equipped)return security(d);if(!q.note(3))return note(d,3);if(!q.console)return radiation(d);if(!q.note(4))return note(d,4);return door(d);}
    static BlockPos target(ServerPlayer p,MatchData d){
        if(d.quest.puzzle&&d.quest.note(2)&&!protectedBySuit(p))return has(p,QuestContent.WRENCH.get(),d)?security(d):storage(d).offset(-1,0,0);
        return target(d);
    }
    static String objective(MatchData d){var q=d.quest;return switch(q.stage()){case 0->!q.note(0)?"Busca la Nota #1":!q.water?"Bebe Agua: cordura >70%":"Escóndete 10 segundos";case 1->"Nota #2: Azul-Amarillo-Rojo-Verde";case 2->"Llave inglesa y traje Hazmat";case 3->"Nota #4 y consola radiactiva";default->q.ready()?"Elige puerta o pared Glitch":"Lee la Nota #5";};}
    static void journal(ServerPlayer p){var d=MatchData.get(p.server);say(p,"MISIÓN "+(d.quest.stage()+1)+"/5 — "+objective(d));for(int i=0;i<5;i++)if(d.quest.note(i))say(p,"NOTA #"+(i+1)+" — "+LORE[i]);}
    private LevelZeroQuest(){}
}
