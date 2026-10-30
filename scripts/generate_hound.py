"""Original Hound assets: deterministic pixel skin and synthesized mono sounds."""
from pathlib import Path
import json
import random
import struct
import zlib
import sys

PROJECT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT / '.tools/audio-python'))
import numpy as np
import soundfile as sf

ROOT = PROJECT / 'src/main/resources/assets/bedroom'
rng = random.Random(812)
raw = bytearray()
for y in range(64):
    raw.append(0)
    for x in range(64):
        shade = rng.randint(-12, 12)
        color = (137 + shade, 131 + shade, 119 + shade, 255)
        if y < 8 and x >= 40:
            color = (186 + shade, 177 + shade, 151 + shade, 255)
        # Front of head: two hollow sockets and cracked skin.
        if 5 <= x <= 10 and 27 <= y <= 32:
            if y in (28, 29) and x in (5, 6, 9, 10): color = (15, 13, 14, 255)
            elif x == 7 and y >= 30: color = (81, 64, 60, 255)
        # Teeth across the front of the open lower jaw.
        if 30 <= x < 36 and 28 <= y < 30:
            color = (183, 174, 132, 255) if x % 2 == 0 else (40, 28, 26, 255)
        raw.extend(color)
def chunk(kind, data):
    return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data) & 0xffffffff)
image = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 64, 64, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')
path = ROOT / 'textures/entity/hound.png'
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(image)

sounds = json.loads((ROOT / 'sounds.json').read_text())
labels = {'steps': ('Garras húmedas', 'Wet claws'), 'breath': ('Jadeo del sabueso', 'Hound panting'),
          'lure': ('Lamento extraño', 'Strange lament'), 'growl': ('Gruñido del sabueso', 'Hound growl'),
          'charge': ('El sabueso carga', 'Hound charges'), 'attack': ('Aullido desgarrador', 'Piercing howl')}
np_rng = np.random.default_rng(971)
for index, (kind, label) in enumerate(labels.items()):
    seconds = [.32, 1.4, 2.2, 1.1, .8, .65][index]
    t = np.arange(round(seconds * 32000)) / 32000
    noise = np_rng.normal(0, 1, len(t))
    frequencies = np.fft.rfftfreq(len(t), 1 / 32000)
    spectrum = np.fft.rfft(noise)
    spectrum *= (frequencies > 100) & (frequencies < (2400 if kind == 'steps' else 1300))
    rough = np.fft.irfft(spectrum, len(t))
    fundamental = [85, 105, 225, 62, 95, 330][index]
    phase = 2 * np.pi * (fundamental * t + 8 * np.sin(t * 6))
    voiced = sum(np.sin(phase * harmonic) / harmonic for harmonic in range(1, 8))
    envelope = np.sin(np.pi * t / seconds) ** 2
    if kind == 'steps': envelope = np.exp(-t * 20) * np.minimum(t * 200, 1)
    if kind == 'breath': envelope *= np.sin(t * 10) ** 2
    wave = (rough * (.8 if kind in ('steps', 'breath') else .25) + voiced * .35) * envelope
    wave = np.tanh(wave * 1.4)
    wave *= .72 / max(np.max(np.abs(wave)), 1e-6)
    file = ROOT / f'sounds/ambient/hound_{kind}.ogg'
    sf.write(file, wave, 32000, format='OGG', subtype='VORBIS')
    decoded, rate = sf.read(file)
    assert rate == 32000 and decoded.ndim == 1 and np.isfinite(decoded).all()
    sounds[f'hound_{kind}'] = {'subtitle': f'subtitles.bedroom.hound_{kind}', 'sounds': [{'name': f'bedroom:ambient/hound_{kind}', 'attenuation_distance': 24}]}
(ROOT / 'sounds.json').write_text(json.dumps(sounds, indent=2) + '\n')
for lang, idx in [('es_mx', 0), ('en_us', 1)]:
    file = ROOT / f'lang/{lang}.json'
    values = json.loads(file.read_text(encoding='utf-8'))
    values['entity.bedroom.hound'] = 'Sabueso' if idx == 0 else 'Hound'
    values.update({f'subtitles.bedroom.hound_{key}': label[idx] for key, label in labels.items()})
    file.write_text(json.dumps(values, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Hound: skin and six validated mono OGG files generated.')
