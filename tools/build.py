#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""One entry point for target validation, dependency preparation and all builds."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
import tomllib
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
COMMON_CLASS = 'com/november/mcphone/workshop/ModularWorkshop.class'


def read_json(path: Path):
    return json.loads(path.read_text(encoding='utf-8'))


def properties(path: Path) -> dict[str, str]:
    return dict(line.split('=', 1) for raw in path.read_text(encoding='utf-8').splitlines()
                if (line := raw.strip()) and not line.startswith('#') and '=' in line)


def digest(path: Path) -> str:
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def layer_directory(name: str, layer: dict) -> Path:
    if not re.fullmatch(r'[A-Za-z0-9_.+\-]+', name):
        raise ValueError(f'Invalid layer name: {name}')
    category = 'both' if layer.get('since') and layer.get('loader') else (
        'version' if layer.get('since') else 'loader')
    return ROOT / 'layers' / category / name


def version_parts(value: str) -> tuple[int, ...]:
    return tuple(int(part) for part in value.split('.'))


def check() -> tuple[dict, dict, dict]:
    targets = read_json(ROOT / 'versions/targets.json')
    lock = read_json(ROOT / 'versions/upstream.lock.json')
    layers = read_json(ROOT / 'versions/layers.json')
    identity = properties(ROOT / 'gradle.properties')
    if not targets or set(targets) != set(lock['targets']):
        raise ValueError('Target set must exactly match the locked MCphone release.')
    if lock['repository'] != 'november521/mcphone' or lock['ref'] != f"v{lock['version']}":
        raise ValueError('Invalid upstream release identity.')
    if not re.fullmatch(r'[0-9a-f]{40}', lock['commit']):
        raise ValueError('Upstream must be pinned to a commit.')
    for key in ('mod_id', 'mod_name', 'mod_version', 'mod_group_id', 'mod_license',
                'mod_authors', 'artifact_name'):
        if not identity.get(key):
            raise ValueError(f'Missing repository identity: {key}')
    if identity['mod_license'] != 'MIT':
        raise ValueError('The project license must remain MIT.')
    if not re.fullmatch(r'[0-9A-Za-z_.+\-]+', identity['mod_version']):
        raise ValueError('Unsafe mod version.')
    used_layers = set()
    for name, target in targets.items():
        if name != f"{target['minecraft']}-{target['loader'].lower()}":
            raise ValueError(f'Target name does not match its platform: {name}')
        if target['project'] != f'platforms/{name}' or not target.get('buildable'):
            raise ValueError(f'Required release target cannot be disabled or relocated: {name}')
        upstream = lock['targets'][name]
        for key in ('minecraft', 'loader', 'loader_version', 'java', 'gradle', 'mappings'):
            if target[key] != upstream[key]:
                raise ValueError(f'{name}: {key} differs from locked MCphone.')
        if target['mappings'] != 'official':
            raise ValueError(f'{name}: shared sources require official mappings.')
        asset = upstream['asset']
        if not re.fullmatch(r'[0-9a-f]{64}', asset['sha256']):
            raise ValueError(f'{name}: invalid dependency checksum.')
        expected_url = (f"https://github.com/{lock['repository']}/releases/download/"
                        f"{lock['ref']}/mcphone-{lock['version']}-{name}.jar")
        if asset['url'] != expected_url:
            raise ValueError(f'{name}: dependency URL must match the locked public release.')
        project = ROOT / target['project']
        for file in ('build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew',
                     'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar'):
            if not (project / file).is_file():
                raise ValueError(f'{name}: missing {file}')
        wrapper = properties(project / 'gradle/wrapper/gradle-wrapper.properties')
        if not wrapper['distributionUrl'].endswith(f"gradle-{target['gradle']}-bin.zip"):
            raise ValueError(f'{name}: wrapper version differs from manifest.')
        if set(identity) & set(properties(project / 'gradle.properties')):
            raise ValueError(f'{name}: identity must only be declared at repository root.')
        for layer_name in target['layers']:
            if layer_name not in layers:
                raise ValueError(f'{name}: undeclared layer {layer_name}')
            layer = layers[layer_name]
            if not layer.get('since') and not layer.get('loader'):
                raise ValueError(f'{layer_name}: unconstrained code belongs in shared/.')
            if layer.get('loader', target['loader']) != target['loader']:
                raise ValueError(f'{name}: incompatible loader layer {layer_name}')
            if layer.get('since') and version_parts(target['minecraft']) < version_parts(layer['since']):
                raise ValueError(f'{name}: incompatible version layer {layer_name}')
            folder = layer_directory(layer_name, layer)
            if not folder.is_dir() or not any(p.is_file() for p in folder.rglob('*')):
                raise ValueError(f'{layer_name}: declared layer is empty.')
            used_layers.add(layer_name)
    if used_layers != set(layers):
        raise ValueError('Every declared layer must be used.')
    projects = {p.name for p in (ROOT / 'platforms').iterdir() if p.is_dir()}
    if projects != set(targets):
        raise ValueError('Platform directories and target manifest differ.')
    forbidden = re.compile(r'\bimport\s+(?:net\.(?:minecraftforge|neoforged|fabricmc)\.|'
                           r'com\.november\.mcphone\.(?:core|feature|platform)\.)')
    for source in (ROOT / 'shared/src/main/java').rglob('*.java'):
        if forbidden.search(source.read_text(encoding='utf-8')):
            raise ValueError(f'{source}: shared code depends on loader or MCphone internals.')
    return targets, lock, identity


def prepare(name: str, lock: dict) -> Path:
    version = f"{lock['version']}-{name}"
    folder = ROOT / '.cache/maven/com/november/mcphone/mcphone' / version
    folder.mkdir(parents=True, exist_ok=True)
    artifact = folder / f'mcphone-{version}.jar'
    asset = lock['targets'][name]['asset']
    if not artifact.is_file() or digest(artifact) != asset['sha256']:
        temporary = artifact.with_suffix('.jar.part')
        request = urllib.request.Request(asset['url'], headers={'User-Agent': 'MCphone-Modular-Workshop'})
        try:
            with urllib.request.urlopen(request, timeout=90) as response, temporary.open('wb') as output:
                shutil.copyfileobj(response, output)
            if digest(temporary) != asset['sha256']:
                raise ValueError(f'{name}: MCphone dependency checksum mismatch.')
            temporary.replace(artifact)
        finally:
            temporary.unlink(missing_ok=True)
    pom = (f'<project><modelVersion>4.0.0</modelVersion><groupId>com.november.mcphone</groupId>'
           f'<artifactId>mcphone</artifactId><version>{version}</version></project>\n')
    (folder / f'mcphone-{version}.pom').write_text(pom, encoding='utf-8')
    return artifact


def java_home(major: int) -> Path:
    candidates = [os.environ.get(f'JAVA_HOME_{major}_X64'), os.environ.get(f'JAVA_{major}_HOME'),
                  os.environ.get('JAVA_HOME')]
    java = shutil.which('java')
    if java:
        candidates.append(str(Path(java).resolve().parent.parent))
    executable = 'java.exe' if os.name == 'nt' else 'java'
    for candidate in dict.fromkeys(c for c in candidates if c):
        binary = Path(candidate) / 'bin' / executable
        if not binary.is_file():
            continue
        probe = subprocess.run([str(binary), '-version'], capture_output=True, text=True)
        found = re.search(r'version\s+"(\d+)', probe.stdout + probe.stderr)
        if probe.returncode == 0 and found and int(found[1]) == major:
            return Path(candidate)
    raise ValueError(f'Java {major} is required. Set JAVA_HOME_{major}_X64 or JAVA_{major}_HOME.')


def jar_name(name: str, identity: dict) -> str:
    return f"{identity['artifact_name']}-{identity['mod_version']}-{name}.jar"


def verify_jar(path: Path, name: str, target: dict, lock: dict, identity: dict):
    with zipfile.ZipFile(path) as jar:
        files = jar.namelist()
        if COMMON_CLASS not in files or 'LICENSE' not in files:
            raise ValueError(f'{path}: missing common bootstrap or license.')
        entrypoint = {'Forge': 'WorkshopForge', 'NeoForge': 'WorkshopNeoForge',
                      'Fabric': 'WorkshopFabric'}[target['loader']]
        if f'com/november/mcphone/workshop/platform/{entrypoint}.class' not in files:
            raise ValueError(f'{path}: missing platform entrypoint.')
        if len(files) != len(set(files)):
            raise ValueError(f'{path}: duplicate JAR entries.')
        if any((f.endswith('.class') and f.startswith('com/november/mcphone/')
                and not f.startswith('com/november/mcphone/workshop/'))
               or f.startswith('META-INF/jars/')
               or f.startswith('META-INF/jarjar/') for f in files):
            raise ValueError(f'{path}: must not embed MCphone or nested dependencies.')
        for file in files:
            if file.endswith('.class'):
                content = jar.read(file)
                if int.from_bytes(content[6:8], 'big') > target['java'] + 44:
                    raise ValueError(f'{path}: class requires a newer Java than declared.')
        metadata_files = {'fabric.mod.json', 'META-INF/mods.toml', 'META-INF/neoforge.mods.toml'}
        expected = {'Forge': 'META-INF/mods.toml', 'NeoForge': 'META-INF/neoforge.mods.toml',
                    'Fabric': 'fabric.mod.json'}[target['loader']]
        if metadata_files.intersection(files) != {expected}:
            raise ValueError(f'{path}: incorrect platform metadata.')
        text = jar.read(expected).decode('utf-8')
        if '${' in text:
            raise ValueError(f'{path}: unexpanded metadata placeholders.')
        metadata = json.loads(text) if target['loader'] == 'Fabric' else tomllib.loads(text)
        if target['loader'] == 'Fabric':
            actual_version = metadata['version']
            actual_id = metadata['id']
            dependency = metadata['depends']['mcphone']
            if dependency != f"={lock['version']}":
                raise ValueError(f'{path}: MCphone runtime dependency differs from lock.')
        else:
            actual_version = metadata['mods'][0]['version']
            actual_id = metadata['mods'][0]['modId']
            dependencies = metadata['dependencies'][identity['mod_id']]
            dependency = next(d for d in dependencies if d['modId'] == 'mcphone')
            required = dependency.get('mandatory') is True if target['loader'] == 'Forge' else (
                dependency.get('type') == 'required')
            if not required or dependency['versionRange'] != f"[{lock['version']}]":
                raise ValueError(f'{path}: MCphone must be a required, locked runtime dependency.')
        if actual_id != identity['mod_id'] or actual_version != identity['mod_version']:
            raise ValueError(f'{path}: inconsistent mod identity.')


def publish_bundle(staging: Path, selected: list[str], targets: dict, lock: dict, identity: dict) -> Path:
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    dirty = bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True))
    rows = []
    for name in selected:
        jar = staging / jar_name(name, identity)
        verify_jar(jar, name, targets[name], lock, identity)
        rows.append({'target': name, 'file': jar.name, 'sha256': digest(jar)})
    report = {'version': identity['mod_version'], 'commit': revision, 'dirty': dirty,
              'mcphone': {'version': lock['version'], 'commit': lock['commit']},
              'complete': set(selected) == set(targets), 'artifacts': rows}
    (staging / 'build-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    (staging / 'SHA256SUMS').write_text(''.join(f"{r['sha256']}  {r['file']}\n" for r in rows), encoding='utf-8')
    parent = ROOT / 'dist' / identity['mod_version']
    parent.mkdir(parents=True, exist_ok=True)
    output = parent / f"{time.strftime('%Y%m%d-%H%M%S')}-{staging.parent.name}"
    shutil.move(str(staging), output)
    print(f'Artifacts ({len(rows)}/{len(targets)}): {output}', flush=True)
    return output


def build(selected: list[str], targets: dict, lock: dict, identity: dict):
    jdks = {targets[name]['java']: java_home(targets[name]['java']) for name in selected}
    for name in selected:
        prepare(name, lock)
    cache = ROOT / '.cache'
    cache.mkdir(exist_ok=True)
    failures = []
    with tempfile.TemporaryDirectory(prefix='build-', dir=cache) as temporary:
        staging = Path(temporary) / 'bundle'
        staging.mkdir()
        for name in selected:
            target = targets[name]
            project = ROOT / target['project']
            artifact = project / 'build/libs' / jar_name(name, identity)
            # An old JAR must never make a failed build appear successful.
            artifact.unlink(missing_ok=True)
            env = os.environ.copy()
            env['JAVA_HOME'] = str(jdks[target['java']])
            env['PATH'] = str(jdks[target['java']] / 'bin') + os.pathsep + env.get('PATH', '')
            command = (['cmd.exe', '/d', '/c', 'gradlew.bat'] if os.name == 'nt'
                       else ['sh', './gradlew']) + ['build', '--no-daemon', '--console=plain']
            print(f'Building {name} (Java {target["java"]}, Gradle {target["gradle"]})', flush=True)
            result = subprocess.run(command, cwd=project, env=env)
            try:
                if result.returncode:
                    raise ValueError(f'Gradle exited with {result.returncode}')
                verify_jar(artifact, name, target, lock, identity)
                shutil.copyfile(artifact, staging / artifact.name)
            except (ValueError, OSError, zipfile.BadZipFile, KeyError) as error:
                failures.append(f'{name}: {error}')
                print(f'FAILED {failures[-1]}', file=sys.stderr, flush=True)
        if failures:
            raise ValueError('No bundle was published. Failed targets:\n' + '\n'.join(failures))
        publish_bundle(staging, selected, targets, lock, identity)


def check_upstream(targets: dict, lock: dict):
    base = f'https://api.github.com/repos/{lock["repository"]}'
    headers = {'User-Agent': 'MCphone-Modular-Workshop', 'Accept': 'application/vnd.github+json'}
    def get(url):
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=45) as response:
            return json.load(response)
    release = get(f'{base}/releases/latest')
    ref = release['tag_name']
    commit = get(f'{base}/commits/{ref}')['sha']
    import base64
    manifest = get(f'{base}/contents/versions/targets.json?ref={ref}')
    upstream = json.loads(base64.b64decode(manifest['content']))
    names = {name for name, row in upstream.items() if isinstance(row, dict) and row.get('buildable')}
    if set(targets) != names:
        raise ValueError(f'Upstream target drift: missing={sorted(names - set(targets))}, '
                         f'extra={sorted(set(targets) - names)}')
    if ref != lock['ref']:
        raise ValueError(f'MCphone is now {ref}; review and update the lock from {lock["ref"]}.')
    if commit != lock['commit']:
        raise ValueError('The upstream release tag no longer matches the locked commit.')
    assets = {asset['name']: asset for asset in release['assets']}
    for name in names:
        for key in ('minecraft', 'loader', 'loader_version', 'java', 'gradle', 'mappings'):
            if upstream[name][key] != targets[name][key]:
                raise ValueError(f'{name}: upstream {key} drift.')
        asset = lock['targets'][name]['asset']
        if assets.get(asset['name'], {}).get('digest') != f"sha256:{asset['sha256']}":
            raise ValueError(f'{name}: upstream release asset differs from the lock.')
    print(f'Aligned with latest MCphone release {ref}.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['check', 'matrix', 'prepare', 'build', 'bundle', 'check-upstream'])
    parser.add_argument('--target', action='append', help='Repeat to select targets; default: all.')
    parser.add_argument('--input', type=Path, help='JAR directory for bundle aggregation.')
    args = parser.parse_args()
    targets, lock, identity = check()
    selected = args.target or list(targets)
    if len(selected) != len(set(selected)) or not set(selected) <= set(targets):
        raise ValueError('Unknown or duplicate target selection.')
    if args.command == 'check':
        print(f'Architecture OK: {len(targets)} targets aligned with MCphone {lock["version"]}.')
    elif args.command == 'matrix':
        print(json.dumps({'include': [{'target': name, 'java': targets[name]['java']} for name in selected]}))
    elif args.command == 'check-upstream':
        check_upstream(targets, lock)
    elif args.command == 'prepare':
        for name in selected:
            print(prepare(name, lock))
    elif args.command == 'build':
        build(selected, targets, lock, identity)
    elif args.command == 'bundle':
        if not args.input or args.target:
            raise ValueError('bundle requires --input and always verifies the complete target set.')
        jars = list(args.input.rglob('*.jar'))
        if sorted(j.name for j in jars) != sorted(jar_name(name, identity) for name in targets):
            raise ValueError('Bundle input must contain exactly one release JAR per target.')
        cache = ROOT / '.cache'
        cache.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(prefix='collect-', dir=cache) as temporary:
            staging = Path(temporary) / 'bundle'
            staging.mkdir()
            for jar in jars:
                shutil.copyfile(jar, staging / jar.name)
            publish_bundle(staging, list(targets), targets, lock, identity)


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, KeyError, subprocess.CalledProcessError, zipfile.BadZipFile) as error:
        print(f'ERROR: {error}', file=sys.stderr)
        sys.exit(1)
