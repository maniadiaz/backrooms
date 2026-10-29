"""Create original mono OGG soundscapes. No samples, voices, or external recordings are used.

Install scripts/audio-requirements.txt in a virtual environment, or use the local
.tools/audio-python directory. Minecraft uses only the generated OGG assets.
"""
from pathlib import Path
import json
import sys

PROJECT = Path(__file__).resolve().parents[1]
LOCAL = PROJECT / '.tools/audio-python'
if LOCAL.is_dir():
    sys.path.insert(0, str(LOCAL))

import numpy as np
import soundfile as sf

RATE = 32000
ROOT = PROJECT / 'src/main/resources/assets/bedroom'
OUT = ROOT / 'sounds/ambient'
OUT.mkdir(parents=True, exist_ok=True)
rng = np.random.default_rng(582103)
report = {}


def time(seconds):
    return np.arange(round(seconds * RATE), dtype=np.float64) / RATE


def noise(n, low, high):
    frequencies = np.fft.rfftfreq(n, 1 / RATE)
    spectrum = np.fft.rfft(rng.normal(0, 1, n))
    spectrum *= (frequencies >= low) & (frequencies <= high)
    result = np.fft.irfft(spectrum, n)
    return result / max(float(np.std(result)), 1e-9)


def edge_fade(wave, seconds=.015):
    size = min(round(seconds * RATE), len(wave) // 2)
    ramp = np.sin(np.linspace(0, np.pi / 2, size)) ** 2
    wave[:size] *= ramp
    wave[-size:] *= ramp[::-1]
    return wave


def write(name, wave, peak=.42, fade=True):
    wave = np.asarray(wave, dtype=np.float64)
    if fade:
        wave = edge_fade(wave)
    maximum = float(np.max(np.abs(wave)))
    wave *= min(1, peak / max(maximum, 1e-9))
    assert np.isfinite(wave).all() and np.max(np.abs(wave)) <= peak + 1e-6
    path = OUT / f'{name}.ogg'
    sf.write(path, wave, RATE, format='OGG', subtype='VORBIS')
    decoded, rate = sf.read(path)
    assert decoded.ndim == 1 and rate == RATE and np.isfinite(decoded).all()
    report[name] = {'seconds': len(decoded) / rate, 'channels': 1, 'sample_rate': rate,
                    'peak': round(float(np.max(np.abs(decoded))), 5),
                    'rms': round(float(np.sqrt(np.mean(decoded ** 2))), 5)}


# Exactly periodic 60 Hz fundamentals/harmonics and periodic band-limited noise.
t = time(8)
hum = (.075 * np.sin(2 * np.pi * 60 * t) + .028 * np.sin(2 * np.pi * 120 * t + .3)
       + .013 * np.sin(2 * np.pi * 240 * t) + .004 * noise(len(t), 250, 900))
hum *= 1 + .06 * np.sin(2 * np.pi * t / 8)
for onset in (1.3, 4.8, 6.2):
    start = round(onset * RATE)
    n = round(.065 * RATE)
    hum[start:start+n] += .012 * noise(n, 1300, 6500) * np.hanning(n)
write('fluorescent_hum', hum, .18, fade=False)


def footstep(index):
    t = time(.46)
    impact = .22 * np.sin(2 * np.pi * (105 - index * 4) * t) * np.exp(-t * 34)
    squish = noise(len(t), 140, 3100) * (1 - np.exp(-t * 75)) * np.exp(-t * 13)
    water = .04 * np.sin(2 * np.pi * (380 * t - 180 * t ** 2)) * np.exp(-t * 19)
    return impact + .095 * squish + water


steps = [footstep(index) for index in range(4)]
for index, step in enumerate(steps):
    write(f'wet_step_{index + 1}', step.copy())

def distant_phrase():
    phrase = np.zeros(round(2.25 * RATE))
    for index, onset in enumerate((.02, .46, .98, 1.43)):
        start = round(onset * RATE)
        step = steps[index].copy() * (.62 - index * .05)
        phrase[start:start + len(step)] += step
        echo = round(.09 * RATE)
        phrase[start + echo:start + echo + len(step)] += step * .25
    spectrum = np.fft.rfft(phrase)
    spectrum *= 1 / (1 + (np.fft.rfftfreq(len(phrase), 1 / RATE) / 1800) ** 4)
    return np.fft.irfft(spectrum, len(phrase))

write('distant_steps', distant_phrase(), .28)
write('sanity_steps', distant_phrase() * 1.2, .35)

t = time(.9)
drip = np.zeros_like(t)
for frequency, amplitude, decay in ((1150, .12, 22), (1740, .065, 32), (2300, .03, 45)):
    drip += amplitude * np.sin(2 * np.pi * frequency * t) * np.exp(-t * decay)
drip += .05 * noise(len(t), 300, 5000) * np.exp(-t * 45)
write('ceiling_drip', drip, .35)

t = time(.8)
click = .10 * noise(len(t), 1000, 11000) * np.exp(-t * 55)
click += .09 * np.sin(2 * np.pi * 120 * t) * np.exp(-t * 6)
click += .035 * noise(len(t), 90, 1200) * np.exp(-t * 10)
write('buzz_click', click, .45)

# Nonverbal, breath-like filtered noise; no speech or impersonation.
t = time(2.7)
breath = noise(len(t), 700, 4200)
envelope = (np.sin(np.pi * t / 2.7) ** 2) * (.45 + .55 * np.sin(2 * np.pi * 2.1 * t) ** 2)
whisper = .05 * breath * envelope + .006 * np.sin(2 * np.pi * 190 * t) * envelope
write('sanity_whisper', whisper, .24)

events = {
    'fluorescent_hum': ('fluorescent_hum', 16),
    'distant_steps': ('distant_steps', 32),
    'ceiling_drip': ('ceiling_drip', 16),
    'buzz_click': ('buzz_click', 24),
    'sanity_whisper': ('sanity_whisper', 16),
    'sanity_steps': ('sanity_steps', 20),
}
sounds = {key: {'subtitle': f'subtitles.bedroom.{key}',
                'sounds': [{'name': f'bedroom:ambient/{name}', 'attenuation_distance': distance}]}
          for key, (name, distance) in events.items()}
sounds['wet_step'] = {'subtitle': 'subtitles.bedroom.wet_step',
                      'sounds': [{'name': f'bedroom:ambient/wet_step_{i}', 'attenuation_distance': 16} for i in range(1, 5)]}
(ROOT / 'sounds.json').write_text(json.dumps(sounds, indent=2) + '\n', encoding='utf-8', newline='\n')

labels = {
    'fluorescent_hum': ('Zumbido fluorescente', 'Fluorescent hum'),
    'wet_step': ('Pisadas húmedas', 'Wet footsteps'),
    'distant_steps': ('Pasos lejanos', 'Distant footsteps'),
    'ceiling_drip': ('Goteo del techo', 'Ceiling drip'),
    'buzz_click': ('Fallo eléctrico', 'Electrical fault'),
    'sanity_whisper': ('Susurro', 'Whisper'),
    'sanity_steps': ('Pasos inquietantes', 'Unsettling footsteps'),
}
for language, index in [('es_mx', 0), ('en_us', 1)]:
    path = ROOT / f'lang/{language}.json'
    values = json.loads(path.read_text(encoding='utf-8'))
    values.update({f'subtitles.bedroom.{key}': pair[index] for key, pair in labels.items()})
    path.write_text(json.dumps(values, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')

# Fluorescent fault phases share the existing lit/burnt textures and preserve the block ID.
states = {'variants': {f'fault={i}': {'model': 'bedroom:block/' + ('fluorescent' if i in (0, 2, 4) else 'broken_fluorescent')}
                       for i in range(5)}}
(ROOT / 'blockstates/fluorescent.json').write_text(json.dumps(states, indent=2) + '\n', encoding='utf-8', newline='\n')
(PROJECT / '.tools/audio-report.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report, indent=2))
