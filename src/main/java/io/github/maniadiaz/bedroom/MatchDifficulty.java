package io.github.maniadiaz.bedroom;

/** Persisted for the whole attempt; never changes underneath an active team. */
enum MatchDifficulty {
    PRINCIPIANTE("principiante",100,1.0,90,150),
    FACIL("facil",150,1.5,60,120),
    NORMAL("normal",250,2.0,45,90),
    DIFICIL("dificil",350,2.5,35,70),
    HARDCORE("hardcore",500,3.0,25,50);
    final String id;
    final int size, firstEncounterSeconds, encounterSeconds;
    final double sanityMultiplier;
    MatchDifficulty(String id,int size,double multiplier,int first,int interval){
        this.id=id;this.size=size;sanityMultiplier=multiplier;firstEncounterSeconds=first;encounterSeconds=interval;
    }
    static MatchDifficulty parse(String id){
        for(var d:values())if(d.id.equalsIgnoreCase(id))return d;
        return FACIL;
    }
}
