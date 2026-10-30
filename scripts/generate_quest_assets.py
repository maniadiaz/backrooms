"""Native pixel materials and models for the five Level 0 missions."""
from pathlib import Path
import json,random,struct,zlib,sys
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
ASSETS=ROOT/'assets/bedroom'
def write(path,value):
    p=ASSETS/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def png(path,w,h,fn):
    raw=bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):raw.extend(fn(x,y))
    def chunk(k,d):return struct.pack('!I',len(d))+k+d+struct.pack('!I',zlib.crc32(k+d)&0xffffffff)
    p=ASSETS/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
palette={'hiding_locker':(91,100,99),'quest_toolbox':(134,42,31),'hazmat_locker':(203,166,32),'master_console':(51,67,61),'armored_exit':(77,83,86),'maintenance_gate':(100,94,74),'puzzle_blue':(36,89,185),'puzzle_yellow':(210,183,39),'puzzle_red':(166,42,31),'puzzle_green':(43,126,60),'lore_note':(204,192,156)}
rng=random.Random(8814)
digits={'1':['010','110','010','010','111'],'2':['111','001','111','100','111'],'3':['111','001','111','001','111'],'4':['101','101','111','001','001']}
numbers={'puzzle_blue':'2','puzzle_yellow':'3','puzzle_red':'1','puzzle_green':'4'}
for name,base in palette.items():
    def pixel(x,y):
        noise=rng.randint(-8,8);color=tuple(max(0,min(255,c+noise)) for c in base)
        if name=='lore_note' and y in (6,9,12,15,18,21,24) and 6<x<25:color=(65,58,44)
        if name!='lore_note' and (x in (1,30) or y in (1,30)):color=(30,31,28)
        if 'locker' in name and (y in (6,8,10) and 5<x<26 or 22<x<26 and 15<y<21):color=(22,23,21)
        if name.startswith('puzzle_') and 8<x<24 and 8<y<24:color=tuple(min(255,c+45) for c in base)
        if name in numbers and 13<=x<19 and 11<=y<21 and digits[numbers[name]][(y-11)//2][(x-13)//2]=='1':color=(242,238,209)
        if name in ('hazmat_locker','maintenance_gate','armored_exit') and y>25 and (x+y)%12<6:color=(28,28,22)
        if name=='master_console' and 5<x<26 and 5<y<20:color=(28,154,70) if (x+y)%6<2 else (9,25,14)
        return color+(255,)
    png(Path(f'textures/block/{name}.png'),32,32,pixel)
    write(Path(f'blockstates/{name}.json'),{'variants':{'':{'model':f'bedroom:block/{name}'}}})
    model={'parent':'minecraft:block/cube_all','textures':{'all':f'bedroom:block/{name}'}}
    if name=='lore_note':model={'textures':{'paper':'bedroom:block/lore_note','particle':'bedroom:block/lore_note'},'elements':[{'from':[2,0,2],'to':[14,1,14],'faces':{s:{'texture':'#paper'} for s in ['up','down','north','south','east','west']}}]}
    write(Path(f'models/block/{name}.json'),model)
for name in ['wrench','armored_key','hazmat_helmet','hazmat_suit','hazmat_boots']:
    def icon(x,y):
        if name=='wrench':on=abs(x-y)<3 or 3<x<13 and 3<y<11
        elif name=='armored_key':on=(x-9)**2+(y-9)**2<25 or abs(x-y)<2 and x>8 or 20<x<25 and 21<y<28
        elif name=='hazmat_helmet':on=5<x<26 and 4<y<26
        elif name=='hazmat_suit':on=7<x<24 and 5<y<27 or 2<x<29 and 7<y<14
        else:on=5<x<13 and 5<y<24 or 18<x<26 and 5<y<24 or 5<x<28 and 22<y<27
        if not on:return (0,0,0,0)
        color=(180,183,174) if name=='wrench' else (187,160,63) if name=='armored_key' else (212,173,27)
        if name=='hazmat_helmet' and 8<x<23 and 9<y<17:color=(19,26,27)
        return color+(255,)
    png(Path(f'textures/item/{name}.png'),32,32,icon)
    write(Path(f'models/item/{name}.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'bedroom:item/{name}'}})
png(Path('textures/models/armor/hazmat_layer_1.png'),64,32,lambda x,y:(21,28,29,255) if 9<x<15 and 9<y<14 or y>26 else (205+(x+y)%12,169+(x*y)%9,28,255))
for language in ['es_mx','en_us']:
    p=ASSETS/f'lang/{language}.json';labels=json.loads(p.read_text(encoding='utf-8'))
    labels.update({f'block.bedroom.{n}':label for n,label in zip(palette,['Casillero de escondite','Caja de herramientas','Taquilla Hazmat','Consola maestra','Puerta blindada','Compuerta de mantenimiento','Pilar Azul','Pilar Amarillo','Pilar Rojo','Pilar Verde','Nota de explorador'])})
    labels.update({f'item.bedroom.{n}':label for n,label in zip(['wrench','armored_key','hazmat_helmet','hazmat_suit','hazmat_boots'],['Llave inglesa','Llave de puerta blindada','Casco Hazmat','Traje Hazmat','Botas Hazmat'])})
    labels['subtitles.bedroom.geiger']='Contador Geiger';p.write_text(json.dumps(labels,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
sys.path.insert(0,str(ROOT.parents[2]/'.tools/audio-python'))
import numpy as np
import soundfile as sf
rate=32000;wave=np.zeros(rate,dtype=np.float32);nrng=np.random.default_rng(577)
for onset in [.02,.1,.19,.31,.36,.55,.61,.77,.88]:
    start=int(onset*rate);n=int(.012*rate);wave[start:start+n]=nrng.normal(0,.18,n)*np.exp(-np.arange(n)/50)
sf.write(ASSETS/'sounds/ambient/geiger.ogg',wave,rate,format='OGG',subtype='VORBIS')
p=ASSETS/'sounds.json';sounds=json.loads(p.read_text(encoding='utf-8'));sounds['geiger']={'subtitle':'subtitles.bedroom.geiger','sounds':[{'name':'bedroom:ambient/geiger','attenuation_distance':16}]};p.write_text(json.dumps(sounds,indent=2)+'\n',encoding='utf-8')
print('Quest blocks, supplies, Hazmat texture and mono Geiger generated.')
