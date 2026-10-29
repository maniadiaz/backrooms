"""Numbered fuse stations, panels, and fuse icon; Python standard library only."""
from pathlib import Path
import json
import struct
import zlib
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/bedroom'
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def png(path,size,pixel):
    raw=bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):raw.extend(pixel(x,y))
    def chunk(kind,data):return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',size,size,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
digits=[['010','110','010','010','111'],['111','001','111','100','111'],['111','001','111','001','111']]
for kind in ['fuse_cache','fuse_panel']:
    variants={}
    for i in range(3):
        for powered in [False,True]:
            name=f'{kind}_{i}_{int(powered)}'
            def pixel(x,y):
                base=(102,105,95) if kind=='fuse_panel' else (126,105,63)
                color=tuple(max(0,c-30) for c in base) if x in (0,15) or y in (0,15) else base
                if y==2 and 3<=x<=12:color=(84,193,114) if powered else ((169,71,54) if kind=='fuse_panel' else (224,183,72))
                dx,dy=(x-5)//2,(y-4)//2
                if 0<=dx<3 and 0<=dy<5 and digits[i][dy][dx]=='1':color=(231,222,178)
                return (*color,255)
            png(ROOT/f'textures/block/{name}.png',16,pixel)
            write(ROOT/f'models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'bedroom:block/{name}'}})
            variants[f'index={i},powered={str(powered).lower()}']={'model':f'bedroom:block/{name}'}
    write(ROOT/f'blockstates/{kind}.json',{'variants':variants})
def icon(x,y):
    if 5<=x<=10 and 2<=y<=13:
        return (181,176,145,255) if y<=4 or y>=11 else (108,178,183,255)
    if 7<=x<=8 and y in (1,14):return (210,200,150,255)
    return (0,0,0,0)
png(ROOT/'textures/item/mission_fuse.png',16,icon)
write(ROOT/'models/item/mission_fuse.json',{'parent':'minecraft:item/generated','textures':{'layer0':'bedroom:item/mission_fuse'}})
for lang in ['es_mx','en_us']:
    path=ROOT/f'lang/{lang}.json';values=json.loads(path.read_text(encoding='utf-8'))
    values.update({'block.bedroom.fuse_cache':'Estuche de fusible' if lang=='es_mx' else 'Fuse case',
                   'block.bedroom.fuse_panel':'Panel eléctrico' if lang=='es_mx' else 'Electrical panel',
                   'item.bedroom.mission_fuse':'Fusible de misión' if lang=='es_mx' else 'Mission fuse'})
    write(path,values)
print('Generated twelve numbered node textures/models and fuse icon.')
