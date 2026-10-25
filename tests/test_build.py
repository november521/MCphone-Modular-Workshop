# SPDX-License-Identifier: MIT
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

SPEC = importlib.util.spec_from_file_location('workshop_build', Path(__file__).parents[1] / 'tools/build.py')
build = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(build)


class ArchitectureTests(unittest.TestCase):
    def setUp(self):
        self.targets, self.lock, self.identity = build.check()
        (build.ROOT / '.cache').mkdir(exist_ok=True)

    def check_changed(self, name, mutate):
        original = build.read_json
        def read(path):
            result = copy.deepcopy(original(path))
            if path.name == name:
                mutate(result)
            return result
        with patch.object(build, 'read_json', side_effect=read):
            with self.assertRaises(ValueError):
                build.check()

    def test_missing_upstream_target_is_rejected(self):
        self.check_changed('targets.json', lambda rows: rows.pop('1.20.1-forge'))

    def test_disabling_a_required_target_is_rejected(self):
        self.check_changed('targets.json', lambda rows: rows['1.21.1-fabric'].update(buildable=False))

    def test_toolchain_drift_is_rejected(self):
        self.check_changed('targets.json', lambda rows: rows['1.20.1-forge'].update(java=21))

    def test_wrong_loader_layer_is_rejected(self):
        self.check_changed('targets.json', lambda rows: rows['1.20.1-forge'].update(layers=['neoforge']))

    def test_non_release_dependency_url_is_rejected(self):
        self.check_changed('upstream.lock.json', lambda lock: lock['targets']['1.20.1-forge']['asset'].update(url='https://example.com/mod.jar'))

    def make_jar(self, path, *, version=None, mcphone=None, major=65, embed=False):
        metadata = {
            'id': self.identity['mod_id'], 'version': version or self.identity['mod_version'],
            'depends': {'mcphone': mcphone or f"={self.lock['version']}"}
        }
        with zipfile.ZipFile(path, 'w') as jar:
            jar.writestr('com/november/mcphone/', b'')
            jar.writestr(build.COMMON_CLASS, b'\xca\xfe\xba\xbe\x00\x00' + major.to_bytes(2, 'big'))
            jar.writestr('com/november/mcphone/workshop/platform/WorkshopFabric.class',
                         b'\xca\xfe\xba\xbe\x00\x00' + major.to_bytes(2, 'big'))
            jar.writestr('LICENSE', 'MIT License')
            jar.writestr('fabric.mod.json', json.dumps(metadata))
            if embed:
                jar.writestr('com/november/mcphone/api/MCphoneApi.class', b'')

    def verify(self, path):
        build.verify_jar(path, '1.21.1-fabric', self.targets['1.21.1-fabric'], self.lock, self.identity)

    def test_valid_artifact_passes(self):
        with tempfile.TemporaryDirectory(dir=build.ROOT / '.cache') as temporary:
            path = Path(temporary) / 'valid.jar'
            self.make_jar(path)
            self.verify(path)

    def test_wrong_version_jar_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=build.ROOT / '.cache') as temporary:
            path = Path(temporary) / 'stale.jar'
            self.make_jar(path, version='0.0.0')
            with self.assertRaises(ValueError):
                self.verify(path)

    def test_wrong_mcphone_dependency_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=build.ROOT / '.cache') as temporary:
            path = Path(temporary) / 'unlocked.jar'
            self.make_jar(path, mcphone='*')
            with self.assertRaises(ValueError):
                self.verify(path)

    def test_newer_java_bytecode_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=build.ROOT / '.cache') as temporary:
            path = Path(temporary) / 'newer-java.jar'
            self.make_jar(path, major=69)
            with self.assertRaises(ValueError):
                self.verify(path)

    def test_embedded_mcphone_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=build.ROOT / '.cache') as temporary:
            path = Path(temporary) / 'embedded.jar'
            self.make_jar(path, embed=True)
            with self.assertRaises(ValueError):
                self.verify(path)

    def test_failed_target_does_not_publish_or_skip_remaining_targets(self):
        with tempfile.TemporaryDirectory(dir=build.ROOT / '.cache') as temporary:
            root = Path(temporary)
            (root / '.cache').mkdir()
            for name, target in self.targets.items():
                libs = root / target['project'] / 'build/libs'
                libs.mkdir(parents=True)
                (libs / build.jar_name(name, self.identity)).write_bytes(b'stale artifact')
            attempted = []
            def run(command, cwd, env):
                attempted.append(cwd.name)
                artifact = cwd / 'build/libs' / build.jar_name(cwd.name, self.identity)
                self.assertFalse(artifact.exists(), 'Stale artifact was not removed')
                if cwd.name == '1.21.1-fabric':
                    return subprocess.CompletedProcess(command, 1)
                artifact.write_bytes(b'new artifact')
                return subprocess.CompletedProcess(command, 0)
            with patch.object(build, 'ROOT', root), patch.object(build, 'java_home', return_value=root), \
                 patch.object(build, 'prepare'), patch.object(build, 'verify_jar'), \
                 patch.object(build.subprocess, 'run', side_effect=run), patch.object(build, 'publish_bundle') as publish:
                with self.assertRaises(ValueError):
                    build.build(list(self.targets), self.targets, self.lock, self.identity)
                self.assertEqual(attempted, list(self.targets))
                publish.assert_not_called()

    def test_incomplete_bundle_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=build.ROOT / '.cache') as temporary:
            with patch('sys.argv', ['build.py', 'bundle', '--input', temporary]), \
                 patch.object(build, 'publish_bundle') as publish:
                with self.assertRaises(ValueError):
                    build.main()
                publish.assert_not_called()


if __name__ == '__main__':
    unittest.main()
