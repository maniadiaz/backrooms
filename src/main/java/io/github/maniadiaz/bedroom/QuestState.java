package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

final class QuestState {
    int notes,sequence;
    boolean water,hidden,puzzle,equipped,console;
    int warning;
    UUID tutorialSmiler;
    final Map<UUID,Hide> hiding=new HashMap<>();
    static final class Hide { final BlockPos pos;int ticks;Hide(BlockPos pos){this.pos=pos.immutable();} }
    boolean note(int i){return (notes&(1<<i))!=0;}
    boolean tutorial(){return note(0)&&water&&hidden;}
    boolean ready(){return tutorial()&&puzzle&&equipped&&console&&note(4);}
    int stage(){return !tutorial()?0:!puzzle?1:!equipped?2:!console?3:4;}
    CompoundTag save(){var t=new CompoundTag();t.putInt("notes",notes);t.putInt("sequence",sequence);t.putBoolean("water",water);t.putBoolean("hidden",hidden);t.putBoolean("puzzle",puzzle);t.putBoolean("equipped",equipped);t.putBoolean("console",console);return t;}
    static QuestState load(CompoundTag t){var q=new QuestState();q.notes=t.getInt("notes")&31;q.sequence=Math.max(0,Math.min(3,t.getInt("sequence")));q.water=t.getBoolean("water");q.hidden=t.getBoolean("hidden");q.puzzle=t.getBoolean("puzzle");q.equipped=t.getBoolean("equipped");q.console=t.getBoolean("console");return q;}
}
