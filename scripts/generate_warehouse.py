"""Original warehouse blocks; standard library only."""
from pathlib import Path
import json,random,struct,zlib
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/bedroom'
def write(path,data):path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def chunk(kind,data):return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
for name,base in [('concrete',(115,119,113)),('wet_concrete',(72,80,78)),('corrugated_steel',(91,105,107)),('loading_crate',(117,85,49)),('loading_exit',(37,69,69))]:
    rng=random.Random(name);raw=bytearray()
    for y in range(32):
        raw.append(0)
        for x in range(32):
            shade=rng.randint(-9,9)
            if name=='corrugated_steel':shade+=(-24 if x%8<2 else 5)
            if name=='loading_crate':shade+=(-30 if x in (1,2,29,30) or y in (1,2,29,30) or abs(x-y)<2 else 0)
            if name=='loading_exit':shade+=(45 if x in (2,3,28,29) else -4)
            raw.extend((*[max(0,min(255,c+shade)) for c in base],255))
    (ROOT/f'textures/block/{name}.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',32,32,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
    write(ROOT/f'models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'bedroom:block/{name}'}})
    write(ROOT/f'blockstates/{name}.json',{'variants':{'':{'model':f'bedroom:block/{name}'}}})
    if name!='loading_exit':write(ROOT/f'models/item/{name}.json',{'parent':f'bedroom:block/{name}'})
for lang in ('es_mx','en_us'):
    p=ROOT/f'lang/{lang}.json';values=json.loads(p.read_text(encoding='utf-8'))
    labels={'concrete':('Hormigón envejecido','Aged concrete'),'wet_concrete':('Hormigón húmedo','Damp concrete'),'corrugated_steel':('Acero corrugado','Corrugated steel'),'loading_crate':('Caja de carga','Loading crate'),'loading_exit':('Compuerta anómala','Anomalous loading gate'),'relay_cache':('Estuche de relé','Relay case'),'relay_panel':('Control de carga','Loading control')}
    for key,pair in labels.items():values[f'block.bedroom.{key}']=pair[0 if lang=='es_mx' else 1]
    values['item.bedroom.loading_relay']='Relé de carga' if lang=='es_mx' else 'Loading relay'
    write(p,values)
print('Warehouse palette and relay labels generated.')
