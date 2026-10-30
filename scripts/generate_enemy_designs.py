"""Original procedural UV materials and Smiler face, matching the native Java meshes."""
from pathlib import Path
import math
import random
import struct
import zlib

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/bedroom/textures/entity'

def png(name,size,pixel):
    raw=bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):raw.extend(pixel(x,y))
    def chunk(kind,data):return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
    ROOT.mkdir(parents=True,exist_ok=True)
    (ROOT/name).write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',size,size,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))

def material_atlas(name,palette,seed):
    rng=random.Random(seed)
    def pixel(x,y):
        tile=(x//64)+(y//64)*2;base=palette[tile]
        grain=rng.randint(-7,7)
        stain=math.sin(x*.23+math.sin(y*.12)*2)*math.sin(y*.17)*12
        vein=abs(math.sin(x*.43+math.sin(y*.14)*3))<.1
        shade=grain+stain-(20 if vein else 0)
        if tile==1:shade+=8 if (x+y*2)%11<2 else 0
        if tile==2:shade=grain*.25+stain*.2
        return tuple(max(0,min(255,int(c+shade))) for c in base)+(255,)
    png(name,128,pixel)

def generate_hound():
    material_atlas('hound.png',[(153,143,128),(197,185,143),(13,15,17),(65,58,48)],80251)

def generate_bacteria():
    material_atlas('bacteria.png',[(88,78,45),(141,126,82),(24,25,19),(70,48,27)],88163)

def generate_smiler():
    rng=random.Random(11401)
    def face(x,y):
        # Transparent emissive layer: no dark rectangular backdrop around the face.
        for cx in (77,179):
            r=math.hypot(x-cx,y-83)
            if r<15:return (255,255,255,255)
            if r<21:return (220,228,232,int((21-r)/6*100))
        u=(x-128)/99
        if abs(u)<1:
            top=128+39*(1-u*u)
            bottom=top+37*(1-u*u)
            if top-4<=y<=bottom+4:
                if y<top or y>bottom:return (139+rng.randrange(55),8,9,245)
                margin=min(y-top,bottom-y)
                gap=(x+int(3*math.sin(x*.15)))%15<2
                upper=13*(1-u*u)+3*math.sin(x*.45)
                lower=12*(1-u*u)+2*math.cos(x*.35)
                if not gap and (y-top<upper or bottom-y<lower):
                    shade=int(25*min(1,margin/6))+rng.randrange(-9,9)
                    return (max(0,240-shade),max(0,224-shade),max(0,157-shade),255)
                return (31,2,5,255)
        return (0,0,0,0)
    png('smiler.png',256,face)
    def shell(x,y):
        value=4+int(3*(1+math.sin(x*.5)*math.cos(y*.37)))+rng.randrange(3)
        return (value,value,value+1,255)
    png('smiler_body.png',64,shell)

if __name__=='__main__':
    generate_hound();generate_bacteria();generate_smiler()
    print('Generated Hound/Bacteria 128px UV atlases and Smiler 256px face + black body.')
