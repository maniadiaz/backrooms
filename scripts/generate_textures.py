"""Rebuild original pixel textures and block models using only Python's standard library."""
import json
import math
from pathlib import Path
import random
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/bedroom'


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def png(name, base, style):
    rng = random.Random(name)
    data = bytearray()
    for y in range(32):
        data.append(0)
        for x in range(32):
            noise = rng.randint(-5, 5)
            d = 0
            if style == 'paper':
                d = (-9 if x % 8 == 0 else 0) - int(max(0, math.sin(x * .19 + y * .1)) * 5)
            elif style == 'carpet':
                d = (-5 if (x + y) % 3 == 0 else 0)
            elif style == 'wet':
                d = -int(18 * (1 + math.sin(x * .25) * math.cos(y * .17)))
            elif style == 'ceiling':
                d = -35 if x in (0, 31) or y in (0, 31) else rng.randint(-7, 2)
            elif style == 'lamp':
                d = -65 if x < 3 or x > 28 or y < 3 or y > 28 else (-30 if x % 7 < 2 else 10)
            data.extend(max(0, min(255, c + noise + d)) for c in base)
    def chunk(kind, content):
        return struct.pack('>I', len(content)) + kind + content + struct.pack('>I', zlib.crc32(kind + content) & 0xffffffff)
    content = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 32, 32, 8, 2, 0, 0, 0))
    content += chunk(b'IDAT', zlib.compress(bytes(data))) + chunk(b'IEND', b'')
    path = ROOT / f'textures/block/{name}.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)


textures = {
    'wallpaper': ((186, 169, 100), 'paper'),
    'damp_carpet': ((131, 112, 65), 'carpet'),
    'wet_carpet': ((124, 107, 65), 'wet'),
    'ceiling_tile': ((191, 190, 177), 'ceiling'),
    'fluorescent': ((224, 225, 195), 'lamp'),
    'broken_fluorescent': ((117, 116, 98), 'lamp'),
    'transition_wall': ((166, 150, 85), 'paper'),
}
for name, (base, style) in textures.items():
    png(name, base, style)
    write_json(ROOT / f'blockstates/{name}.json', {'variants': {'': {'model': f'bedroom:block/{name}'}}})
    write_json(ROOT / f'models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'bedroom:block/{name}'}})
    write_json(ROOT / f'models/item/{name}.json', {'parent': f'bedroom:block/{name}'})

faces = {face: {'texture': '#all'} for face in ('north', 'south', 'east', 'west', 'up', 'down')}
for axis in ('x', 'z', 'y'):
    boxes = []
    if axis in ('x', 'y'):
        boxes.append({'from': [0, 0, 6], 'to': [16, 16, 10], 'faces': faces})
    if axis in ('z', 'y'):
        boxes.append({'from': [6, 0, 0], 'to': [10, 16, 16], 'faces': faces})
    write_json(ROOT / f'models/block/wall_panel_{axis}.json', {'textures': {'all': 'bedroom:block/wallpaper', 'particle': 'bedroom:block/wallpaper'}, 'elements': boxes})
write_json(ROOT / 'blockstates/wall_panel.json', {'variants': {f'axis={axis}': {'model': f'bedroom:block/wall_panel_{axis}'} for axis in ('x', 'y', 'z')}})
write_json(ROOT / 'models/item/wall_panel.json', {'parent': 'bedroom:block/wall_panel_x'})

names = {
    'es_mx': ['Papel tapiz desgastado', 'Panel de papel tapiz', 'Moqueta húmeda', 'Moqueta empapada', 'Panel de techo', 'Fluorescente', 'Fluorescente fundido', 'Pared anómala (salida provisional)'],
    'en_us': ['Worn wallpaper', 'Wallpaper panel', 'Damp carpet', 'Soaked carpet', 'Ceiling tile', 'Fluorescent light', 'Burnt-out fluorescent', 'Anomalous wall (temporary exit)'],
}
ids = ['wallpaper', 'wall_panel', 'damp_carpet', 'wet_carpet', 'ceiling_tile', 'fluorescent', 'broken_fluorescent', 'transition_wall']
for language, values in names.items():
    write_json(ROOT / f'lang/{language}.json', {f'block.bedroom.{key}': value for key, value in zip(ids, values)})
