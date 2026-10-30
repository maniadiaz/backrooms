"""Deterministic original Bacteria skin and four synthesized positional sounds."""
from pathlib import Path
import json
import random
import struct
import sys
import zlib
PROJECT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(PROJECT/'.tools/audio-python'))
import numpy as np
import soundfile as sf
ROOT = PROJECT/'src/main/resources/assets/bedroom'
rng = random.Random(9821)
raw = bytearray()
for y in range(64):
    raw.append(0)
    for x in range(64):
        shade = rng.randint(-16,16)
        color = (91+shade,55+shade,49+shade,255)
        if y < 16 and x >= 40:
            color = (49+shade,51+shade,44+shade,255) if x >= 54 else (134+shade,108+shade,85+shade,255)
            if (x+y)%5 == 0: color = (105,66,37,255)
        elif 20 <= y < 34: color = (156+shade,143+shade,112+shade,255)
        elif (x*3+y)%11 == 0: color = (137,72,61,255)
        raw.extend(color)
def chunk(kind,data): return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
file = ROOT/'textures/entity/bacteria.png'
file.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',64,64,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
RATE = 32000
nrng = np.random.default_rng(51093)
def noise(n,low,high):
    freq = np.fft.rfftfreq(n,1/RATE)
    spectrum = np.fft.rfft(nrng.normal(0,1,n))*((freq>=low)&(freq<=high))
    values = np.fft.irfft(spectrum,n)
    return values/max(np.std(values),1e-9)
events = json.loads((ROOT/'sounds.json').read_text())
labels = {'drag':('Arrastre húmedo','Wet dragging'),'metal':('Cables rozando','Scraping wires'),
          'alert':('Grito de Bacteria','Bacteria shriek'),'attack':('Golpe de Bacteria','Bacteria strike')}
report = {}
for kind, seconds in [('drag',.65),('metal',1.25),('alert',.85),('attack',.55)]:
    t = np.arange(round(seconds*RATE))/RATE
    envelope = np.sin(np.pi*t/seconds)**2
    if kind == 'drag': wave = (noise(len(t),80,1800)*.8 + noise(len(t),2200,6000)*.15)*envelope*(.5+.5*np.sin(t*23)**2)
    elif kind == 'metal':
        wave = noise(len(t),2500,7000)*.1*envelope
        for freq in (470,733,1190,1810): wave += np.sin(2*np.pi*freq*t)*np.exp(-t*7)*.15
        wave *= np.minimum(1,t*100)
    elif kind == 'alert':
        phase = 2*np.pi*(110*t+50*t*t+6*np.sin(2*np.pi*5*t))
        wave = (np.sin(phase)*.4+np.sin(phase*2)*.18+noise(len(t),400,4500)*.35)*envelope
        wave += noise(len(t),1000,10000)*np.exp(-t*40)*np.minimum(1,t*250)*.25
    else: wave = (noise(len(t),100,3800)+.3*np.sin(2*np.pi*85*t))*envelope*np.exp(-t*4)
    wave = np.tanh(wave * 1.3)
    wave *= .72/max(np.max(np.abs(wave)),1e-9)
    file = ROOT/f'sounds/ambient/bacteria_{kind}.ogg'
    sf.write(file,wave,RATE,format='OGG',subtype='VORBIS')
    decoded,rate = sf.read(file)
    assert decoded.ndim == 1 and rate == RATE and np.isfinite(decoded).all()
    report[kind] = {'seconds':len(decoded)/rate,'channels':1,'peak':float(np.max(np.abs(decoded)))}
    events[f'bacteria_{kind}'] = {'subtitle':f'subtitles.bedroom.bacteria_{kind}','sounds':[{'name':f'bedroom:ambient/bacteria_{kind}','attenuation_distance':24}]}
(ROOT/'sounds.json').write_text(json.dumps(events,indent=2)+'\n',encoding='utf-8')
for language,index in [('es_mx',0),('en_us',1)]:
    file = ROOT/f'lang/{language}.json'
    values = json.loads(file.read_text(encoding='utf-8'))
    values['entity.bedroom.bacteria'] = 'Bacteria'
    values.update({f'subtitles.bedroom.bacteria_{key}':pair[index] for key,pair in labels.items()})
    file.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))

# Preserve the redesigned mesh materials when rebuilding audio.
from generate_enemy_designs import generate_bacteria
generate_bacteria()
