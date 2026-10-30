"""Checkpoint terminal, personal recovery door and positional heartbeat."""
from pathlib import Path
import json,struct,zlib,sys,math
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/bedroom'
def png(path,w,h,fn):
    raw=bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):raw.extend(fn(x,y))
    def chunk(k,d):return struct.pack('!I',len(d))+k+d+struct.pack('!I',zlib.crc32(k+d)&0xffffffff)
    p=ASSETS/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
font={'G':['111','100','101','101','111'],'U':['101','101','101','101','111'],'A':['010','101','111','101','101'],'R':['110','101','110','101','101'],'D':['110','101','101','101','110']}
def terminal(x,y):
    color=(51,65,58)
    if 1<x<30 and 5<y<25:color=(11,75,35)
    if 2<=x<30 and 13<=y<18:
        index=(x-2)//4;column=(x-2)%4
        if column<3 and font['GUARDAR'[index]][y-13][column]=='1':color=(192,255,178)
    if y in (3,28):color=(112,172,91)
    return color+(255,)
png(Path('textures/block/checkpoint_terminal.png'),32,32,terminal)
for path,value in [('blockstates/checkpoint_terminal.json',{'variants':{'':{'model':'bedroom:block/checkpoint_terminal'}}}),('models/block/checkpoint_terminal.json',{'parent':'minecraft:block/cube_all','textures':{'all':'bedroom:block/checkpoint_terminal'}})]:
    (ASSETS/path).write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def door(x,y):
    if x<2 or x>29 or y<2 or y>62:return (0,0,0,0)
    if x<5 or x>26 or y<6 or y>59:return (132,217,229,255)
    nx=(x-16)/7;ny=(29-y)/7
    if (nx*nx+ny*ny-1)**3-nx*nx*ny**3<0:return (224,47,58,255)
    if 23<x<26 and 40<y<43:return (230,236,218,255)
    return (8,13,20,225)
png(Path('textures/environment/recovery_door.png'),32,64,door)
sys.path.insert(0,str(ROOT/'.tools/audio-python'))
import numpy as np
import soundfile as sf
rate=32000;t=np.arange(int(.8*rate))/rate;wave=np.zeros_like(t)
for onset,frequency,amplitude in [(.015,65,.65),(.24,48,.48)]:
    u=np.maximum(0,t-onset);envelope=(1-np.exp(-u*190))*np.exp(-u*27)*(t>=onset)
    wave+=amplitude*(np.sin(2*np.pi*frequency*u)+.2*np.sin(2*np.pi*frequency*2*u))*envelope
sf.write(ASSETS/'sounds/ambient/recovery_heartbeat.ogg',wave,rate,format='OGG',subtype='VORBIS')
decoded,decoded_rate=sf.read(ASSETS/'sounds/ambient/recovery_heartbeat.ogg');assert decoded.ndim==1 and decoded_rate==32000 and max(abs(decoded))<1
p=ASSETS/'sounds.json';sounds=json.loads(p.read_text(encoding='utf-8'));sounds['recovery_heartbeat']={'subtitle':'subtitles.bedroom.recovery_heartbeat','sounds':[{'name':'bedroom:ambient/recovery_heartbeat','attenuation_distance':16}]};p.write_text(json.dumps(sounds,indent=2)+'\n',encoding='utf-8')
for language in ['es_mx','en_us']:
    p=ASSETS/f'lang/{language}.json';labels=json.loads(p.read_text(encoding='utf-8'));labels['block.bedroom.checkpoint_terminal']='Guardar checkpoint';labels['subtitles.bedroom.recovery_heartbeat']='Latido hacia la puerta';p.write_text(json.dumps(labels,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Checkpoint/recovery artwork and validated mono heartbeat generated.')
