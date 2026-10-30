"""Code-native solid flashlight model: grip, battery barrel, reflector, lens and switch."""
import json
from pathlib import Path

faces={side:{'texture':'#metal'} for side in ('north','south','east','west','up','down')}
def box(start,end,texture):
    return {'from':start,'to':end,'faces':{k:{'texture':'#'+texture} for k in faces}}
model={
 'gui_light':'front',
 'textures':{'metal':'minecraft:block/iron_block','grip':'minecraft:block/black_concrete',
             'ring':'minecraft:block/gray_concrete','lens':'minecraft:block/white_concrete','switch':'minecraft:block/red_concrete',
             'particle':'minecraft:block/iron_block'},
 'elements':[
  box([6,6,6],[10,10,15],'grip'),box([5.5,5.5,4],[10.5,10.5,7],'metal'),
  box([5,5,1.5],[11,11,4.5],'ring'),box([5.5,5.5,1.3],[10.5,10.5,1.6],'metal'),
  box([6,6,1.2],[10,10,1.4],'lens'),box([6,6,14],[10,10,15],'metal'),
  box([7,9.8,8],[9,10.5,10],'switch'),
  *[box([5.8,5.8,z],[10.2,10.2,z+.45],'ring') for z in (7.5,9,10.5,12)]],
 'display':{
  'firstperson_righthand':{'rotation':[0,-45,0],'translation':[1.5,1,-2.5],'scale':[.85,.85,.85]},
  'firstperson_lefthand':{'rotation':[0,45,0],'translation':[1.5,1,-2.5],'scale':[.85,.85,.85]},
  'thirdperson_righthand':{'rotation':[90,0,0],'translation':[0,2,0],'scale':[.8,.8,.8]},
  'thirdperson_lefthand':{'rotation':[90,0,0],'translation':[0,2,0],'scale':[.8,.8,.8]},
  'gui':{'rotation':[25,-40,-20],'translation':[0,0,0],'scale':[1.1,1.1,1.1]},
  'ground':{'translation':[0,2,0],'scale':[.6,.6,.6]},
  'fixed':{'rotation':[0,90,0],'scale':[.8,.8,.8]}}}
path=Path(__file__).resolve().parents[1]/'src/main/resources/assets/bedroom/models/item/flashlight.json'
path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
print('Generated solid flashlight model')
