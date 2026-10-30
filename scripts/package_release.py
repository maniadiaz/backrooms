"""Build checked client/server ZIPs without worlds, credentials, or third-party binaries."""
from pathlib import Path
import hashlib,json,zipfile,tomllib
PROJECT=Path(__file__).resolve().parents[1]
props=dict(line.split('=',1) for line in (PROJECT/'gradle.properties').read_text().splitlines() if '=' in line)
version=props['mod_version'];jar=PROJECT/f'build/libs/backrooms-{version}.jar'
if not jar.is_file():raise SystemExit('Compile the current version before packaging.')
out=PROJECT/'dist';out.mkdir(exist_ok=True)
sha=lambda data:hashlib.sha256(data).hexdigest()
jar_bytes=jar.read_bytes()
with zipfile.ZipFile(jar) as archive:
    names=set(archive.namelist())
    for path in ['META-INF/mods.toml','data/bedroom/dimension/level_1.json','data/bedroom/dimension/darkrooms.json','data/bedroom/advancements/level_zero_complete.json','assets/bedroom/textures/entity/bacteria.png','assets/bedroom/models/item/hazmat_suit.json','assets/bedroom/models/block/hiding_locker.json','assets/bedroom/sounds/ambient/geiger.ogg']:
        assert path in names,f'Missing required release asset: {path}'
    for path in ['assets/bedroom/models/block/checkpoint_terminal.json','assets/bedroom/textures/environment/recovery_door.png','assets/bedroom/sounds/ambient/recovery_heartbeat.ogg']:
        assert path in names,f'Missing checkpoint/recovery release asset: {path}'
    metadata=archive.read('META-INF/mods.toml').decode()
    assert f'version="{version}"' in metadata and 'displayName="Backrooms"' in metadata and 'modId="bedroom"' in metadata
for config in (PROJECT/'pack/config').glob('*.toml'):tomllib.loads(config.read_text())
for kind in ['client','server']:
    files={f'mods/{jar.name}':jar_bytes,'INSTALACION.md':(PROJECT/'docs/INSTALACION.md').read_bytes(),
           'LEEME.md':f'# Backrooms {version}\n\nLeer [instalación](INSTALACION.md), [reglas y controles](COMO-JUGAR.md) y [pruebas y límites](PRUEBAS.md).\n\nMinecraft Java 1.20.1 / Forge 47.4.28 / Java 17. Essential se descarga por separado.\n'.encode(),
           'COMO-JUGAR.md':(PROJECT/'docs/COMO-JUGAR.md').read_bytes(),'PRUEBAS.md':(PROJECT/'docs/ENTREGA-FINAL.md').read_bytes(),'AVISOS.md':(PROJECT/'THIRD_PARTY_NOTICES.md').read_bytes(),
           'ACTUALIZACION-1.1.0.md':(PROJECT/'docs/ACTUALIZACION-1.1.0.md').read_bytes(),
           'ENEMIGOS-1.2.0.md':(PROJECT/'docs/ENEMIGOS-1.2.0.md').read_bytes(),
           'CAMPANIA-NIVEL-0-1.3.0.md':(PROJECT/'docs/CAMPANIA-NIVEL-0-1.3.0.md').read_bytes(),
           'REGLAS-Y-CHECKPOINTS-1.4.0.md':(PROJECT/'docs/REGLAS-Y-CHECKPOINTS-1.4.0.md').read_bytes()}
    for config in (PROJECT/'pack/config').glob('*.toml'):
        if kind=='server' and config.name=='bedroom-client.toml':continue
        files[f'config/{config.name}']=config.read_bytes()
    if kind=='server':files['server.properties.example']=(PROJECT/'pack/server.properties.example').read_bytes()
    manifest={'name':'Backrooms','version':version,'package':kind,'format':'manual-forge-directory','modId':'bedroom','command':'/backrooms','legacyCommand':'/bedroom',
              'minecraft':props['minecraft_version'],'forge':props['forge_version'],'java':17,'players':'1-4',
              'essential':'optional; download separately for Forge 1.20.1 on all clients',
              'files':{name:sha(data) for name,data in sorted(files.items())}}
    files['backrooms-manifest.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
    target=out/f'backrooms-{version}-{kind}.zip'
    with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED) as archive:
        for name,data in sorted(files.items()):
            info=zipfile.ZipInfo(name,date_time=(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;archive.writestr(info,data)
    with zipfile.ZipFile(target) as archive:
        assert archive.testzip() is None
        for name,digest in manifest['files'].items():assert sha(archive.read(name))==digest
        assert all(not name.startswith(('/', '../')) and '..' not in Path(name).parts for name in archive.namelist())
        assert not any(name.startswith(('saves/','logs/','screenshots/')) for name in archive.namelist())
    print(f'{target.name}: {target.stat().st_size} bytes, integrity verified')
(out/'SHA256SUMS.txt').write_text('\n'.join(f'{sha(file.read_bytes())}  {file.name}' for file in sorted(out.glob(f'backrooms-{version}-*.zip')))+'\n'+f'{sha(jar_bytes)}  {jar.name}\n',encoding='utf-8')
