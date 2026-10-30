"""Original emissive pixel face and synthesized mono Smiler audio; no external samples."""
from pathlib import Path
import json
import math
import struct
import sys
import zlib

PROJECT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT / '.tools/audio-python'))
import numpy as np
import soundfile as sf

ROOT = PROJECT / 'src/main/resources/assets/bedroom'
raw = bytearray()
for y in range(64):
    raw.append(0)
    for x in range(64):
        value = 0
        for center in (19, 45):
            radius = ((x-center)/6)**2 + ((y-22)/4)**2
            if radius <= 1: value = max(value, int(210 + 45*(1-radius)))
            elif radius < 1.55: value = max(value, int(65*(1.55-radius)/.55))
        norm = (x-31.5)/24
        if abs(norm) < 1:
            top = 34 + 10*(1-norm*norm)
            bottom = top + 6*(1-norm*norm)
            if top <= y <= bottom:
                value = max(value, 245 if x % 5 != 0 and abs(y-(top+bottom)/2) > .35 else 95)
            elif min(abs(y-top), abs(y-bottom)) < 1.2:
                value = max(value, 60)
        raw.extend((value, value, int(value*.96), 255 if value else 0))
def chunk(kind, data):
    return struct.pack('!I',len(data)) + kind + data + struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
file = ROOT / 'textures/entity/smiler.png'
file.parent.mkdir(parents=True, exist_ok=True)
file.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR',struct.pack('!2I5B',64,64,8,6,0,0,0)) + chunk(b'IDAT',zlib.compress(raw)) + chunk(b'IEND',b''))

RATE = 32000
rng = np.random.default_rng(6381)
def filtered(seconds, low, high):
    n = round(seconds*RATE)
    freq = np.fft.rfftfreq(n,1/RATE)
    spectrum = np.fft.rfft(rng.normal(0,1,n))
    spectrum *= (freq >= low) & (freq <= high)
    result = np.fft.irfft(spectrum,n)
    return result/max(np.std(result),1e-9)

events = json.loads((ROOT/'sounds.json').read_text())
labels = {'hiss': ('Siseo del Smiler','Smiler hissing'), 'laugh': ('Risa distorsionada','Distorted laughter'), 'attack': ('Estallido del Smiler','Smiler strike')}
report = {}
for kind, seconds in [('hiss',4),('laugh',1.8),('attack',.9)]:
    t = np.arange(round(seconds*RATE))/RATE
    if kind == 'hiss':
        # Periodic FFT noise and exact cycles allow seamless looping without a voiced tone.
        wave = filtered(seconds,1500,6500)*(.8+.2*np.cos(2*np.pi*2*t))
    elif kind == 'laugh':
        pulses = np.maximum(0,np.sin(2*np.pi*4.5*t))**3
        tone = np.sin(2*np.pi*(185*t+5*np.sin(2*np.pi*3*t)))
        wave = (tone*.5+filtered(seconds,400,2400)*.25)*pulses*np.sin(np.pi*t/seconds)**2
    else:
        wave = (filtered(seconds,300,5500)+.3*np.sin(2*np.pi*(180*t+120*t*t)))*np.sin(np.pi*t/seconds)**2*np.exp(-t*2)
    wave *= .68/max(np.max(np.abs(wave)),1e-9)
    file = ROOT/f'sounds/ambient/smiler_{kind}.ogg'
    sf.write(file,wave,RATE,format='OGG',subtype='VORBIS')
    decoded, rate = sf.read(file)
    assert rate == RATE and decoded.ndim == 1 and np.isfinite(decoded).all()
    report[kind] = {'seconds':len(decoded)/rate,'channels':1,'rate':rate,'peak':float(np.max(np.abs(decoded)))}
    events[f'smiler_{kind}'] = {'subtitle':f'subtitles.bedroom.smiler_{kind}', 'sounds':[{'name':f'bedroom:ambient/smiler_{kind}','attenuation_distance':24}]}
(ROOT/'sounds.json').write_text(json.dumps(events,indent=2)+'\n',encoding='utf-8')
for language, index in [('es_mx',0),('en_us',1)]:
    file = ROOT/f'lang/{language}.json'
    values = json.loads(file.read_text(encoding='utf-8'))
    values['entity.bedroom.smiler'] = 'Smiler'
    values.update({f'subtitles.bedroom.smiler_{key}':pair[index] for key,pair in labels.items()})
    file.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
