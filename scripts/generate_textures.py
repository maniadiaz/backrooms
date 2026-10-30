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
            elif style == 'box':
                d = 22 if 14 <= x <= 17 else (-24 if x in (0, 31) or y in (0, 31) else 0)
            elif style == 'cabinet':
                d = -55 if y % 16 < 2 else (35 if 12 <= x <= 20 and y % 16 in (6, 7) else 0)
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
    language_path = ROOT / f'lang/{language}.json'
    labels = json.loads(language_path.read_text(encoding='utf-8')) if language_path.exists() else {}
    labels.update({f'block.bedroom.{key}': value for key, value in zip(ids, values)})
    labels['item.bedroom.nutrition_bar'] = 'Barra nutritiva' if language == 'es_mx' else 'Nutrition bar'
    write_json(ROOT / f'lang/{language}.json', labels)

# Preserve the fault properties when rebuilding textures after the audio/lighting stage.
write_json(ROOT / 'blockstates/fluorescent.json', {'variants': {
    f'fault={i}': {'model': 'bedroom:block/' + ('fluorescent' if i in (0, 2, 4) else 'broken_fluorescent')} for i in range(5)}})

# Transparent pixel icon for the wrapped nutrition bar.
data = bytearray()
for y in range(16):
    data.append(0)
    for x in range(16):
        color = (0, 0, 0, 0)
        if 1 <= x <= 14 and 5 <= y <= 10:
            color = (93, 82, 66, 255) if y in (5, 10) else (169, 159, 139, 255)
            if x in (1, 2, 13, 14):
                color = (111 if (x + y) % 2 else 191, 108 if (x + y) % 2 else 186, 99 if (x + y) % 2 else 172, 255)
            elif 6 <= x <= 11 and 6 <= y <= 9:
                color = (103, 76, 48, 255)
            if (x, y) in ((7, 7), (7, 8), (8, 8), (9, 7), (9, 8)):
                color = (219, 204, 159, 255)
        data.extend(color)
def icon_chunk(kind, content):
    return struct.pack('>I', len(content)) + kind + content + struct.pack('>I', zlib.crc32(kind + content) & 0xffffffff)
icon = b'\x89PNG\r\n\x1a\n' + icon_chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
icon += icon_chunk(b'IDAT', zlib.compress(bytes(data))) + icon_chunk(b'IEND', b'')
path = ROOT / 'textures/item/nutrition_bar.png'
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(icon)
write_json(ROOT / 'models/item/nutrition_bar.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'bedroom:item/nutrition_bar'}})

def item_icon(name, frame=0):
    rows = bytearray()
    for y in range(16):
        rows.append(0)
        for x in range(16):
            color = (0, 0, 0, 0)
            if name == 'almond_water':
                if 4 <= x <= 11 and 4 <= y <= 14: color = (217, 208, 177, 255)
                if 6 <= x <= 9 and 1 <= y <= 4: color = (94, 90, 77, 255)
                if 5 <= x <= 10 and 8 <= y <= 11: color = (151, 134, 85, 255)
            elif name == 'energy_drink':
                if 5 <= x <= 10 and 2 <= y <= 13: color = (87, 98, 85, 255)
                if 6 <= x <= 9 and 5 <= y <= 11: color = (169, 222, 61, 255)
                if 5 <= x <= 10 and y in (2, 13): color = (199, 202, 191, 255)
            elif name == 'bandage':
                r = math.hypot(x - 7.5, y - 7.5)
                if r <= 6: color = (192, 184, 160, 255) if int(r) % 2 else (230, 224, 201, 255)
                if r < 2: color = (105, 101, 86, 255)
            elif name == 'battery':
                if (3 <= x <= 6 or 9 <= x <= 12) and 4 <= y <= 13: color = (131, 132, 123, 255)
                if (3 <= x <= 6 or 9 <= x <= 12) and 10 <= y <= 13: color = (84, 90, 66, 255)
                if x in (4, 5, 10, 11) and y == 3: color = (207, 205, 187, 255)
            elif name == 'flashlight':
                if 6 <= x <= 9 and 5 <= y <= 14: color = (96, 101, 94, 255)
                if 4 <= x <= 11 and 2 <= y <= 5: color = (164, 168, 154, 255)
                if 5 <= x <= 10 and y in (2, 3): color = (237, 229, 171, 255)
            else:
                r = math.hypot(x - 7.5, y - 7.5)
                if r <= 6.8: color = (156, 139, 83, 255) if r > 5.4 else (48, 49, 41, 255)
                angle = frame * math.pi / 8
                along = (x - 7.5) * math.sin(angle) - (y - 7.5) * math.cos(angle)
                across = (x - 7.5) * math.cos(angle) + (y - 7.5) * math.sin(angle)
                if abs(across) < 1 and abs(along) < 5: color = (211, 87, 71, 255) if along > 0 else (222, 217, 186, 255)
            rows.extend(color)
    content = b'\x89PNG\r\n\x1a\n' + icon_chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
    content += icon_chunk(b'IDAT', zlib.compress(bytes(rows))) + icon_chunk(b'IEND', b'')
    target = f'anomalous_compass_{frame}' if name == 'anomalous_compass' else name
    (ROOT / f'textures/item/{target}.png').write_bytes(content)
    write_json(ROOT / f'models/item/{target}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'bedroom:item/{target}'}})

for item in ('almond_water', 'energy_drink', 'bandage', 'battery', 'flashlight'):
    item_icon(item)
for frame in range(16): item_icon('anomalous_compass', frame)
write_json(ROOT / 'models/item/anomalous_compass.json', {
    'parent': 'minecraft:item/generated', 'textures': {'layer0': 'bedroom:item/anomalous_compass_0'},
    'overrides': [{'predicate': {'bedroom:angle': frame / 16}, 'model': f'bedroom:item/anomalous_compass_{frame}'} for frame in range(16)]})

for name, base, style, height in (('cardboard_box', (157, 128, 78), 'box', 12), ('filing_cabinet', (127, 134, 124), 'cabinet', 16)):
    png(name, base, style)
    for opened in (False, True):
        suffix = 'open' if opened else 'closed'
        elements = [{'from': [2, 0, 2], 'to': [14, height, 14], 'faces': faces}]
        if opened:
            elements = [{'from': [2, 0, 2], 'to': [14, height - 2, 14], 'faces': faces},
                        {'from': [1, height - 1, 1], 'to': [15, height, 3], 'faces': faces}]
        write_json(ROOT / f'models/block/{name}_{suffix}.json', {'textures': {'all': f'bedroom:block/{name}', 'particle': f'bedroom:block/{name}'}, 'elements': elements})
    write_json(ROOT / f'blockstates/{name}.json', {'variants': {f'opened={str(value).lower()}': {'model': f'bedroom:block/{name}_{"open" if value else "closed"}'} for value in (False, True)}})
    write_json(ROOT / f'models/item/{name}.json', {'parent': f'bedroom:block/{name}_closed'})
write_json(ROOT / 'blockstates/beam_light.json', {'variants': {'': {'model': 'bedroom:block/beam_light'}}})
write_json(ROOT / 'models/block/beam_light.json', {'textures': {'particle': 'minecraft:block/glass'}, 'elements': []})

for language in names:
    path = ROOT / f'lang/{language}.json'
    labels = json.loads(path.read_text(encoding='utf-8'))
    es = language == 'es_mx'
    for key, a, b in [('almond_water', 'Agua de Almendras', 'Almond water'), ('energy_drink', 'Bebida energética Juice', 'Juice energy drink'),
                      ('bandage', 'Vendas', 'Bandages'), ('battery', 'Pilas AAA', 'AAA batteries'), ('flashlight', 'Linterna', 'Flashlight'),
                      ('anomalous_compass', 'Brújula anómala', 'Anomalous compass')]:
        labels[f'item.bedroom.{key}'] = a if es else b
    labels.update({'block.bedroom.cardboard_box': 'Caja de cartón' if es else 'Cardboard box',
                   'block.bedroom.filing_cabinet': 'Archivador metálico' if es else 'Filing cabinet',
                   'key.categories.bedroom': 'Backrooms', 'key.bedroom.next_camera': 'Siguiente cámara' if es else 'Next spectator camera',
                   'key.bedroom.previous_camera': 'Cámara anterior' if es else 'Previous spectator camera'})
    write_json(path, labels)

# Preserve the solid held model when regenerating the rest of the sprites.
import runpy
runpy.run_path(str(Path(__file__).with_name('generate_flashlight_model.py')))
